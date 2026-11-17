package net.chronoflow.app

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.tiers.Capture
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Capture
@RunWith(Parameterized::class)
class LaunchDeviceCaptureTest(
    private val dark: Boolean,
    private val ms: Int,
    private val reduced: Boolean,
    private val bars: Boolean,
    private val language: String,
) : ScreenTest() {
    @Test fun capturesTheSeededLaunchOrSystemBars() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val graph = (instrumentation.targetContext.applicationContext as ChronoFlowApp).graph
        instrumentation.runOnMainSync { graph.resetLaunchForTest() }
        val theme = if (dark) "dark" else "light"
        launch(
            mapOf(
                "CF_UI_TEST_PROFILE" to if (bars) "screen" else "launch",
                "cf.settings.theme" to theme,
                "cf.settings.language" to language,
                "cf.freezeAt" to ms.toString(),
                "cf.reduceMotion" to reduced.toString(),
            ),
        )
        val device = UiDevice.getInstance(instrumentation)
        if (!bars) assertTrue(device.wait(Until.hasObject(By.res("launch.opening")), 5000))
        val screenshot = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        if (bars) {
            val height = (device.displayDensity / 160f * 48).toInt()
            BoardCapture.capture(
                Bitmap.createBitmap(screenshot, 0, 0, screenshot.width, height),
                "SplashScreen_system-bars_pixel_en_$theme",
            )
        } else {
            val state = if (reduced) "reduceMotion" else "ready"
            val time = ms.toString().padStart(4, '0')
            BoardCapture.capture(screenshot, "SplashScreen_$state-${time}_pixel_${language}_$theme")
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "dark={0},ms={1},reduce={2},bars={3},{4}")
        fun parameters(): Collection<Array<Any>> =
            listOf(false, true).flatMap { dark ->
                listOf(0, 300, 700, 1000, 1100, 1400, 1500).map { arrayOf<Any>(dark, it, false, false, "en") } +
                    listOf(arrayOf<Any>(dark, 0, true, false, "en"), arrayOf<Any>(dark, 0, false, true, "en"))
            } + listOf(arrayOf<Any>(false, 0, false, false, "nl"))
    }
}
