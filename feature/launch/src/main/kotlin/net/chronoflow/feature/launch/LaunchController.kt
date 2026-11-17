package net.chronoflow.feature.launch

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Rect
import androidx.compose.ui.graphics.ImageBitmap
import net.chronoflow.core.designsystem.theme.ReduceMotion
import net.chronoflow.feature.launch.model.LaunchClock
import net.chronoflow.feature.launch.model.MediaLaunchClock

class LaunchController(
    val moon: (Resources) -> ImageBitmap,
    val nanoTime: () -> Long = System::nanoTime,
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
    var overlayPlaced = false
        private set
    var splashHeld = false
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
    }

    fun onFirstDraw(moonRectPx: Rect) {
        if (t0Nanos != UNSET) return
        t0Nanos = nanoTime()
        clock = MediaLaunchClock(t0Nanos, nanoTime)
        frameZeroMoonRectPx = moonRectPx
        LaunchTrace.endFirstCommit()
    }

    fun onOpeningSettled() {
        if (openingDone) return
        openingDone = true
        LaunchTrace.endLaunch()
    }

    companion object {
        const val UNSET = Long.MIN_VALUE
    }
}
