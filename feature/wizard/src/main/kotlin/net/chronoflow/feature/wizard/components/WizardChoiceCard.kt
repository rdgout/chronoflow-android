package net.chronoflow.feature.wizard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.cfFocusRing
import net.chronoflow.core.designsystem.effect.cfPress
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.feature.wizard.shell.WizardMetrics
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

internal enum class WizardChoiceStyle { Return, Chronotype, Question }

@Composable
internal fun WizardChoiceCard(
    title: WizardCopy,
    hint: WizardCopy?,
    selected: Boolean,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit,
    style: WizardChoiceStyle = WizardChoiceStyle.Return,
    rawTitle: String? = null,
) {
    val sleep = style == WizardChoiceStyle.Chronotype
    val question = style == WizardChoiceStyle.Question
    val large = LocalDensity.current.fontScale >= WizardMetrics.questionLargeFontScale
    val shape =
        RoundedCornerShape(
            when {
                sleep -> PlanUITokens.radiusXl.dp
                question -> PlanUITokens.wizardQuestionRadius.dp
                else -> PlanUITokens.radius2Xl.dp
            },
        )
    val source = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .testTag(tag)
            .cfFocusRing(shape)
            .selectable(
                selected,
                source,
                indication = null,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ).cfPress(source)
            .background(CfTheme.colors.card, shape)
            .border(AuthTokens.focusWidth.dp, if (selected) CfTheme.colors.dawn else CfTheme.colors.border, shape)
            .heightIn(
                min =
                    when {
                        question && large -> WizardMetrics.questionLargeMinHeight.dp
                        question -> PlanUITokens.wizardFlightRowHeight.dp
                        else -> PlanUITokens.wizardChoiceHeight.dp
                    },
            ).padding(if (sleep) AuthTokens.focusWidth.dp else 0.dp)
            .padding(
                start = (if (sleep) PlanUITokens.wizardContentGap else PlanUITokens.space5).dp,
                end =
                    when {
                        sleep -> PlanUITokens.wizardContentGap.dp
                        question -> PlanUITokens.space5.dp
                        else -> PlanUITokens.space4.dp
                    },
                top =
                    when {
                        sleep -> PlanUITokens.wizardChronotypePadding.dp
                        question -> 0.dp
                        else -> PlanUITokens.space3.dp
                    },
                bottom =
                    when {
                        sleep -> PlanUITokens.wizardChronotypePadding.dp
                        question -> 0.dp
                        else -> PlanUITokens.space3.dp
                    },
            ),
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
        verticalAlignment = if (sleep) Alignment.Top else Alignment.CenterVertically,
    ) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (sleep) PlanUITokens.wizardLabelGap.dp else 0.dp),
        ) {
            Text(
                rawTitle ?: title.resolve(),
                Modifier.testTag("$tag.title"),
                color = CfTheme.colors.foreground,
                style = CfTheme.type.style(if (sleep) CfTypeScale.wizardChronotype else CfTypeScale.wizardChoice),
            )
            hint?.let {
                Text(
                    it.resolve(),
                    color = CfTheme.colors.mutedForeground,
                    style = CfTheme.type.style(if (sleep) CfTypeScale.wizardHint else CfTypeScale.screenBody),
                )
            }
        }
        if (style == WizardChoiceStyle.Return) {
            Icon(
                LucideGlyphs.ChevronRight.imageVector(WizardMetrics.headerStroke),
                null,
                Modifier.size(WizardMetrics.chevronGlyph.dp),
                tint = CfTheme.colors.mutedForeground,
            )
        }
    }
}
