package net.chronoflow.core.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.CfDarkColors
import net.chronoflow.core.designsystem.generated.CfLightColors
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import kotlin.math.abs

abstract class PrimitiveTestFixture {
    @get:Rule val compose = createComposeRule()
    protected open val dark = false
    protected val colors get() = if (dark) CfDarkColors else CfLightColors

    protected fun host(content: @Composable () -> Unit) {
        compose.setContent {
            ChronoFlowTheme(darkTheme = dark) {
                Box(
                    Modifier
                        .testTag("fixture")
                        .fillMaxSize()
                        .background(colors.background)
                        .padding(16.dp),
                ) { content() }
            }
        }
    }

    protected fun pixels(tag: String): PixelMap = compose.onNodeWithTag(tag).captureToImage().toPixelMap()
}

internal fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
    val results = mutableListOf<TextLayoutResult>()
    performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
    return results.single()
}

internal fun assertTokenColor(
    actual: Color,
    expected: Color,
) {
    assertTrue(
        "$actual != $expected",
        abs(actual.red - expected.red) <= 2f / 255 &&
            abs(actual.green - expected.green) <= 2f / 255 && abs(actual.blue - expected.blue) <= 2f / 255,
    )
}

internal fun PixelMap.hasInk(expected: Color): Boolean =
    (0 until height).any { y ->
        (0 until width).any { x ->
            val c = this[x, y]
            abs(c.red - expected.red) <= 2f / 255 && abs(c.green - expected.green) <= 2f / 255 &&
                abs(c.blue - expected.blue) <= 2f / 255
        }
    }
