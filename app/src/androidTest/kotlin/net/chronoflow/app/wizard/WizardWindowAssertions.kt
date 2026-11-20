package net.chronoflow.app.wizard

import android.view.View
import android.view.inspector.WindowInspector
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue

internal fun focusedWizardWindow(): View {
    lateinit var view: View
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
        view = WindowInspector.getGlobalWindowViews().single { it.hasWindowFocus() }
    }
    return view
}

internal fun ComposeTestRule.assertWizardKeyboardGeometry(
    decor: View,
    queryTag: String = "wizard.airport.query",
    resultTag: String = "wizard.airport.DXB",
    sheetTag: String = "wizard.airport-search",
) {
    fun keyboard(): KeyboardGeometry {
        lateinit var value: KeyboardGeometry
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val insets = checkNotNull(ViewCompat.getRootWindowInsets(decor))
            val bottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            value =
                KeyboardGeometry(
                    insets.isVisible(WindowInsetsCompat.Type.ime()) &&
                        bottom >= 100 * decor.resources.displayMetrics.density,
                    (decor.height - bottom).toFloat(),
                    insets.getInsets(WindowInsetsCompat.Type.statusBars()).top.toFloat(),
                )
        }
        return value
    }
    try {
        waitUntil(5_000) { keyboard().visible }
    } catch (_: ComposeTimeoutException) {
        throw AssertionError("software keyboard not visible — check hw.keyboard / show_ime_with_hard_keyboard")
    }

    fun bounds(tag: String): Rect {
        val node = onNodeWithTag(tag).fetchSemanticsNode()
        val position = node.positionInWindow
        return Rect(position.x, position.y, position.x + node.size.width, position.y + node.size.height)
    }
    var previous: WizardWindowGeometry? = null
    lateinit var stable: WizardWindowGeometry
    waitUntil(5_000) {
        val current = WizardWindowGeometry(bounds(queryTag), bounds(resultTag), keyboard())
        val settled = current.keyboard.visible && current == previous
        previous = current
        if (settled) stable = current
        settled
    }
    assertTrue("query above keyboard", stable.query.bottom <= stable.keyboard.top)
    assertTrue("first result above keyboard", stable.result.bottom <= stable.keyboard.top)
    assertTrue("sheet below status bar", bounds(sheetTag).top >= stable.keyboard.statusBottom)
    assertTrue("query has its full height", stable.query.height > 0)
    assertTrue("first result has its full height", stable.result.height > 0)
}

private data class KeyboardGeometry(
    val visible: Boolean,
    val top: Float,
    val statusBottom: Float,
)

private data class WizardWindowGeometry(
    val query: Rect,
    val result: Rect,
    val keyboard: KeyboardGeometry,
)
