package net.chronoflow.app.auth

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import net.chronoflow.app.MainActivity
import org.junit.Assert.assertTrue

internal class AuthKeyboardAssertions(
    private val compose: SemanticsNodeInteractionsProvider,
    private val activity: MainActivity,
    private val waitFor: (Long, () -> Boolean) -> Unit,
) {
    fun requireSoftwareKeyboard() {
        try {
            waitFor(3_000) { keyboard().visible }
        } catch (_: ComposeTimeoutException) {
            throw AssertionError("software keyboard not visible — check show_ime_with_hard_keyboard")
        }
    }

    fun assertRevealed(
        field: String,
        submit: String,
    ) {
        var previous: RevealBounds? = null
        var settled: RevealBounds? = null
        waitFor(3_000) {
            val now = RevealBounds(bounds(field), bounds(submit), keyboard())
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
