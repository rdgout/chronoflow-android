package net.chronoflow.feature.wizard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.wizard.model.WizardDateFormat
import net.chronoflow.core.wizard.model.WizardDateTime
import net.chronoflow.feature.wizard.shell.WizardMetrics
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun WizardTimeRow(
    label: WizardCopy,
    value: String?,
    timeFormat: TimeFormat,
    onClick: () -> Unit,
    tag: String,
    minHeight: Dp = WizardMetrics.timeRowMinHeight.dp,
) {
    val language = Language.of(LocalConfiguration.current.locales[0].language)
    val formatted =
        remember(value, timeFormat, language) {
            value?.takeIf(WizardDateTime::isTime)?.let { WizardDateFormat.time(it, timeFormat, language) }
        }
    val text = label.resolve()
    val empty = WizardCopy("not set").resolve()
    val shape = RoundedCornerShape(WizardMetrics.pillRadius.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .testTag(tag)
            .cfClickable(onClick, role = Role.Button)
            .semantics(mergeDescendants = true) {
                contentDescription = text
                stateDescription = formatted ?: empty
            }.padding(
                start = PlanUITokens.space4.dp,
                end = PlanUITokens.space3.dp,
                top = PlanUITokens.wizardControlGap.dp,
                bottom = PlanUITokens.wizardControlGap.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            Modifier.weight(1f),
            color = CfTheme.colors.foreground,
            style = CfTheme.type.style(CfTypeScale.wizardRow),
        )
        Text(
            formatted ?: WizardCopy("Time").resolve(),
            Modifier
                .testTag("$tag.visual")
                .background(CfTheme.colors.wizardPickerFill, shape)
                .padding(horizontal = PlanUITokens.space3.dp, vertical = PlanUITokens.wizardControlGap.dp),
            color = if (formatted == null) CfTheme.colors.mutedForeground else CfTheme.colors.foreground,
            style = CfTheme.type.style(CfTypeScale.wizardPicker).copy(fontFeatureSettings = "tnum"),
            softWrap = false,
            maxLines = 1,
        )
    }
}
