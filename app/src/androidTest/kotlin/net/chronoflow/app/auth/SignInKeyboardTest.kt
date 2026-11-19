package net.chronoflow.app.auth

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import net.chronoflow.app.MainActivity
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Assert.assertTrue
import org.junit.Test

@Smoke
class SignInKeyboardTest : ScreenTest() {
    private lateinit var activity: MainActivity

    @Test fun fieldAndSubmitStayAboveTheSoftwareKeyboard() {
        launch(
            mapOf(
                "CF_AUTH_TEST_AUDIT_SCREEN" to "sign-in",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_MOCK_DELAY" to "0",
            ),
        ).onActivity { activity = it }
        compose.onNodeWithTag("sign-in.email").performClick()
        requireSoftwareKeyboard()
        assertRevealed("sign-in.email")
        compose.onNodeWithTag("sign-in.email").performImeAction()
        compose.onNodeWithTag("sign-in.password").assertIsFocused()
        requireSoftwareKeyboard()
        assertRevealed("sign-in.password")
    }

    private fun requireSoftwareKeyboard() {
        try {
            waitFor(3_000) { keyboard().visible }
        } catch (_: ComposeTimeoutException) {
            throw AssertionError("software keyboard not visible — check show_ime_with_hard_keyboard")
        }
    }

    private fun assertRevealed(field: String) {
        var previous: RevealBounds? = null
        var settled: RevealBounds? = null
        waitFor(3_000) {
            val now = RevealBounds(bounds(field), bounds("sign-in.submit"), keyboard())
            val stable = now.keyboard.visible && now == previous
            previous = now
            if (stable) settled = now
            stable
        }
        val result = checkNotNull(settled)
        assertTrue("field is above the software keyboard", result.field.bottom <= result.keyboard.top)
        assertTrue("submit is above the software keyboard", result.submit.bottom <= result.keyboard.top)
        assertTrue("field is below the status bar", result.field.top >= result.keyboard.statusBottom)
        assertTrue("submit is below the status bar", result.submit.top >= result.keyboard.statusBottom)
        assertTrue("field has its full height", result.field.height > 0)
        assertTrue("submit has its full height", result.submit.height > 0)
    }

    private fun bounds(tag: String): Rect {
        val node = compose.onNodeWithTag(tag).fetchSemanticsNode()
        val position = node.positionInWindow
        return Rect(position.x, position.y, position.x + node.size.width, position.y + node.size.height)
    }

    private fun keyboard(): KeyboardBounds {
        var result: KeyboardBounds? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val decor = activity.window.decorView
            val insets = checkNotNull(ViewCompat.getRootWindowInsets(decor))
            val bottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val minimum = 100 * activity.resources.displayMetrics.density
            result =
                KeyboardBounds(
                    insets.isVisible(WindowInsetsCompat.Type.ime()) && bottom >= minimum,
                    (decor.height - bottom).toFloat(),
                    insets.getInsets(WindowInsetsCompat.Type.statusBars()).top.toFloat(),
                )
        }
        return checkNotNull(result)
    }
}

private data class KeyboardBounds(
    val visible: Boolean,
    val top: Float,
    val statusBottom: Float,
)

private data class RevealBounds(
    val field: Rect,
    val submit: Rect,
    val keyboard: KeyboardBounds,
)
