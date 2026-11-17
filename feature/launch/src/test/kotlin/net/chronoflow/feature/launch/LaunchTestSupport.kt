package net.chronoflow.feature.launch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.core.model.launch.SignedOutReadiness
import net.chronoflow.feature.launch.model.LaunchClock
import net.chronoflow.feature.launch.model.MediaLaunchClock
import java.io.File
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

internal fun projectRoot(): File =
    generateSequence(File(checkNotNull(System.getProperty("user.dir")))) { it.parentFile }
        .first { File(it, "settings.gradle.kts").isFile && File(it, "plan.md").isFile }

internal fun launchController(
    rule: ComposeContentTestRule,
    ms: Int? = 0,
    reduced: Boolean = false,
    observer: () -> Unit = {},
    nextFrame: suspend () -> Long = { androidx.compose.runtime.withFrameNanos { it } },
    clockFactory: (Long) -> LaunchClock = { t0 ->
        MediaLaunchClock(t0) { rule.mainClock.currentTime * 1_000_000 }
    },
    readiness: (LaunchClock) -> LaunchReadiness = { SignedOutReadiness },
    frameLog: LaunchFrameLog? = null,
    overrides: LaunchOverrides = LaunchOverrides(freezeAt = ms?.milliseconds),
): LaunchController {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return LaunchController(
        moon = { resources ->
            val night =
                resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
            val path = if (night) "drawable-night-xxxhdpi" else "drawable-xxxhdpi"
            BitmapFactory
                .decodeFile(
                    File(projectRoot(), "app/src/main/res/$path/launch_moon.png").path,
                    BitmapFactory.Options().apply {
                        inDensity = 640
                        inTargetDensity = resources.displayMetrics.densityDpi
                        inScaled =
                            true
                    },
                ).asImageBitmap()
        },
        nanoTime = {
            rule.mainClock.currentTime * 1_000_000
        },
        compositionObserver = observer,
        nextFrameNanos = nextFrame,
        clockFactory = clockFactory,
        readinessFactory = readiness,
        frameLog = frameLog,
    ).apply {
        captureAppearance(context.resources.configuration)
        captureReduceMotion(!reduced, null)
        claimOverlay(overrides)
    }
}

internal fun firstDraw(
    rule: ComposeContentTestRule,
    controller: LaunchController,
) {
    rule.mainClock.advanceTimeByFrame()
    rule.waitForIdle()
    if (controller.t0Nanos == LaunchController.UNSET) rule.onNodeWithTag("launch.opening").captureToImage()
    check(controller.t0Nanos != LaunchController.UNSET)
}

internal fun channelDifference(
    a: Int,
    b: Int,
): Int =
    maxOf(
        abs((a shr 16 and 255) - (b shr 16 and 255)),
        abs((a shr 8 and 255) - (b shr 8 and 255)),
        abs((a and 255) - (b and 255)),
    )

internal fun assertFrameZero(
    bitmap: Bitmap,
    controller: LaunchController,
) {
    val rect = checkNotNull(controller.frameZeroMoonRectPx)
    val sky = controller.appearance.colors.skyTop
    val expected = sky.toArgb()
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    for (y in 0 until bitmap.height) {
        for (x in 0 until bitmap.width) {
            if (!rect.contains(x, y) && channelDifference(expected, pixels[y * bitmap.width + x]) > 1) {
                throw AssertionError(
                    "sky differs at ($x,$y): expected=${expected.toUInt().toString(
                        16,
                    )} actual=${pixels[y * bitmap.width + x].toUInt().toString(
                        16,
                    )} size=${bitmap.width}x${bitmap.height} rect=$rect",
                )
            }
        }
    }
}
