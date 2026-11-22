@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.feature.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.layout.BoardMetrics
import net.chronoflow.core.designsystem.system.CfModalBottomSheet
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.ui.R

@BoardMetrics("WizardSheet")
private object FirstTripRemindersMetrics {
    const val BELL_TINT = 0.16f
}

@Composable
fun FirstTripRemindersSheet(
    state: RemindersUiState,
    onEnable: () -> Unit,
    onNotNow: () -> Unit,
) {
    val busy by rememberUpdatedState(state.busy)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { !busy })
    CfModalBottomSheet(onDismissRequest = onNotNow, sheetState = sheetState) {
        FirstTripRemindersSheetContent(state, onEnable, onNotNow)
    }
}

@Composable
fun FirstTripRemindersSheetContent(
    state: RemindersUiState,
    onEnable: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CfTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .testTag("reminders.firstTrip")
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PlanUITokens.space5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = PlanUITokens.space1.dp).padding(top = PlanUITokens.space2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
        ) {
            Box(
                Modifier
                    .size(
                        PlanUITokens.space14.dp,
                    ).background(colors.dawn.copy(alpha = FirstTripRemindersMetrics.BELL_TINT), CircleShape)
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    LucideGlyphs.Bell.imageVector(),
                    null,
                    Modifier.size(PlanUITokens.walkthroughContentGap.dp),
                    tint = colors.dawnText,
                )
            }
            Text(
                stringResource(R.string.reminders_turn_on_reminders_fc061c),
                Modifier.testTag("reminders.firstTrip.title").semantics { heading() },
                style = CfTheme.type.style(CfTypeScale.wizardLongTitle),
                color = colors.foreground,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.reminders_reminders_are_off_when_you_turn_968d52),
                style = CfTheme.type.style(CfTypeScale.screenBodyLg),
                color = colors.mutedForeground,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height((PlanUITokens.wizardContentGap + PlanUITokens.wizardControlGap).dp))
        Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardControlGap.dp)) {
            CfButton(
                stringResource(R.string.reminders_turn_on_reminders),
                onEnable,
                Modifier.testTag("reminders.firstTrip.enable"),
                minHeight = PlanUITokens.wizardButtonHeight.dp,
                fillWidth = true,
                textStyle = CfTheme.type.style(CfTypeScale.wizardButton),
                enabled =
                    !state.busy && state.settings != null,
            )
            CfButton(
                stringResource(R.string.reminders_not_now),
                onNotNow,
                Modifier.testTag("reminders.firstTrip.notNow"),
                style = CfButtonStyle.Ghost,
                minHeight = AuthTokens.touch.dp,
                fillWidth = true,
                textStyle = CfTheme.type.secondaryButton,
            )
        }
    }
}
