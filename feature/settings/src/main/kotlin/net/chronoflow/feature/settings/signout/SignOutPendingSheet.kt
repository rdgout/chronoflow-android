@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.feature.settings.signout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.layout.BoardMetrics
import net.chronoflow.core.designsystem.system.CfModalBottomSheet
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.plan.stepLabelKey
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.plan.CategorySwatch
import net.chronoflow.core.ui.plan.CategorySwatchSize
import net.chronoflow.core.ui.plan.planScreenText
import net.chronoflow.feature.settings.home.PendingChange
import net.chronoflow.feature.settings.home.PendingText

@BoardMetrics("SignOutPending", "SignOutPendingSettings")
object SignOutPendingMetrics {
    val TOP = PlanUITokens.space3
    val BOTTOM = PlanUITokens.space8
}

@Composable
fun SignOutPendingSheet(
    items: List<PendingChange>,
    busy: Boolean,
    onStay: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentBusy by rememberUpdatedState(busy)
    val sheet =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { it != SheetValue.Hidden || !currentBusy },
        )
    CfModalBottomSheet({ if (!busy) onStay() }, sheetState = sheet, modifier = modifier) {
        SignOutPendingContent(items, busy, onStay, onDiscard)
        BackHandler(enabled = busy) {}
    }
}

@Composable
fun SignOutPendingContent(
    items: List<PendingChange>,
    busy: Boolean,
    onStay: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .testTag("settings.pending")
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = PlanUITokens.space5.dp,
            ).padding(top = SignOutPendingMetrics.TOP.dp, bottom = SignOutPendingMetrics.BOTTOM.dp),
        verticalArrangement = Arrangement.spacedBy(AuthTokens.cardGap.dp),
    ) {
        Text(
            pluralStringResource(R.plurals.settings_sign_out_and_lose_n_changes, items.size, items.size),
            Modifier.testTag("settings.pending.title").semantics {
                heading()
            },
            style = CfTheme.type.style(CfTypeScale.screenCardTitle),
        )
        Text(
            stringResource(R.string.settings_these_haven_t_reached_your_accou_c0e36b),
            style = CfTheme.type.style(CfTypeScale.screenBodyLg),
            color = CfTheme.colors.mutedForeground,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .testTag(
                    "settings.pending.items",
                ).background(CfTheme.colors.surface2, CfTheme.shapes.xl)
                .padding(horizontal = PlanUITokens.space4.dp, vertical = PlanUITokens.space3.dp),
            verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
        ) {
            items.forEach { item ->
                key(item.id) {
                    Row(
                        Modifier.testTag("settings.pending.item.${item.id}"),
                        horizontalArrangement = Arrangement.spacedBy(AuthTokens.bannerGap.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        item.category?.let { CategorySwatch(it, size = CategorySwatchSize.Small) }
                        Text(pendingText(item.text), Modifier.weight(1f), style = CfTheme.type.screenBody)
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(AuthTokens.brandGap.dp)) {
            CfButton(
                stringResource(R.string.settings_stay_signed_in),
                onStay,
                Modifier.testTag("settings.pending.stay"),
                enabled = !busy,
                fillWidth = true,
                minHeight = PlanUITokens.actionMinHeight.dp,
            )
            val discard = stringResource(R.string.settings_sign_out_and_discard)
            CfButton(
                discard,
                onDiscard,
                Modifier.testTag("settings.pending.discard"),
                style = CfButtonStyle.GhostDestructive,
                enabled = !busy,
                loading = busy,
                loadingDescription = discard,
                fillWidth = true,
                minHeight = PlanUITokens.actionMinHeight.dp,
            )
        }
        Text(
            stringResource(R.string.settings_changes_sync_by_themselves_when_1009db),
            Modifier.fillMaxWidth(),
            style = CfTheme.type.style(CfTypeScale.settingsHint),
            color = CfTheme.colors.mutedForeground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun pendingText(text: PendingText): String =
    when (text) {
        PendingText.Generic -> {
            stringResource(R.string.settings_a_change_waiting_to_sync)
        }

        PendingText.Sleep -> {
            stringResource(R.string.settings_changed_your_usual_sleep_times)
        }

        is PendingText.CheckedOff -> {
            stringResource(
                R.string.settings_checked_off_step,
                planScreenText(checkNotNull(stepLabelKey(text.type))),
            )
        }
    }
