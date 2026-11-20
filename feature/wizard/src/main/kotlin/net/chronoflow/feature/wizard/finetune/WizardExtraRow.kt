package net.chronoflow.feature.wizard.finetune

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.feature.wizard.components.WizardErrorLine
import net.chronoflow.feature.wizard.components.fieldCopy
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardMetrics
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun WizardExtraRow(
    row: ExtraRowUi,
    enabled: Boolean,
    formatter: CopyLineFormatter?,
    onEvent: (WizardEvent) -> Unit,
) {
    val c = CfTheme.colors
    Row(
        Modifier
            .testTag(row.tag)
            .cfClickable({
                onEvent(WizardEvent.OpenPrompt(row.promptId))
            }, enabled = enabled, role = Role.Button)
            .heightIn(min = PlanUITokens.wizardButtonHeight.dp)
            .padding(
                start = PlanUITokens.space4.dp,
                end = PlanUITokens.space3.dp,
                top = PlanUITokens.wizardControlGap.dp,
                bottom = PlanUITokens.wizardControlGap.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardLabelGap.dp)) {
            Text(
                WizardCopy(row.labelKey).resolve() + row.suffix.orEmpty(),
                color = c.foreground,
                style = CfTheme.type.style(CfTypeScale.wizardSearchName),
            )
            if (row.required) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        LucideGlyphs.Star.imageVector(),
                        null,
                        Modifier.size(PlanUITokens.space3.dp).testTag("${row.tag}.star"),
                        tint = c.dawnText,
                    )
                    Text(
                        WizardCopy("Helps a lot").resolve(),
                        color = c.dawnText,
                        style = CfTheme.type.style(CfTypeScale.screenMeta).copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
            row.error?.let { WizardErrorLine(fieldCopy(it, formatter), "wizard.extra.error.${row.promptId}") }
        }
        val answer =
            when (val answer = row.answer) {
                is AnswerText.Literal -> answer.text
                is AnswerText.Copy -> answer.copy.resolve()
                null -> WizardCopy("Add").resolve()
            }
        Text(
            answer,
            Modifier.testTag("${row.tag}.value"),
            color = if (row.answer == null) c.dawnText else c.foreground,
            style = CfTheme.type.style(CfTypeScale.wizardFieldLabel),
            maxLines = 1,
            softWrap = false,
        )
        Icon(
            LucideGlyphs.ChevronRight.imageVector(),
            null,
            Modifier.size(WizardMetrics.chevronGlyph.dp),
            tint = c.mutedForeground,
        )
    }
}
