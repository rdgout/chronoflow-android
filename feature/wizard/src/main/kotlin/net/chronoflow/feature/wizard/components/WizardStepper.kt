package net.chronoflow.feature.wizard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.feature.wizard.shell.WizardMetrics
import kotlin.math.roundToInt
import androidx.compose.ui.semantics.testTag as semanticsTestTag

@Composable
internal fun WizardStepper(
    value: Int,
    range: IntRange,
    label: String,
    onChange: (Int) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = CfTheme.colors
    Box(
        modifier.width((WizardMetrics.target * 2).dp).height(WizardMetrics.target.dp).testTag(testTag).semantics {
            contentDescription = label
            stateDescription = value.toString()
            progressBarRangeInfo =
                ProgressBarRangeInfo(
                    current = value.toFloat(),
                    range = range.first.toFloat()..range.last.toFloat(),
                    steps = (range.last - range.first - 1).coerceAtLeast(0),
                )
            if (!enabled) disabled()
            setProgress { target ->
                val next = target.roundToInt().coerceIn(range)
                if (!enabled || next == value) {
                    false
                } else {
                    onChange(next)
                    true
                }
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(WizardMetrics.stepperWidth.dp)
                .height(WizardMetrics.stepperHeight.dp)
                .background(c.wizardPickerFill, RoundedCornerShape(WizardMetrics.stepperRadius.dp))
                .drawBehind {
                    drawLine(
                        c.border,
                        Offset(size.width / 2, 0f),
                        Offset(size.width / 2, size.height),
                        AuthTokens.borderWidth.dp.toPx(),
                    )
                },
        )
        Row {
            StepperHalf(false, enabled && value > range.first, "$testTag.minus") { onChange(value - 1) }
            StepperHalf(true, enabled && value < range.last, "$testTag.plus") { onChange(value + 1) }
        }
    }
}

@Composable
private fun StepperHalf(
    plus: Boolean,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    val ink = CfTheme.colors.foreground.copy(alpha = if (enabled) 1f else PlanUITokens.wizardEmptyOpacity)
    Box(
        Modifier
            .size(WizardMetrics.target.dp)
            .cfClickable(onClick, enabled = enabled)
            .clearAndSetSemantics {
                semanticsTestTag = tag
                hideFromAccessibility()
                if (!enabled) disabled()
            }.drawBehind {
                val arm = WizardMetrics.stepperArm.dp.toPx()
                val stroke = WizardMetrics.stepperStroke.dp.toPx()
                drawLine(ink, Offset(center.x - arm / 2, center.y), Offset(center.x + arm / 2, center.y), stroke)
                if (plus) {
                    drawLine(
                        ink,
                        Offset(center.x, center.y - arm / 2),
                        Offset(center.x, center.y + arm / 2),
                        stroke,
                    )
                }
            },
    )
}
