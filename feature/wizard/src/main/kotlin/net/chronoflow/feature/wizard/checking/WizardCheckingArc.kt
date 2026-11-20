package net.chronoflow.feature.wizard.checking

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.feature.wizard.shell.WizardMetrics
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

@Composable
internal fun WizardCheckingArc(
    from: String,
    to: String,
) {
    val phase =
        if (CfTheme.reduceMotion) {
            null
        } else {
            rememberInfiniteTransition(label = "checking.arc").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(WizardMetrics.arcPeriodMillis, easing = LinearEasing)),
                label = "checking.arc.phase",
            )
        }
    val dawn = CfTheme.colors.dawn
    Row(
        Modifier.fillMaxWidth().testTag("wizard.checking.arc").clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(from, color = CfTheme.colors.foreground, style = CfTheme.type.style(CfTypeScale.wizardArc))
        Box(
            Modifier.weight(1f).height(PlanUITokens.wizardArcHeight.dp).drawWithCache {
                val scale = min(size.width / (WizardMetrics.arcBoxWidth * density), 1f)
                val unit = density * scale
                val ox = (size.width - WizardMetrics.arcBoxWidth * unit) / 2
                val oy = (size.height - PlanUITokens.wizardArcHeight * unit) / 2
                val path =
                    Path().apply {
                        moveTo(ox + WizardMetrics.arcStartX * unit, oy + WizardMetrics.arcBaseY * unit)
                        cubicTo(
                            ox + WizardMetrics.arcControl1X * unit,
                            oy + WizardMetrics.arcControlY * unit,
                            ox + WizardMetrics.arcControl2X * unit,
                            oy + WizardMetrics.arcControlY * unit,
                            ox + WizardMetrics.arcEndX * unit,
                            oy + WizardMetrics.arcBaseY * unit,
                        )
                    }
                val stroke =
                    Stroke(
                        width = AuthTokens.focusWidth * unit,
                        cap = StrokeCap.Round,
                        pathEffect =
                            PathEffect.dashPathEffect(
                                floatArrayOf(
                                    PlanUITokens.space1 * unit,
                                    PlanUITokens.wizardControlGap * unit,
                                ),
                            ),
                    )
                val radius = PlanUITokens.wizardArcDotRadius * unit
                onDrawBehind {
                    drawPath(path, dawn, style = stroke)
                    val t = phase?.let { ((sin(it.value * 2 * PI) + 1) / 2).toFloat() } ?: WizardMetrics.arcReducedPhase
                    val u = 1 - t
                    val x =
                        u * u * u * WizardMetrics.arcStartX + 3 * u * u * t * WizardMetrics.arcControl1X +
                            3 * u * t * t * WizardMetrics.arcControl2X + t * t * t * WizardMetrics.arcEndX
                    val y =
                        u * u * u * WizardMetrics.arcBaseY + 3 * u * u * t * WizardMetrics.arcControlY +
                            3 * u * t * t * WizardMetrics.arcControlY + t * t * t * WizardMetrics.arcBaseY
                    drawCircle(dawn, radius, Offset(ox + x * unit, oy + y * unit))
                }
            },
        )
        Text(to, color = CfTheme.colors.foreground, style = CfTheme.type.style(CfTypeScale.wizardArc))
    }
}
