package net.chronoflow.feature.launch

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.core.view.animation.PathInterpolatorCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import net.chronoflow.core.designsystem.theme.ReduceMotion
import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.core.model.launch.SignedOutReadiness
import net.chronoflow.feature.launch.model.LaunchClock
import net.chronoflow.feature.launch.model.LaunchCoordinator
import net.chronoflow.feature.launch.model.LaunchEffect
import net.chronoflow.feature.launch.model.LaunchEvent
import net.chronoflow.feature.launch.model.LaunchFrameSampler
import net.chronoflow.feature.launch.model.LaunchHandOffTimeline
import net.chronoflow.feature.launch.model.LaunchKind
import net.chronoflow.feature.launch.model.LaunchProcessPolicy
import net.chronoflow.feature.launch.model.LaunchRoute
import net.chronoflow.feature.launch.model.LaunchSize
import net.chronoflow.feature.launch.model.LaunchState
import net.chronoflow.feature.launch.model.LaunchTimeline
import net.chronoflow.feature.launch.model.MediaLaunchClock
import net.chronoflow.feature.launch.model.SplashExit
import net.chronoflow.feature.launch.model.SplashExitPolicy
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class LaunchController(
    val moon: (Resources) -> ImageBitmap,
    val processToken: String = "process",
    val nanoTime: () -> Long = System::nanoTime,
    val nextFrameNanos: suspend () -> Long = { withFrameNanos { it } },
    internal val compositionObserver: () -> Unit = {},
    private val tracing: LaunchTracing = LaunchTrace,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    private val clockFactory: (Long) -> LaunchClock = { MediaLaunchClock(it, nanoTime) },
    private val readinessFactory: (LaunchClock) -> LaunchReadiness = { SignedOutReadiness },
    private val statusIo: CoroutineDispatcher = Dispatchers.IO,
    val frameLog: LaunchFrameLog? = null,
    private val onReadinessStarted: (Long, LaunchOverrides) -> Unit = { _, _ -> },
) {
    private var capturedAppearance: LaunchAppearance? = null
    val appearance: LaunchAppearance get() = checkNotNull(capturedAppearance)
    private var capturedReduceMotion: Boolean? = null
    val reduceMotion: Boolean get() = checkNotNull(capturedReduceMotion)
    var overrides = LaunchOverrides.None
        private set
    private var claimed = false
    var overlayClaimedByCurrentActivity = false
        private set
    var overlayShowing by mutableStateOf(false)
    var overlayPlaced = false
        private set
    var splashHeld = false
    private val placement = CompletableDeferred<Unit>()
    private val firstDraw = CompletableDeferred<Long>()
    var t0Nanos = UNSET
        private set
    var openingDone = false
        private set
    var systemIconBoundsPx: Rect? = null
    var frameZeroMoonRectPx: Rect? = null
        private set
    lateinit var clock: LaunchClock
        private set

    private var opening = LaunchTimeline.opening(LaunchSize(1.0, 1.0), false)
    private var launchState = LaunchState()
    internal var coordinator: LaunchCoordinator? = null
        private set
    private var readinessStarted = false
    private var headless = false
    private var activityCount = 0
    var lastLaunchKind: LaunchKind? = null
        private set
    var warmFadeCount = 0
        private set
    private var exitTraceJob: Job? = null
    private var statusJob: Job? = null
    private var statusRequest: StatusRequest? = null
    var handOff by mutableStateOf(HandOffPlan(LaunchFrameSampler(opening), null, 0L))
        private set
    var route by mutableStateOf<LaunchRoute?>(null)
        private set
    var showsStatus by mutableStateOf(false)
        private set
    var statusRaster by mutableStateOf<StatusRaster?>(null)
        private set
    var handOffComplete by mutableStateOf(false)
        private set
    val overlayActive: Boolean get() = overlayShowing && !handOffComplete
    var handOffEndElapsedNanos: Long? = null
        private set
    var statusAnnouncementCount = 0
        private set

    internal fun setOpening(size: LaunchSize) {
        if (opening.size == size && opening.reduceMotion == reduceMotion) return
        opening = LaunchTimeline.opening(size, reduceMotion)
        rebuildHandOff()
    }

    private fun rebuildHandOff() {
        val timeline =
            if (overrides.mode !=
                null
            ) {
                overrides.timeline(opening)
            } else {
                LaunchHandOffTimeline.build(opening, launchState)
            }
        handOff =
            HandOffPlan(
                LaunchFrameSampler(timeline),
                LaunchHandOffTimeline.exitEndSeconds(timeline),
                handOff.revision + 1,
            )
    }

    private fun startReadiness() {
        if (readinessStarted || handOffComplete) return
        readinessStarted = true
        onReadinessStarted(t0Nanos, overrides)
        if (overrides.mode == LaunchMode.Slow) prepareStatus()
        if (overrides.mode != null) {
            val start = if (overrides.mode == LaunchMode.Ready) 1500.milliseconds else overrides.readyAt
            scheduleExitTrace(maxOf(if (reduceMotion) Duration.ZERO else 1500.milliseconds, start))
            return
        }
        if (overrides.freezeAt != null) return
        coordinator =
            LaunchCoordinator(
                (overrides.readiness ?: readinessFactory)(clock),
                clock,
                reduceMotion,
                scope = scope,
                onChange = ::onLaunchChange,
            )
        coordinator?.start()
    }

    private fun onLaunchChange(
        state: LaunchState,
        effects: List<LaunchEffect>,
    ) {
        launchState = state
        if (state.isReady) tracing.endReady()
        state.route?.let { route = it }
        if (state.showsStatus && !showsStatus) statusAnnouncementCount++
        showsStatus = state.showsStatus
        if (effects.any {
                it is LaunchEffect.CommitExit || it is LaunchEffect.CommitStatus ||
                    it == LaunchEffect.CancelStatus
            }
        ) {
            rebuildHandOff()
        }
        if (effects.any { it is LaunchEffect.CommitStatus }) prepareStatus()
        val start = state.exitStart
        if (state.didCommitExit && start != null) scheduleExitTrace(start)
    }

    private fun scheduleExitTrace(start: Duration) {
        if (exitTraceJob != null) return
        exitTraceJob =
            scope.launch {
                clock.sleepUntil(start)
                tracing.beginExit()
            }
    }

    internal fun configureStatus(
        context: Context,
        text: String,
        density: Float,
        fontScale: Float,
        color: Int,
    ) {
        val request = StatusRequest(context.applicationContext, text, density, fontScale, color)
        if (statusRequest == request) return
        statusRequest = request
        statusJob?.cancel()
        statusRaster = null
        prepareStatus()
    }

    private fun prepareStatus() {
        if ((launchState.statusStart == null && overrides.mode != LaunchMode.Slow) ||
            statusRaster != null || statusJob?.isActive == true || t0Nanos == UNSET
        ) {
            return
        }
        val request = statusRequest ?: return
        statusJob =
            scope.launch {
                statusRaster =
                    prepareStatusRaster(
                        request.context,
                        request.text,
                        request.density,
                        request.fontScale,
                        request.color,
                        statusIo,
                    )
            }
    }

    fun onExitFrame(elapsedNanos: Long) {
        if (handOffComplete) return
        handOffEndElapsedNanos = elapsedNanos
        coordinator?.send(LaunchEvent.ExitCompleted)
        coordinator?.stop()
        exitTraceJob?.cancel()
        tracing.endFirstCommit()
        tracing.endReady()
        tracing.endExit()
        tracing.endLaunch()
        handOffComplete = true
        overlayShowing = false
    }

    fun captureAppearance(configuration: Configuration) {
        if (capturedAppearance ==
            null
        ) {
            capturedAppearance =
                LaunchAppearance(
                    configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES,
                )
        }
    }

    fun captureReduceMotion(
        system: Boolean,
        override: Boolean?,
    ) {
        if (capturedReduceMotion == null) capturedReduceMotion = ReduceMotion.resolve(system, override)
    }

    fun onActivityCreated(
        savedInstanceState: Bundle?,
        skipOverlay: Boolean,
        activityOverrides: LaunchOverrides = overrides,
    ): LaunchKind {
        val kind =
            LaunchProcessPolicy.classify(
                savedInstanceState != null,
                savedInstanceState?.getString(KEY_PROCESS_TOKEN),
                processToken,
                activityCount == 0,
            )
        activityCount++
        lastLaunchKind = kind
        overlayClaimedByCurrentActivity = false
        splashHeld = false
        if (LaunchProcessPolicy.claimsOverlay(kind)) {
            if (claimOverlay(activityOverrides.copy(skipOverlay = skipOverlay))) {
                tracing.beginFirstCommit()
                if (activityOverrides.mode == null && activityOverrides.freezeAt == null) tracing.beginReady()
            }
        } else {
            overrides = activityOverrides
            if (kind == LaunchKind.Warm) overlayShowing = false
        }
        if (!overlayActive && route == null) startHeadless()
        return kind
    }

    private fun startHeadless() {
        if (headless) return
        coordinator?.stop()
        headless = true
        clock = clockFactory(nanoTime())
        coordinator =
            LaunchCoordinator(
                (overrides.readiness ?: readinessFactory)(clock),
                clock,
                true,
                scope = scope,
                onChange = { state, _ ->
                    launchState = state
                    state.route?.let {
                        route = it
                        coordinator?.stop()
                        headless = false
                    }
                },
            )
        coordinator?.start()
    }

    fun onSystemSplashExit(
        provider: SplashScreenViewProvider,
        kind: LaunchKind,
    ) {
        if (SplashExitPolicy.forActivity(kind, overlayClaimedByCurrentActivity) == SplashExit.Fade300) {
            warmFadeCount++
            provider.view
                .animate()
                .alpha(0f)
                .setDuration(300)
                .setInterpolator(PathInterpolatorCompat.create(0f, 0f, 0.58f, 1f))
                .withEndAction { provider.remove() }
        } else {
            if (overlayClaimedByCurrentActivity) {
                val icon = provider.iconView
                val location = IntArray(2)
                icon.getLocationOnScreen(location)
                systemIconBoundsPx = Rect(location[0], location[1], location[0] + icon.width, location[1] + icon.height)
            }
            provider.remove()
        }
    }

    fun claimOverlay(overrides: LaunchOverrides): Boolean {
        this.overrides = overrides
        overlayClaimedByCurrentActivity = !overrides.skipOverlay && !claimed
        overlayShowing = overlayClaimedByCurrentActivity
        if (overlayClaimedByCurrentActivity) {
            claimed = true
            splashHeld = overrides.holdSplash
        } else {
            splashHeld = false
        }
        return overlayClaimedByCurrentActivity
    }

    fun holdsSystemSplash(): Boolean = (overlayClaimedByCurrentActivity && !overlayPlaced) || splashHeld

    fun markOverlayPlaced() {
        overlayPlaced = true
        placement.complete(Unit)
    }

    fun onFirstDraw(moonRectPx: Rect) {
        if (t0Nanos != UNSET) return
        t0Nanos = nanoTime()
        clock = clockFactory(t0Nanos)
        frameZeroMoonRectPx = moonRectPx
        tracing.endFirstCommit()
        firstDraw.complete(t0Nanos)
        scope.launch {
            yield()
            startReadiness()
        }
    }

    suspend fun awaitOverlayPlaced() {
        placement.await()
    }

    internal val firstDrawSignal get() = firstDraw

    internal suspend fun awaitFirstDraw(): Long = firstDraw.await()

    internal suspend fun playOpening(
        settledNanos: Long,
        onElapsed: (Long) -> Unit,
    ) = runLaunchOpening(firstDraw, nanoTime, nextFrameNanos, settledNanos, onElapsed = onElapsed)

    fun onFrameCommitted(overlayClaimed: Boolean) {
        if (!overlayClaimed && !overlayActive) {
            tracing.endFirstCommit()
            tracing.endLaunch()
        }
    }

    fun onActivityDestroyed(
        isFinishing: Boolean = true,
        isChangingConfigurations: Boolean = false,
    ) {
        if (!isFinishing || isChangingConfigurations) return
        if (!handOffComplete) onExitFrame(if (t0Nanos == UNSET) 0L else (nanoTime() - t0Nanos).coerceAtLeast(0L))
        coordinator?.stop()
        headless = false
        exitTraceJob?.cancel()
        statusJob?.cancel()
        tracing.endFirstCommit()
        tracing.endReady()
        tracing.endExit()
        tracing.endLaunch()
    }

    fun onOpeningSettled() {
        if (openingDone) return
        openingDone = true
    }

    companion object {
        const val UNSET = Long.MIN_VALUE
        const val KEY_PROCESS_TOKEN = "cf.launch.processToken"
    }
}

data class HandOffPlan(
    val sampler: LaunchFrameSampler,
    val exitEndSeconds: Double?,
    val revision: Long,
)

private data class StatusRequest(
    val context: Context,
    val text: String,
    val density: Float,
    val fontScale: Float,
    val color: Int,
)
