package net.chronoflow.feature.launch

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaunchOverlayRenderTest(
    private val dark: Boolean,
    private val ms: Int,
    private val reduced: Boolean,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(DeviceProfiles.pixel8, dark = dark),
            ).around(compose)

    @Test fun openingRendersTheSpecifiedFrameWithoutInvalidGeometry() {
        val controller = launchController(compose, ms, reduced)
        compose.setContent { ProfiledLaunchOverlay(controller) }
        firstDraw(compose, controller)
        val bitmap = compose.onNodeWithTag("launch.opening").captureToImage().asAndroidBitmap()
        val output = java.io.File(projectRoot(), "build/phase3a-validation/render-$dark-$ms-$reduced.png")
        output.parentFile?.mkdirs()
        output.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        val density = DeviceProfiles.pixel8.density
        if (ms <= 300 || reduced) assertFrameZero(bitmap, controller)
        if (ms == 0 || ms == 1500 || reduced) {
            val rect = checkNotNull(controller.frameZeroMoonRectPx)
            val lift = if (reduced || ms == 0) 0.0 else -28.0
            val scale = if (reduced || ms == 0) 1.0 else 0.86
            val y = (rect.exactCenterY() + (lift - 39 * scale) * density).roundToInt()
            assertTrue(
                "stroke at (${rect.centerX()},$y): actual=${bitmap.getPixel(
                    rect.centerX(),
                    y,
                ).toUInt().toString(
                    16,
                )} expected=${controller.appearance.colors.dawn.toArgb().toUInt().toString(
                    16,
                )} rect=$rect bitmap=${bitmap.width}x${bitmap.height}",
                channelDifference(
                    bitmap.getPixel(rect.centerX(), y),
                    controller.appearance.colors.dawn
                        .toArgb(),
                ) <= 12,
            )
        }
        if (ms == 1500 &&
            !reduced
        ) {
            assertTrue(
                channelDifference(
                    bitmap.getPixel(bitmap.width / 2, bitmap.height - 1),
                    controller.appearance.colors.skyHorizon
                        .toArgb(),
                ) <=
                    3,
            )
        }
    }

    companion object {
        @JvmStatic
        @Parameters(name = "dark={0},ms={1},reduce={2}")
        fun parameters(): Collection<Array<Any>> =
            listOf(false, true).flatMap { dark ->
                listOf(0, 300, 700, 1000, 1100, 1400, 1500).map { arrayOf<Any>(dark, it, false) } +
                    listOf(arrayOf<Any>(dark, 1500, true))
            }
    }
}
