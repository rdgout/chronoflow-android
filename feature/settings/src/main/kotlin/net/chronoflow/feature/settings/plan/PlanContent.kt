package net.chronoflow.feature.settings.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfBadge
import net.chronoflow.core.designsystem.component.CfCard
import net.chronoflow.core.designsystem.component.CfNotice
import net.chronoflow.core.designsystem.component.CfNoticeTone
import net.chronoflow.core.designsystem.component.CfSkeleton
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.generated.SettingsTokens
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.ui.R
import net.chronoflow.feature.settings.components.SettingsEyebrow
import net.chronoflow.feature.settings.components.SettingsLoadingRows
import net.chronoflow.feature.settings.components.SettingsStoredState
import net.chronoflow.feature.settings.components.settingsText

@Composable
fun PlanContent(
    state: PlanScreenState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val plan = state.plan
    Column(modifier, verticalArrangement = Arrangement.spacedBy(PlanUITokens.space5.dp)) {
        if (!plan.loaded) {
            PlanLoading()
        } else {
            if (plan.paymentPending) {
                CfNotice(
                    stringResource(R.string.settings_payment_processing),
                    stringResource(R.string.settings_your_credit_shows_up_here_in_a_m_e3e5a6),
                    Modifier.testTag("settings.plan.processing"),
                )
            }
            if (plan.paymentFailed) {
                CfNotice(
                    stringResource(R.string.settings_your_last_payment_failed),
                    stringResource(SettingsPlanText.paymentFailedResource),
                    Modifier.testTag("settings.plan.failed"),
                    tone = CfNoticeTone.Warning,
                )
            }
            if (plan.hasBalance) {
                PlanHero(state)
                if (!plan.unlimited) {
                    Text(
                        stringResource(R.string.settings_credits_and_plans_aren_t_sold_in_b19fa7),
                        Modifier.padding(horizontal = PlanUITokens.space1.dp).testTag("settings.plan.noLink"),
                        style = CfTheme.type.screenBody,
                        color = CfTheme.colors.mutedForeground,
                    )
                }
            } else {
                SettingsStoredState(true, false, onRefresh)
            }
            if (plan.offline) {
                Text(
                    stringResource(R.string.settings_needs_a_connection_your_plans_an_d67bcc),
                    Modifier.testTag("settings.plan.offline"),
                    style = CfTheme.type.screenBody,
                    color = CfTheme.colors.mutedForeground,
                )
            }
        }
    }
}

@Composable
private fun PlanHero(state: PlanScreenState) {
    val plan = state.plan
    CfCard(
        Modifier.testTag("settings.plan.hero"),
        spacing = if (plan.unlimited) PlanUITokens.space3.dp else PlanUITokens.space4.dp,
    ) {
        SettingsEyebrow(stringResource(R.string.settings_your_plan), dawn = true)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
            verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
        ) {
            Text(
                if (plan.unlimited) {
                    stringResource(R.string.settings_unlimited_trips)
                } else {
                    pluralStringResource(R.plurals.settings_n_credits_left, plan.credits ?: 0, plan.credits ?: 0)
                },
                Modifier.testTag("settings.plan.balance"),
                style = CfTheme.type.style(CfTypeScale.screenPageTitle).copy(fontFeatureSettings = "tnum"),
            )
            if (plan.unlimited && plan.cancelled) {
                CfBadge(
                    stringResource(R.string.settings_cancelled),
                    Modifier.align(Alignment.CenterVertically).testTag("settings.plan.cancelled"),
                )
            }
        }
        if (plan.unlimited) {
            state.line?.let { line ->
                Text(
                    settingsText(line.key, mapOf("date" to line.date, "interval" to settingsText(line.interval))),
                    Modifier.testTag("settings.plan.date"),
                    style = CfTheme.type.screenBody.copy(fontFeatureSettings = "tnum"),
                    color = CfTheme.colors.mutedForeground,
                )
            }
        } else {
            Text(
                stringResource(R.string.settings_one_credit_plans_one_direction_o_03b8b1),
                style = CfTheme.type.screenBody,
                color = CfTheme.colors.mutedForeground,
            )
        }
    }
}

@Composable
private fun PlanLoading() {
    val description = stringResource(R.string.settings_loading_your_plan)
    Column(
        Modifier.testTag("settings.plan.loading").clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.space5.dp),
    ) {
        CfCard(shadow = false, spacing = PlanUITokens.space3.dp) {
            CfSkeleton(
                Modifier.size(SettingsTokens.skeletonEyebrowWidth.dp, PlanUITokens.space4.dp),
                CfTheme.shapes.full,
            )
            CfSkeleton(Modifier.size(SettingsTokens.skeletonTitleWidth.dp, SettingsTokens.rowDisc.dp))
            CfSkeleton(Modifier.fillMaxWidth().height(PlanUITokens.space4.dp), CfTheme.shapes.full)
            CfSkeleton(
                Modifier.fillMaxWidth(SettingsTokens.skeletonLineRatio).height(PlanUITokens.space4.dp),
                CfTheme.shapes.full,
            )
            CfSkeleton(
                Modifier
                    .padding(top = PlanUITokens.space2.dp)
                    .fillMaxWidth()
                    .height(PlanUITokens.walkthroughDoneHeight.dp),
                CfTheme.shapes.full,
            )
        }
        SettingsLoadingRows()
    }
}
