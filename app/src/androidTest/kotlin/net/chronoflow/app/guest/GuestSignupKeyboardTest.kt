package net.chronoflow.app.guest

import android.view.View
import android.view.inspector.WindowInspector
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class GuestSignupKeyboardTest : ScreenTest() {
    @Test fun testConfirmationStaysAboveTheKeyboard() {
        val started = System.nanoTime()
        val scenario =
            launch(
                mapOf(
                    "CF_MOCK_DELAY" to "0",
                    "cf.settings.language" to null,
                    "cf.settings.theme" to null,
                    "CF_GUEST_WIZARD" to "1",
                    "CF_GUEST_WIZARD_JOURNEY" to "1",
                    "CF_WIZARD_BOARD_CAPTURE" to "GuestSignup",
                ),
            )
        compose.waitUntil(3_000) { compose.onNodeWithTag("guest-signup.confirmation").isDisplayed() }
        compose.onNodeWithTag("guest-signup.confirmation").performScrollTo().performClick()
        lateinit var decor: View
        scenario.onActivity {
            decor = WindowInspector.getGlobalWindowViews().single { it.hasWindowFocus() }
            assertNotSame("the sheet owns the IME window", it.window.decorView, decor)
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var imeBottom = 0
        compose.waitUntil(2_000) {
            instrumentation.runOnMainSync {
                imeBottom = ViewCompat.getRootWindowInsets(decor)?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            }
            imeBottom > 0
        }

        var previousGeometry: Pair<Rect, Float>? = null

        fun confirmationAboveIme(): Boolean {
            var keyboardTop = 0f
            instrumentation.runOnMainSync {
                val insets = checkNotNull(ViewCompat.getRootWindowInsets(decor))
                keyboardTop = (decor.height - insets.getInsets(WindowInsetsCompat.Type.ime()).bottom).toFloat()
            }
            val field = compose.onNodeWithTag("guest-signup.confirmation").fetchSemanticsNode()
            val position = field.positionInWindow
            val bounds = Rect(position.x, position.y, position.x + field.size.width, position.y + field.size.height)
            val geometry = bounds to keyboardTop
            val ready = geometry == previousGeometry && bounds.bottom <= keyboardTop
            previousGeometry = geometry
            return ready
        }
        // The form's bring-into-view scroll starts after the IME finishes its own animation.
        waitForImeToSettle("guest-signup.confirmation", windowDecor = decor, layoutReady = ::confirmationAboveIme)
        assertTrue("confirmation stays above the IME", confirmationAboveIme())
        compose.onNodeWithTag("guest-signup.confirmation").performImeAction()
        compose.waitUntil(3_000) { compose.onNodeWithTag("auth.banner.action").isDisplayed() }
        assertTrue("guest keyboard screen exceeded 10 s", System.nanoTime() - started < 10_000_000_000L)
    }
}
