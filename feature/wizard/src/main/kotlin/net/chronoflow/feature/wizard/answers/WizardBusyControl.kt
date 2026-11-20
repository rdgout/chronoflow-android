@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.feature.wizard.answers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.designsystem.component.CfDivider
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.wizard.model.WizardDateTemplate
import net.chronoflow.core.wizard.model.WizardDateTime
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardMetrics
import net.chronoflow.feature.wizard.sleep.clockString
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

internal fun busyRangeText(
    row: JsonObject?,
    language: Language,
): String? {
    val fields = row ?: return null
    val start = fields["start_local"].clockString()?.let(WizardDateTime::date) ?: return null
    val end = fields["end_local"].clockString()?.let(WizardDateTime::date) ?: return null
    val startText = WizardDateTime.format(start, WizardDateTemplate.EEE_D_MMM, language)
    val endText =
        if (start.toLocalDate() == end.toLocalDate()) {
            ""
        } else {
            WizardDateTime.format(end, WizardDateTemplate.EEE_D_MMM, language) + " · "
        }
    return startText + " · " + WizardDateTime.string(start).takeLast(5) + "–" + endText +
        WizardDateTime.string(end).takeLast(5)
}

@Composable
internal fun WizardBusyControl(
    ui: WizardAnswerSheetUi,
    onEvent: (WizardEvent) -> Unit,
) {
    val language = Language.of(LocalConfiguration.current.locales[0].language)
    val state = ui.state
    var stage by rememberSaveable(state.prompt.id) { mutableIntStateOf(-1) }
    var index by rememberSaveable(state.prompt.id) { mutableIntStateOf(-1) }
    var from by rememberSaveable(state.prompt.id) { mutableStateOf("") }
    var to by rememberSaveable(state.prompt.id) { mutableStateOf("") }

    fun open(row: Int?) {
        val values = row?.let { state.intervals.getOrNull(it) as? JsonObject }
        from = values?.get("start_local").clockString() ?: ui.busyInitial ?: return
        to = values?.get("end_local").clockString().orEmpty()
        index = row ?: -1
        stage = 0
    }
    Column {
        if (state.intervals.isNotEmpty()) {
            WizardAnswerCard {
                state.intervals.forEachIndexed { i, row ->
                    if (i > 0) CfDivider()
                    val text = busyRangeText(row as? JsonObject, language) ?: WizardCopy("Choose").resolve()
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = PlanUITokens.space4.dp),
                        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text,
                            Modifier
                                .weight(1f)
                                .heightIn(min = WizardMetrics.sheetRowMinHeight.dp)
                                .cfClickable({ open(i) }, role = Role.Button)
                                .testTag("wizard.answer.busy-range.$i"),
                            color = CfTheme.colors.foreground,
                            style = CfTheme.type.style(CfTypeScale.wizardSearchName).copy(fontFeatureSettings = "tnum"),
                        )
                        val description = WizardCopy("Remove {{range}}", mapOf("range" to text)).resolve()
                        Box(
                            Modifier
                                .size(PlanUITokens.actionMinHeight.dp)
                                .testTag("wizard.answer.remove-busy.$i")
                                .semantics { contentDescription = description }
                                .cfClickable({
                                    onEvent(
                                        WizardEvent.UpdateAnswer(
                                            "pre_departure_busy_intervals",
                                            state.removeBusy(i).values.getValue("pre_departure_busy_intervals"),
                                        ),
                                    )
                                }, role = Role.Button),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .size(
                                        PlanUITokens.space8.dp,
                                    ).background(CfTheme.colors.wizardPickerFill, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    LucideGlyphs.X.imageVector(),
                                    null,
                                    Modifier.size(PlanUITokens.wizardSpinnerSize.dp),
                                    tint = CfTheme.colors.mutedForeground,
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = PlanUITokens.actionMinHeight.dp)
                .testTag("wizard.answer.add-busy")
                .cfClickable({ open(null) }, role = Role.Button)
                .padding(horizontal = PlanUITokens.space1.dp),
            horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                LucideGlyphs.Plus.imageVector(),
                null,
                Modifier.size(PlanUITokens.space4.dp),
                tint = CfTheme.colors.dawnText,
            )
            Text(
                WizardCopy("Add busy hours").resolve(),
                color = CfTheme.colors.dawnText,
                style = CfTheme.type.style(CfTypeScale.wizardSearchName),
            )
        }
    }
    if (stage >= 0) {
        WizardBusyPickers(
            stage,
            from,
            to,
            ui.busyZone,
            onDismiss = { stage = -1 },
            onFrom = { value ->
                from = value
                val start = WizardDateTime.date(value)!!
                val previous = WizardDateTime.date(to)
                to = WizardDateTime.string(previous?.takeIf { it > start } ?: start.plusHours(1))
                stage++
            },
            onTo = { value ->
                to = value
                stage++
            },
            onDone = { value ->
                val next = state.putBusy(index.takeIf { it >= 0 }, from, value)
                onEvent(
                    WizardEvent.UpdateAnswer(
                        "pre_departure_busy_intervals",
                        next.values.getValue("pre_departure_busy_intervals"),
                    ),
                )
                stage = -1
            },
        )
    }
}
