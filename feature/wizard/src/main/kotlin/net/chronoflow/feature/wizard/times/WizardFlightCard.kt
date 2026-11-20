@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard.times

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfDivider
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.feature.wizard.components.WizardErrorLine
import net.chronoflow.feature.wizard.shell.WizardMetrics
import net.chronoflow.feature.wizard.ui.resolve
import net.chronoflow.feature.wizard.ui.wizardCopy

@Composable
internal fun WizardFlightCard(
    ui: WizardFlightCardUi,
    timeFormat: TimeFormat,
    errorRequester: BringIntoViewRequester,
    onPick: (WizardDateTimeFieldUi, WizardPickerKind) -> Unit,
) {
    val route = "${ui.fromIata} → ${ui.toIata}"
    val shape = RoundedCornerShape(PlanUITokens.wizardCardRadius.dp)
    Column(
        Modifier.fillMaxWidth().testTag("wizard.flight.${ui.index}"),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
    ) {
        Row(
            Modifier
                .padding(
                    horizontal = PlanUITokens.space1.dp,
                ).testTag("wizard.flight.${ui.index}.route")
                .semantics(mergeDescendants = true) {
                    heading()
                },
            horizontalArrangement =
                Arrangement.spacedBy(
                    PlanUITokens.space2.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                LucideGlyphs.Plane.imageVector(),
                null,
                Modifier.size(WizardMetrics.routeGlyph.dp),
                tint = CfTheme.colors.dawnText,
            )
            Text(
                route,
                color = CfTheme.colors.foreground,
                style = CfTheme.type.style(CfTypeScale.wizardRoute),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(
                    shape,
                ).background(CfTheme.colors.card)
                .border(AuthTokens.borderWidth.dp, CfTheme.colors.border, shape),
        ) {
            WizardDateTimeRow(ui.departure, timeFormat, onPick)
            CfDivider()
            WizardDateTimeRow(ui.arrival, timeFormat, onPick)
        }
        if (ui.hasErrors) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .testTag(
                        "wizard.flight.errors.${ui.index}",
                    ).bringIntoViewRequester(errorRequester),
                verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
            ) {
                ui.departure.error?.let {
                    WizardErrorLine(
                        it.wizardCopy().resolve(),
                        "wizard.${ui.index}.departure.error",
                    )
                }
                ui.arrival.error?.let { WizardErrorLine(it.wizardCopy().resolve(), "wizard.${ui.index}.arrival.error") }
            }
        }
    }
}
