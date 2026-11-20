package net.chronoflow.feature.wizard.times

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.wizard.model.WizardDateFormat
import net.chronoflow.feature.wizard.components.fieldCopy
import net.chronoflow.feature.wizard.shell.WizardMetrics
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

enum class WizardPickerKind { Date, Time }

@Composable
internal fun WizardDateTimeRow(
    field: WizardDateTimeFieldUi,
    timeFormat: TimeFormat,
    onPick: (WizardDateTimeFieldUi, WizardPickerKind) -> Unit,
    copyFormatter: CopyLineFormatter? = null,
) {
    val language = Language.of(LocalConfiguration.current.locales[0].language)
    val dateFormat = remember(language) { { date: String -> WizardDateFormat.datePill(date, language) } }
    val timeText =
        remember(language, timeFormat) {
            { time: String ->
                WizardDateFormat.time(time, timeFormat, language)
            }
        }
    val date = field.datePart?.let(dateFormat)
    val time = field.timePart?.let(timeText)
    val spoken = field.datePart?.let { WizardDateFormat.dateSpoken(it, language) }
    Layout(
        content = {
            Column(Modifier.testTag("${field.tag}.label")) {
                Text(
                    WizardCopy(if (field.arrival) "Arrives" else "Departs").resolve(),
                    color = CfTheme.colors.foreground,
                    style = CfTheme.type.style(CfTypeScale.wizardRow),
                )
                Text(
                    field.zone.resolve(),
                    color = CfTheme.colors.mutedForeground,
                    style = CfTheme.type.style(CfTypeScale.wizardHint),
                )
            }
            WizardDateTimePill(
                field,
                WizardPickerKind.Date,
                date,
                spoken,
                copyFormatter,
            ) { onPick(field, WizardPickerKind.Date) }
            WizardDateTimePill(
                field,
                WizardPickerKind.Time,
                time,
                time,
                copyFormatter,
            ) { onPick(field, WizardPickerKind.Time) }
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(
                    min = PlanUITokens.wizardFlightRowHeight.dp,
                ).padding(
                    start = PlanUITokens.space4.dp,
                    end = PlanUITokens.space3.dp,
                    top = PlanUITokens.space2.dp,
                    bottom = PlanUITokens.space2.dp,
                ),
    ) { measurables, constraints ->
        val gap = PlanUITokens.space2.dp.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val labelWidth = measurables[0].maxIntrinsicWidth(constraints.maxHeight)
        val dateWidth = measurables[1].maxIntrinsicWidth(constraints.maxHeight)
        val timeWidth = measurables[2].maxIntrinsicWidth(constraints.maxHeight)
        if (labelWidth + gap + dateWidth + gap + timeWidth <= constraints.maxWidth) {
            val label =
                measurables[0].measure(
                    loose.copy(
                        maxWidth =
                            constraints.maxWidth - dateWidth - timeWidth - gap * 2,
                    ),
                )
            val datePill = measurables[1].measure(loose.copy(minWidth = dateWidth, maxWidth = dateWidth))
            val timePill = measurables[2].measure(loose.copy(minWidth = timeWidth, maxWidth = timeWidth))
            val height = maxOf(label.height, datePill.height, timePill.height)
            layout(constraints.maxWidth, height) {
                label.placeRelative(0, (height - label.height) / 2)
                datePill.placeRelative(
                    constraints.maxWidth - timePill.width - gap - datePill.width,
                    (height - datePill.height) / 2,
                )
                timePill.placeRelative(constraints.maxWidth - timePill.width, (height - timePill.height) / 2)
            }
        } else {
            val label = measurables[0].measure(loose)
            val width = (constraints.maxWidth - gap) / 2
            val pills = Constraints.fixedWidth(width)
            val datePill = measurables[1].measure(pills)
            val timePill = measurables[2].measure(pills)
            layout(constraints.maxWidth, label.height + gap + maxOf(datePill.height, timePill.height)) {
                label.placeRelative(0, 0)
                datePill.placeRelative(0, label.height + gap)
                timePill.placeRelative(width + gap, label.height + gap)
            }
        }
    }
}

@Composable
private fun WizardDateTimePill(
    field: WizardDateTimeFieldUi,
    kind: WizardPickerKind,
    text: String?,
    spoken: String?,
    copyFormatter: CopyLineFormatter?,
    onClick: () -> Unit,
) {
    val date = kind == WizardPickerKind.Date
    val shape = RoundedCornerShape(WizardMetrics.pillRadius.dp)
    val failure = if (date) field.error?.let { fieldCopy(it, copyFormatter) } else null
    val description =
        WizardCopy(
            when {
                date && field.arrival -> "Arrival date"
                date -> "Departure date"
                field.arrival -> "Arrival time"
                else -> "Departure time"
            },
        ).resolve()
    val empty = WizardCopy("not set").resolve()
    Box(
        Modifier
            .testTag("${field.tag}.${if (date) "date" else "time"}")
            .semantics {
                contentDescription = description
                stateDescription = spoken ?: empty
                failure?.let { error(it) }
            }.cfClickable(
                onClick,
                focusShape = shape,
            ).sizeIn(minHeight = WizardMetrics.target.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text ?: WizardCopy(if (date) "Date" else "Time").resolve(),
            Modifier
                .testTag("${field.tag}.${if (date) "date" else "time"}.visual")
                .fillMaxWidth()
                .background(CfTheme.colors.wizardPickerFill, shape)
                .then(
                    if (failure ==
                        null
                    ) {
                        Modifier
                    } else {
                        Modifier.border(AuthTokens.focusWidth.dp, CfTheme.colors.destructive, shape)
                    },
                ).padding(horizontal = PlanUITokens.space3.dp, vertical = PlanUITokens.wizardControlGap.dp),
            color =
                when {
                    failure != null -> CfTheme.colors.destructive
                    text == null -> CfTheme.colors.mutedForeground
                    else -> CfTheme.colors.foreground
                },
            style = CfTheme.type.style(CfTypeScale.wizardPicker).copy(fontFeatureSettings = "tnum"),
            softWrap = false,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
