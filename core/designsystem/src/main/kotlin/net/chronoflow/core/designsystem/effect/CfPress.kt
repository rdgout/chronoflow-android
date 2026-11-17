package net.chronoflow.core.designsystem.effect

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.chronoflow.core.designsystem.generated.PressTokens
import net.chronoflow.core.designsystem.theme.LocalReduceMotion

data class PressValues(
    val scale: Float,
    val alpha: Float,
    val durationMillis: Int,
)

fun pressValues(
    isPressed: Boolean,
    reduceMotion: Boolean,
): PressValues =
    PressValues(
        scale = if (isPressed && !reduceMotion) PressTokens.scale else 1f,
        alpha = if (isPressed) PressTokens.opacity else 1f,
        durationMillis = if (reduceMotion) 0 else PressTokens.durationMillis,
    )

private val pressEasing = PressTokens.curve.let { CubicBezierEasing(it[0], it[1], it[2], it[3]) }

object CfPressIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = CfPressNode(interactionSource)

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = System.identityHashCode(this)
}

private class CfPressNode(
    private val source: InteractionSource,
) : Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode {
    private val progress = Animatable(0f)
    private var layer: GraphicsLayer? = null
    private var reduceMotion = false

    override fun onAttach() {
        layer = requireGraphicsContext().createGraphicsLayer()
        coroutineScope.launch {
            val presses = mutableSetOf<PressInteraction.Press>()
            var animation: Job? = null
            source.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> presses.add(interaction)
                    is PressInteraction.Release -> presses.remove(interaction.press)
                    is PressInteraction.Cancel -> presses.remove(interaction.press)
                    else -> return@collect
                }
                reduceMotion = currentValueOf(LocalReduceMotion)
                animation?.cancel()
                animation =
                    launch {
                        val target = if (presses.isEmpty()) 0f else 1f
                        if (reduceMotion) {
                            progress.snapTo(target)
                        } else {
                            progress.animateTo(target, tween(PressTokens.durationMillis, easing = pressEasing))
                        }
                    }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val ink = layer ?: return
        val p = progress.value
        ink.record { this@draw.drawContent() }
        val scale = if (reduceMotion) 1f else 1f + (PressTokens.scale - 1f) * p
        ink.scaleX = scale
        ink.scaleY = scale
        ink.alpha = 1f + (PressTokens.opacity - 1f) * p
        drawLayer(ink)
    }

    override fun onDetach() {
        layer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        layer = null
    }
}

fun Modifier.cfPress(interactionSource: InteractionSource): Modifier = indication(interactionSource, CfPressIndication)

fun Modifier.cfClickable(
    onClick: () -> Unit,
    role: Role = Role.Button,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
    focusShape: Shape = RectangleShape,
): Modifier =
    cfFocusRing(focusShape).clickable(
        interactionSource = interactionSource,
        indication = CfPressIndication,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = onClick,
    )
