package net.chronoflow.feature.launch

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.launch.model.LaunchLayout
import net.chronoflow.feature.launch.model.LaunchSize
import net.chronoflow.feature.launch.model.StatusLineLayout
import org.junit.Assert.assertEquals
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
class LaunchSlowStatusRenderTest(
    private val language: AppLanguage,
    private val dark: Boolean,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(DeviceProfiles.pixel8, language, dark),
            ).around(compose)

    @Test fun slowStatusDrawsTextAndThreePulsingDotsThenHandsOff() {
        val host = LaunchEndingTestHost(compose, readyMs = 3500)
        val controller = host.controller
        host.advanceTo(16)
        compose.waitUntil(2000) {
            org.robolectric.Shadows
                .shadowOf(android.os.Looper.getMainLooper())
                .idle()
            controller.statusRaster != null
        }
        val raster = requireNotNull(controller.statusRaster)
        val p = DeviceProfiles.pixel8
        val density = p.density
        val layout =
            StatusLineLayout(
                p.widthDp.toDouble(),
                raster.textWidthDp,
                raster.ascentDp,
                raster.descentDp,
                raster.lineHeightDp,
                density.toDouble(),
            )
        val top = LaunchLayout(LaunchSize(p.widthDp.toDouble(), p.heightDp.toDouble())).status.y
        host.advanceTo(1990)
        val before = compose.onNodeWithTag("launch.opening").captureToImage().asAndroidBitmap()
        host.advanceTo(2600)
        val after = compose.onNodeWithTag("launch.opening").captureToImage().asAndroidBitmap()
        val title =
            controller.appearance.colors.skyTitle
                .toArgb()
        val left = (layout.text.minX * density).toInt()
        val right = (layout.text.maxX * density).toInt()
        val y0 = ((top + layout.text.minY) * density).toInt()
        val y1 = ((top + layout.text.maxY) * density).toInt()
        var matching = 0
        var beforeMatching = 0
        for (y in y0 until y1) {
            for (x in left until right) {
                if (channelDifference(title, after.getPixel(x, y)) <= 24) matching++
                if (channelDifference(title, before.getPixel(x, y)) <= 24) beforeMatching++
            }
        }
        assertEquals(0, beforeMatching)
        assertTrue(matching > 0)
        val cy = ((top + raster.lineHeightDp / 2) * density).roundToInt()
        val differences =
            IntArray(3) { i ->
                val x = ((layout.dots.minX + 2 + i * 7) * density).roundToInt()
                channelDifference(after.getPixel(x, cy), before.getPixel(x, cy))
            }
        assertTrue(differences.all { it > 0 })
        assertTrue(differences[0] > differences[1] && differences[0] > differences[2])
        assertEquals(1, controller.statusAnnouncementCount)
        assertTrue(
            compose
                .onNodeWithTag(
                    "launch.opening",
                ).fetchSemanticsNode()
                .config
                .contains(SemanticsProperties.StateDescription),
        )
        host.advanceTo(3908)
        assertTrue(controller.handOffComplete)
        assertTrue(requireNotNull(controller.handOffEndElapsedNanos) in 3_900_000_000..3_908_000_000)
    }

    companion object {
        @JvmStatic
        @Parameters(name = "{0}, dark={1}")
        fun cases() =
            listOf(
                arrayOf<Any>(AppLanguage.En, false),
                arrayOf<Any>(AppLanguage.En, true),
                arrayOf<Any>(AppLanguage.Nl, false),
                arrayOf<Any>(AppLanguage.Nl, true),
            )
    }
}
