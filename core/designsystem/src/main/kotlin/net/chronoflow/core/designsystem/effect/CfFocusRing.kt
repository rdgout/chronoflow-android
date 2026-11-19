package net.chronoflow.core.designsystem.effect

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.theme.LocalCfColors

fun Modifier.cfFocusRing(shape: Shape): Modifier = this.then(FocusRingElement(shape))

private data class FocusRingElement(
    val shape: Shape,
) : ModifierNodeElement<FocusRingNode>() {
    override fun create() = FocusRingNode(shape)

    override fun update(node: FocusRingNode) {
        node.shape = shape
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "cfFocusRing"
    }
}

private class FocusRingNode(
    var shape: Shape,
) : Modifier.Node(),
    FocusEventModifierNode,
    DrawModifierNode,
    CompositionLocalConsumerModifierNode {
    private var focused = false
    private var cachedSize = Size.Unspecified
    private var cachedShape: Shape? = null
    private var cachedDirection: LayoutDirection? = null
    private var cachedDensity = Float.NaN
    private var cachedOutline: Outline? = null
    private var cachedStroke: Stroke? = null

    override fun onFocusEvent(focusState: FocusState) {
        focused = focusState.isFocused
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (focused) {
            val stroke = AuthTokens.focusWidth.dp.toPx()
            val half = stroke / 2
            if (cachedSize != size || cachedShape != shape || cachedDirection != layoutDirection ||
                cachedDensity != density
            ) {
                cachedOutline =
                    shape.createOutline(Size(size.width + stroke, size.height + stroke), layoutDirection, this)
                cachedStroke = Stroke(stroke)
                cachedSize = size
                cachedShape = shape
                cachedDirection = layoutDirection
                cachedDensity = density
            }
            val outline = requireNotNull(cachedOutline)
            val style = requireNotNull(cachedStroke)
            translate(-half, -half) { drawOutline(outline, currentValueOf(LocalCfColors).ring, style = style) }
        }
    }
}
