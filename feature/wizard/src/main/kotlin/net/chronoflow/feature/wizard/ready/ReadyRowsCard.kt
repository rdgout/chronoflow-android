package net.chronoflow.feature.wizard.ready

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardMetrics
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun ReadyRowsCard(
    ui: ReadyRowsUi,
    onEvent: (WizardEvent) -> Unit,
) {
    val colors = CfTheme.colors
    val shape = RoundedCornerShape(PlanUITokens.wizardCardRadius.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(
                shape,
            ).background(colors.card)
            .border(AuthTokens.borderWidth.dp, colors.border, shape),
    ) {
        ui.rows.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(thickness = AuthTokens.borderWidth.dp, color = colors.border)
            ReadyRowContent(row, ui.enabled, onEvent)
        }
    }
}

@Composable
private fun ReadyRowContent(
    row: ReadyRowUi,
    enabled: Boolean,
    onEvent: (WizardEvent) -> Unit,
) {
    val colors = CfTheme.colors
    val haptics = LocalHaptics.current
    val label = WizardCopy(row.kind.label).resolve()
    val value = row.value.resolve()
    val stacked = LocalDensity.current.fontScale >= ReadyMetrics.stackedFontScale
    val hint = if (row.kind == ReadyRow.LatestWake) null else WizardCopy("Opens this step").resolve()
    val style = CfTheme.type.style(CfTypeScale.screenBody)
    Row(
        Modifier
            .fillMaxWidth()
            .testTag("wizard.ready.${row.kind.id}")
            .semantics(mergeDescendants = true) { contentDescription = "$label, $value" }
            .cfClickable({
                if (row.kind != ReadyRow.LatestWake) haptics.perform(HapticEvent.Selection)
                onEvent(WizardEvent.RevisitFromReady(row.kind))
            }, enabled = enabled, role = Role.Button, onClickLabel = hint)
            .heightIn(min = PlanUITokens.actionMinHeight.dp)
            .padding(
                start = PlanUITokens.space4.dp,
                end = ReadyMetrics.rowEndPadding.dp,
                top = ReadyMetrics.rowVerticalPadding.dp,
                bottom = ReadyMetrics.rowVerticalPadding.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (stacked) {
            Column(
                Modifier.weight(1f).testTag("wizard.ready.${row.kind.id}.stacked"),
                verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardLabelGap.dp),
            ) {
                Text(label, color = colors.mutedForeground, style = style)
                Text(value, color = colors.foreground, style = style.copy(fontFeatureSettings = "tnum"))
            }
        } else {
            Text(
                label,
                Modifier.width(PlanUITokens.wizardReadyLabelWidth.dp),
                color = colors.mutedForeground,
                style = style,
            )
            Text(
                value,
                Modifier.weight(1f),
                color = colors.foreground,
                style = style.copy(fontFeatureSettings = "tnum"),
            )
        }
        Icon(
            LucideGlyphs.ChevronRight.imageVector(),
            null,
            Modifier.size(WizardMetrics.chevronGlyph.dp),
            tint = colors.mutedForeground,
        )
    }
}
