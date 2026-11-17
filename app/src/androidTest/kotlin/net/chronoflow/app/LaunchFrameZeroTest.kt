package net.chronoflow.app

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.tiers.Smoke
import net.chronoflow.feature.launch.LaunchController
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

@Smoke
class LaunchFrameZeroTest : ScreenTest() {
    private var startedActivity: MainActivity? = null
    private val destroyed = CountDownLatch(1)
    private val activityLifecycle =
        ActivityLifecycleCallback { activity, stage ->
            if (activity is MainActivity) {
                startedActivity = activity
                if (stage == Stage.DESTROYED) destroyed.countDown()
            }
        }

    @After fun finishPlainLaunch() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var finish = false
        instrumentation.runOnMainSync {
            (instrumentation.targetContext.applicationContext as ChronoFlowApp).graph.launch.splashHeld = false
            startedActivity?.let {
                finish = true
                it.finish()
            }
        }
        if (finish) assertTrue("plain launch did not finish", destroyed.await(5, TimeUnit.SECONDS))
        instrumentation.runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance().removeLifecycleCallback(activityLifecycle)
        }
    }

    @Test fun frameZeroMatchesSystemSplashLight() = verify(false)

    @Test fun frameZeroMatchesSystemSplashDark() = verify(true)

    private fun verify(dark: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val graph = (instrumentation.targetContext.applicationContext as ChronoFlowApp).graph
        instrumentation.runOnMainSync {
            graph.resetLaunchForTest()
            ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback(activityLifecycle)
        }
        launchWithoutIdle(
            mapOf(
                "CF_UI_TEST_PROFILE" to "launch",
                "cf.settings.theme" to if (dark) "dark" else "light",
                "cf.holdSplash" to "true",
                "cf.freezeAt" to "0",
            ),
        )
        val controller = graph.launch
        runBlocking { withTimeout(5000) { controller.awaitOverlayPlaced() } }
        instrumentation.runOnMainSync { assertTrue(controller.holdsSystemSplash()) }
        val system = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        instrumentation.runOnMainSync { controller.splashHeld = false }
        val device = UiDevice.getInstance(instrumentation)
        assertTrue(
            "system splash never handed over to launch.opening",
            device.wait(Until.hasObject(By.res("launch.opening")), 5000),
        )
        waitFor(5000) { controller.t0Nanos != LaunchController.UNSET && controller.systemIconBoundsPx != null }
        val overlay = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        BoardCapture.capture(system, "harness/parity-system-${if (dark) "dark" else "light"}")
        BoardCapture.capture(overlay, "harness/parity-overlay-${if (dark) "dark" else "light"}")
        val icon = checkNotNull(controller.systemIconBoundsPx)
        val moon = checkNotNull(controller.frameZeroMoonRectPx)
        val density = instrumentation.targetContext.resources.displayMetrics.density
        val measuredDp = icon.width() / density.toDouble()
        assertEquals("system icon view must be 192 dp; measured $measuredDp dp", 192.0, measuredDp, 1.0 / density)
        assertEquals(icon.exactCenterX().toDouble(), moon.exactCenterX().toDouble(), 1.0)
        assertEquals(icon.exactCenterY().toDouble(), moon.exactCenterY().toDouble(), 1.0)
        for (rect in listOf(icon, moon)) {
            assertEquals(device.displayWidth / 2.0, rect.exactCenterX().toDouble(), 1.0)
            assertEquals(device.displayHeight / 2.0, rect.exactCenterY().toDouble(), 1.0)
        }
        val x = device.displayWidth / 2
        val y = device.displayHeight / 2
        assertTrue("centre pixel differs", difference(system.getPixel(x, y), overlay.getPixel(x, y)) <= 2)
        val sky =
            instrumentation.targetContext
                .createConfigurationContext(
                    Configuration(instrumentation.targetContext.resources.configuration).apply {
                        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                            if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                    },
                ).getColor(R.color.launch_sky_top)
        assertMoonCircle(system, overlay, x, y, 96 * density, sky)
        val band = (24 * density).roundToInt()
        for (bitmap in listOf(system, overlay)) {
            for (edgeX in 0 until band) {
                assertTrue("left sky band differs", difference(sky, bitmap.getPixel(edgeX, y)) <= 1)
                assertTrue("right sky band differs", difference(sky, bitmap.getPixel(bitmap.width - 1 - edgeX, y)) <= 1)
            }
        }
    }

    private fun assertMoonCircle(
        a: Bitmap,
        b: Bitmap,
        cx: Int,
        cy: Int,
        radius: Float,
        sky: Int,
    ) {
        val r = radius.roundToInt()
        val side = 2 * r + 3
        val left = cx - r - 1
        val top = cy - r - 1
        val pixelsA = IntArray(side * side)
        val pixelsB = IntArray(side * side)
        a.getPixels(pixelsA, 0, side, left, top, side, side)
        b.getPixels(pixelsB, 0, side, left, top, side, side)
        var flat = 0
        var close = 0
        var worstFlat = 0
        var weightA = 0.0
        var weightB = 0.0
        var inkXA = 0.0
        var inkYA = 0.0
        var inkXB = 0.0
        var inkYB = 0.0
        for (y in 1..2 * r + 1) {
            for (x in 1..2 * r + 1) {
                val dx = x - r - 1
                val dy = y - r - 1
                if (dx * dx + dy * dy > radius * radius) continue
                val i = y * side + x
                val aDistance = difference(pixelsA[i], sky)
                val bDistance = difference(pixelsB[i], sky)
                if (aDistance > 64) {
                    weightA += aDistance
                    inkXA += dx * aDistance
                    inkYA += dy * aDistance
                }
                if (bDistance > 64) {
                    weightB += bDistance
                    inkXB += dx * bDistance
                    inkYB += dy * bDistance
                }
                if (isEdge(pixelsA, i, side) || isEdge(pixelsB, i, side)) continue
                val delta = difference(pixelsA[i], pixelsB[i])
                worstFlat = maxOf(worstFlat, delta)
                if (delta <= 4) close++
                flat++
            }
        }
        assertTrue("moon ink is missing", weightA > 0 && weightB > 0)
        assertEquals("moon ink centroid x", inkXA / weightA, inkXB / weightB, 0.5)
        assertEquals("moon ink centroid y", inkYA / weightA, inkYB / weightB, 0.5)
        assertTrue("no flat pixels in comparison", flat > 0)
        assertTrue("flat moon pixels differ by $worstFlat", worstFlat <= 16)
        assertTrue("only ${100.0 * close / flat}% of flat moon pixels agree", close >= flat * 0.995)
    }

    private fun isEdge(
        pixels: IntArray,
        centre: Int,
        stride: Int,
    ): Boolean {
        var minR = 255
        var minG = 255
        var minB = 255
        var maxR = 0
        var maxG = 0
        var maxB = 0
        for (dy in -1..1) {
            for (dx in -1..1) {
                val pixel = pixels[centre + dy * stride + dx]
                val red = pixel shr 16 and 255
                val green = pixel shr 8 and 255
                val blue = pixel and 255
                minR = minOf(minR, red)
                minG = minOf(minG, green)
                minB = minOf(minB, blue)
                maxR = maxOf(maxR, red)
                maxG = maxOf(maxG, green)
                maxB = maxOf(maxB, blue)
                if (maxR - minR > 32 || maxG - minG > 32 || maxB - minB > 32) return true
            }
        }
        return false
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
