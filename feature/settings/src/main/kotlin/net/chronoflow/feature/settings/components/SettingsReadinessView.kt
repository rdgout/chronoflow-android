package net.chronoflow.feature.settings.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.component.CfCard
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.generated.SettingsTokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.ui.R
import net.chronoflow.feature.settings.bodyclock.BodyClockReadiness

@Composable
fun SettingsReadinessView(
    state: BodyClockReadiness,
    onFirst: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CfTheme.colors
    val status = modifier.testTag("settings.bodyClock.readiness").semantics { liveRegion = LiveRegionMode.Polite }
    when (state) {
        is BodyClockReadiness.NeedsAnswers -> {
            CfCard(status, spacing = PlanUITokens.space3.dp) {
                SettingsEyebrow(stringResource(R.string.settings_almost_ready), dawn = true)
                Text(
                    if (state.missingCount == 1) {
                        stringResource(R.string.settings_we_need_1_more_answer_before_we_c7ff63)
                    } else {
                        stringResource(
                            R.string.settings_we_need_count_more_answers_befor_63734e,
                            state.missingCount.toString(),
                        )
                    },
                    style = CfTheme.type.style(CfTypeScale.screenSectionTitle),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Canvas(
                        Modifier
                            .size(PlanUITokens.progressTrackWidth.dp, SettingsTokens.pillGap.dp)
                            .clearAndSetSemantics {},
                    ) {
                        val radius = CornerRadius(size.height / 2)
                        drawRoundRect(colors.muted, cornerRadius = radius)
                        drawRoundRect(
                            colors.dawn,
                            size = Size(size.width * state.done / state.total, size.height),
                            cornerRadius = radius,
                        )
                    }
                    Icon(LucideGlyphs.Check.imageVector(), null, Modifier.size(PlanUITokens.space3.dp))
                    Text(
                        stringResource(
                            R.string.settings_done_total_done,
                            state.done.toString(),
                            state.total.toString(),
                        ),
                        style = CfTheme.type.style(CfTypeScale.screenMeta).copy(fontFeatureSettings = "tnum"),
                        color = colors.mutedForeground,
                    )
                }
                CfButton(
                    stringResource(R.string.settings_go_to_the_first_one),
                    onFirst,
                    Modifier.fillMaxWidth().testTag("settings.bodyClock.firstMissing"),
                    style = CfButtonStyle.Secondary,
                    size = CfButtonSize.Sm,
                    minHeight = PlanUITokens.walkthroughDoneHeight.dp,
                )
            }
        }

        is BodyClockReadiness.Excluded -> {
            Column(
                status
                    .fillMaxWidth()
                    .background(colors.settingsWarningBackground, CfTheme.shapes.x3l)
                    .border(AuthTokens.borderWidth.dp, colors.settingsWarningBorder, CfTheme.shapes.x3l)
                    .padding(PlanUITokens.space5.dp),
                verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
                    Icon(
                        LucideGlyphs.TriangleAlert.imageVector(),
                        null,
                        Modifier.size(PlanUITokens.loadingHeadingHeight.dp),
                        tint = colors.dawnText,
                    )
                    Text(
                        stringResource(R.string.settings_no_plan_available),
                        style = CfTheme.type.style(CfTypeScale.settingsStrong),
                    )
                }
                state.lineKeys.forEachIndexed { index, key ->
                    Text(
                        settingsText(key),
                        Modifier.testTag("settings.bodyClock.exclusion.$index"),
                        style = CfTheme.type.screenBody,
                    )
                }
            }
        }

        is BodyClockReadiness.Ready -> {
            CfCard(status) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(SettingsTokens.rowDisc.dp)
                            .background(colors.dawn, CfTheme.shapes.full)
                            .testTag("settings.bodyClock.readiness.check"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            LucideGlyphs.Check.imageVector(),
                            null,
                            Modifier.size(PlanUITokens.loadingHeadingHeight.dp),
                            tint = colors.dawnForeground,
                        )
                    }
                    Column(Modifier.weight(1f).testTag("settings.bodyClock.readiness.details")) {
                        Text(
                            stringResource(
                                if (state.adjusted) {
                                    R.string.settings_your_plan_is_adjusted_for_your_p_1d3dd4
                                } else {
                                    R.string.settings_required_answers_complete
                                },
                            ),
                            style = CfTheme.type.style(CfTypeScale.settingsStrong),
                        )
                        Text(
                            stringResource(R.string.settings_your_age_range_and_shift_work_an_8024ff),
                            style = CfTheme.type.screenBody,
                            color = colors.mutedForeground,
                        )
                    }
                }
                state.adjustmentKeys.forEachIndexed { index, key ->
                    Text(
                        settingsText(key),
                        Modifier.testTag("settings.bodyClock.adjustment.$index"),
                        style = CfTheme.type.screenBody,
                    )
                }
            }
        }
    }
}
