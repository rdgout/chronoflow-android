package net.chronoflow.app

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Smoke
import net.chronoflow.feature.launch.LaunchController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToInt

@Smoke
class LaunchFrameZeroTest : ScreenTest() {
    @Test fun frameZeroMatchesSystemSplashLight() = verify(false)

    @Test fun frameZeroMatchesSystemSplashDark() = verify(true)

    private fun verify(dark: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val graph = (instrumentation.targetContext.applicationContext as ChronoFlowApp).graph
        instrumentation.runOnMainSync { graph.resetLaunchForTest() }
        launch(
            mapOf(
                "CF_UI_TEST_PROFILE" to "launch",
                "cf.settings.theme" to if (dark) "dark" else "light",
                "cf.holdSplash" to "true",
                "cf.freezeAt" to "0",
            ),
        )
        val controller = graph.launch
        waitFor(5000) { controller.overlayPlaced && controller.holdsSystemSplash() }
        val system = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        instrumentation.runOnMainSync { controller.splashHeld = false }
        val device = UiDevice.getInstance(instrumentation)
        assertTrue(
            "system splash never handed over to launch.opening",
            device.wait(Until.hasObject(By.res("launch.opening")), 5000),
        )
        waitFor(5000) { controller.t0Nanos != LaunchController.UNSET && controller.systemIconBoundsPx != null }
        val overlay = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        val icon = checkNotNull(controller.systemIconBoundsPx)
        val moon = checkNotNull(controller.frameZeroMoonRectPx)
        val density = instrumentation.targetContext.resources.displayMetrics.density
        val measuredDp = icon.width() / density.toDouble()
        assertEquals("system icon must be 288 dp; measured $measuredDp dp", 288.0, measuredDp, 1.0 / density)
        assertRectsAgree(icon, moon)
        for (rect in listOf(icon, moon)) {
            assertEquals(device.displayWidth / 2.0, rect.exactCenterX().toDouble(), 1.0)
            assertEquals(device.displayHeight / 2.0, rect.exactCenterY().toDouble(), 1.0)
        }
        val x = device.displayWidth / 2
        val y = device.displayHeight / 2
        assertTrue("centre pixel differs", difference(system.getPixel(x, y), overlay.getPixel(x, y)) <= 2)
        assertMoonCircle(system, overlay, x, y, 96 * density)
        val sky =
            instrumentation.targetContext
                .createConfigurationContext(
                    Configuration(instrumentation.targetContext.resources.configuration).apply {
                        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                            if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                    },
                ).getColor(R.color.launch_sky_top)
        val band = (24 * density).roundToInt()
        for (bitmap in listOf(system, overlay)) {
            for (edgeX in 0 until band) {
                assertTrue("left sky band differs", difference(sky, bitmap.getPixel(edgeX, y)) <= 1)
                assertTrue("right sky band differs", difference(sky, bitmap.getPixel(bitmap.width - 1 - edgeX, y)) <= 1)
            }
        }
    }

    private fun assertRectsAgree(
        a: Rect,
        b: Rect,
    ) {
        assertTrue("left: $a vs $b", abs(a.left - b.left) <= 1)
        assertTrue("top: $a vs $b", abs(a.top - b.top) <= 1)
        assertTrue("right: $a vs $b", abs(a.right - b.right) <= 1)
        assertTrue("bottom: $a vs $b", abs(a.bottom - b.bottom) <= 1)
    }

    private fun assertMoonCircle(
        a: Bitmap,
        b: Bitmap,
        cx: Int,
        cy: Int,
        radius: Float,
    ) {
        var total = 0
        var close = 0
        val r = radius.roundToInt()
        for (y in cy - r..cy + r) {
            for (x in cx - r..cx + r) {
                if ((x - cx) * (x - cx) + (y - cy) * (y - cy) > radius * radius) continue
                val delta = difference(a.getPixel(x, y), b.getPixel(x, y))
                assertTrue("moon differs by $delta at ($x,$y)", delta <= 16)
                if (delta <= 4) close++
                total++
            }
        }
        assertTrue("only ${100.0 * close / total}% of moon pixels agree", close >= total * 0.99)
    }

    private fun difference(
        a: Int,
        b: Int,
    ): Int =
        maxOf(
            abs((a shr 16 and 255) - (b shr 16 and 255)),
            abs((a shr 8 and 255) - (b shr 8 and 255)),
            abs((a and 255) - (b and 255)),
        )
}
