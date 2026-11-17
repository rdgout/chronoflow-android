package net.chronoflow.feature.launch

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Rect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CompletableDeferred
import net.chronoflow.core.designsystem.theme.ReduceMotion
import net.chronoflow.feature.launch.model.LaunchClock
import net.chronoflow.feature.launch.model.MediaLaunchClock

class LaunchController(
    val moon: (Resources) -> ImageBitmap,
    val nanoTime: () -> Long = System::nanoTime,
    val nextFrameNanos: suspend () -> Long = { withFrameNanos { it } },
    internal val compositionObserver: () -> Unit = {},
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
    var restored = false
        private set
    var systemIconBoundsPx: Rect? = null
    var frameZeroMoonRectPx: Rect? = null
        private set
    lateinit var clock: LaunchClock
        private set

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

    fun onActivityCreated(restored: Boolean) {
        this.restored = restored
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
        clock = MediaLaunchClock(t0Nanos, nanoTime)
        frameZeroMoonRectPx = moonRectPx
        LaunchTrace.endFirstCommit()
        firstDraw.complete(t0Nanos)
    }

    suspend fun awaitOverlayPlaced() {
        placement.await()
    }

    internal suspend fun awaitFirstDraw(): Long = firstDraw.await()

    internal suspend fun playOpening(
        settledNanos: Long,
        onElapsed: (Long) -> Unit,
    ) = runLaunchOpening(firstDraw, nanoTime, nextFrameNanos, settledNanos, onElapsed)

    fun onOpeningSettled() {
        if (openingDone) return
        openingDone = true
        LaunchTrace.endLaunch()
    }

    companion object {
        const val UNSET = Long.MIN_VALUE
    }
}
