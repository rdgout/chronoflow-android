@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard.times

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withContext
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.LocalReduceMotion
import net.chronoflow.feature.wizard.components.WizardBanner
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve
import net.chronoflow.feature.wizard.ui.wizardCopy
import kotlin.coroutines.EmptyCoroutineContext

private object InstantWizardScroll : MotionDurationScale {
    override val scaleFactor = 0f
}

@Composable
internal fun WizardTimesStep(
    ui: WizardTimesUi,
    onEvent: (WizardEvent) -> Unit,
) {
    val pick =
        remember(onEvent) {
            { field: WizardDateTimeFieldUi, kind: WizardPickerKind ->
                onEvent(WizardEvent.OpenFlightPicker(WizardPicker(field.index, field.arrival, kind)))
            }
        }
    val requesters = remember(ui.cards.map { it.id }) { ui.cards.associate { it.index to BringIntoViewRequester() } }
    var handled by rememberSaveable { mutableIntStateOf(ui.continueFailures) }
    val haptics = LocalHaptics.current
    val reduce = LocalReduceMotion.current
    LaunchedEffect(ui.continueFailures) {
        if (ui.continueFailures < handled) {
            handled = ui.continueFailures
        } else if (ui.continueFailures > handled) {
            handled = ui.continueFailures
            haptics.perform(HapticEvent.Warning)
            withFrameNanos { }
            val first = ui.cards.firstOrNull { it.hasErrors }
            if (first !=
                null
            ) {
                withContext(if (reduce) InstantWizardScroll else EmptyCoroutineContext) {
                    requesters.getValue(first.index).bringIntoView()
                }
            }
        }
    }
    ui.picker?.let { presentation ->
        val picker = presentation.picker
        ui.cards.firstOrNull { it.index == picker.index }?.let { card ->
            key(picker) {
                WizardFlightPickers(
                    presentation,
                    if (picker.arrival) card.arrival else card.departure,
                    onEvent,
                    { onEvent(WizardEvent.DismissFlightPicker) },
                )
            }
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardContentGap.dp)) {
        ui.banner?.let { WizardBanner(it.wizardCopy().resolve(), "wizard.${ui.id}.past") }
        ui.cards.forEachIndexed { position, card ->
            key(card.id) {
                if (position > 0) WizardConnectionChip(card.connection, card.index)
                WizardFlightCard(card, ui.timeFormat, requesters.getValue(card.index), pick)
            }
        }
        ui.flightMinutes?.let { minutes ->
            Text(
                WizardCopy("Flight time {{d}}", mapOf("d" to wizardDuration(minutes))).resolve(),
                Modifier.testTag("wizard.flight-time").padding(horizontal = PlanUITokens.space1.dp).layout {
                    measurable,
                    constraints,
                    ->
                    val placeable = measurable.measure(constraints)
                    val pull = PlanUITokens.space1.dp.roundToPx()
                    layout(
                        placeable.width,
                        (placeable.height - pull).coerceAtLeast(0),
                    ) { placeable.placeRelative(0, -pull) }
                },
                color = CfTheme.colors.mutedForeground,
                style = CfTheme.type.style(CfTypeScale.wizardHint).copy(fontFeatureSettings = "tnum"),
            )
        }
        if (ui.cards.size > 2) Spacer(Modifier.height(PlanUITokens.space6.dp))
    }
}
