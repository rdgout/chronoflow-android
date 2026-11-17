package net.chronoflow.core.designsystem.effect

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.FocusEventModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
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

    override fun onFocusEvent(focusState: FocusState) {
        focused = focusState.isFocused
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (focused) {
            val stroke = AuthTokens.focusWidth.dp.toPx()
            val half = stroke / 2
            val outline = shape.createOutline(Size(size.width + stroke, size.height + stroke), layoutDirection, this)
            translate(-half, -half) { drawOutline(outline, currentValueOf(LocalCfColors).ring, style = Stroke(stroke)) }
        }
    }
}
