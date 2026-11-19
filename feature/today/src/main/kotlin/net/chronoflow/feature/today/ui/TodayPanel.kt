package net.chronoflow.feature.today.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.system.CfLinearProgress
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.tnum
import net.chronoflow.core.model.contracts.DayStatus
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.ui.R
import net.chronoflow.feature.today.HiddenNotice
import net.chronoflow.feature.today.ProgressUi
import net.chronoflow.feature.today.TodayPanelUi
import androidx.compose.ui.semantics.testTag as semanticsTestTag

@Composable
internal fun TodayPanelHeader(
    panel: TodayPanelUi,
    modifier: Modifier = Modifier,
) {
    val todayProbe = LocalTodayProbe.current
    androidx.compose.runtime.SideEffect { todayProbe?.composed(TodayProbe.Kind.PANEL, "panel") }
    Column(modifier) {
        panel.dayLabel?.let { label ->
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
                verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    label,
                    Modifier.semantics {
                        heading()
                    },
                    color = CfTheme.colors.foreground,
                    style = CfTheme.type.style(CfTypeScale.screenSectionTitle),
                )
                panel.dayStatus?.let { DayStatusPill(it) }
            }
            panel.progress?.let { DayProgressRow(it, Modifier.padding(top = PlanUITokens.space1.dp)) }
        }
    }
}

@Composable
internal fun DayStatusPill(
    status: DayStatus,
    modifier: Modifier = Modifier,
) {
    val colors = CfTheme.colors
    val adapted = status == DayStatus.Adapted || status == DayStatus.AdaptedSlowerClock
    val title =
        when (status) {
            DayStatus.Adapting -> R.string.screens_adjusting
            DayStatus.PartiallyAdapted -> R.string.screens_partly_adapted
            DayStatus.Adapted -> R.string.screens_expected_adapted
            DayStatus.AdaptedSlowerClock -> R.string.screens_protect_a_slower_clock
            else -> return
        }
    val shape = CfTheme.shapes.full
    val fill =
        when {
            adapted -> colors.dawn
            status == DayStatus.PartiallyAdapted -> colors.secondary
            else -> androidx.compose.ui.graphics.Color.Transparent
        }
    val content =
        when {
            adapted -> colors.dawnForeground
            status == DayStatus.PartiallyAdapted -> colors.secondaryForeground
            else -> colors.foreground
        }
    Row(
        modifier
            .background(fill, shape)
            .then(
                if (status ==
                    DayStatus.Adapting
                ) {
                    Modifier.border(
                        AuthTokens.borderWidth.dp,
                        colors.dawn.copy(alpha = PlanUITokens.statusBorderOpacity),
                        shape,
                    )
                } else {
                    Modifier
                },
            ).padding(horizontal = PlanUITokens.space2.dp, vertical = (PlanUITokens.space1 / 2).dp),
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (adapted) {
            Icon(
                LucideGlyphs.Check.imageVector(),
                null,
                Modifier.size(TodayMetrics.statusIcon),
                tint = content,
            )
        }
        Text(
            stringResource(title),
            color = content,
            style = CfTheme.type.style(CfTypeScale.screenMeta).copy(fontWeight = FontWeight.Medium),
        )
    }
}

@Composable
internal fun DayProgressRow(
    progress: ProgressUi,
    modifier: Modifier = Modifier,
) {
    val skipped =
        if (progress.skipped >
            0
        ) {
            pluralStringResource(R.plurals.screens_count_skipped, progress.skipped, progress.skipped)
        } else {
            null
        }
    val spoken =
        stringResource(R.string.screens_done_of_total_done, progress.done, progress.total) +
            (skipped?.let { ", $it" } ?: "")
    FlowRow(
        modifier
            .clearAndSetSemantics {
                contentDescription = spoken
                semanticsTestTag = "today.progress"
            },
        horizontalArrangement =
            Arrangement.spacedBy(
                PlanUITokens.space2.dp,
            ),
        verticalArrangement =
            Arrangement.spacedBy(
                PlanUITokens.space1.dp,
            ),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        CfLinearProgress({
            progress.done.toFloat() / progress.total
        }, Modifier.width(PlanUITokens.progressTrackWidth.dp).height(TodayMetrics.progressHeight))
        Row(
            horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                LucideGlyphs.Check.imageVector(),
                null,
                Modifier.size(TodayMetrics.statusIcon),
                tint = CfTheme.colors.mutedForeground,
            )
            Text(
                stringResource(R.string.screens_done_total_done, progress.done, progress.total),
                color = CfTheme.colors.mutedForeground,
                style =
                    CfTheme.type
                        .style(CfTypeScale.screenMeta)
                        .copy(fontWeight = FontWeight.Medium)
                        .tnum(),
            )
        }
        if (skipped !=
            null
        ) {
            Text(
                skipped,
                color = CfTheme.colors.mutedForeground,
                style = CfTheme.type.style(CfTypeScale.screenMeta).tnum(),
            )
        }
    }
}

@Composable
internal fun TodaySectionHeading(
    title: Int,
    language: Language,
    modifier: Modifier = Modifier,
) {
    Text(
        stringResource(title).uppercase(languageLocale(language)),
        modifier.semantics {
            heading()
        },
        color = CfTheme.colors.mutedForeground,
        style = CfTheme.type.screenLabel.copy(letterSpacing = TodayMetrics.sectionTracking),
    )
}

@Composable
internal fun NothingNow(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().todayDashedBorder().padding(PlanUITokens.space6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
    ) {
        Icon(
            LucideGlyphs.CalendarCheck.imageVector(),
            null,
            Modifier.size(PlanUITokens.space6.dp),
            tint = CfTheme.colors.mutedForeground,
        )
        Text(
            stringResource(R.string.screens_nothing_scheduled_right_now),
            color = CfTheme.colors.mutedForeground,
            style = CfTheme.type.style(CfTypeScale.screenBodyLg),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun HiddenStepsLink(
    hidden: HiddenNotice,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .background(
                CfTheme.colors.surface2,
                CfTheme.shapes.xl,
            ).cfClickable(onSettings, Role.Button, focusShape = CfTheme.shapes.xl)
            .semantics(mergeDescendants = true) {}
            .testTag("today.hidden")
            .heightIn(min = PlanUITokens.space14.dp)
            .padding(vertical = PlanUITokens.space2.dp, horizontal = PlanUITokens.space3.dp),
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            LucideGlyphs.EyeOff.imageVector(),
            null,
            Modifier.size(TodayMetrics.hiddenIcon),
            tint = CfTheme.colors.mutedForeground,
        )
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(
                    if (hidden ==
                        HiddenNotice.All
                    ) {
                        R.string.screens_all_categories_are_hidden
                    } else {
                        R.string.screens_some_important_steps_are_hidden
                    },
                ),
                color = CfTheme.colors.foreground,
                style = CfTheme.type.screenBody.copy(fontWeight = FontWeight.Medium),
            )
            Text(
                stringResource(R.string.screens_show_them_in_settings_preferences),
                color = CfTheme.colors.mutedForeground,
                style =
                    CfTheme.type.screenBody.copy(
                        fontSize = TodayMetrics.hiddenSubtitleSize,
                        lineHeight = TodayMetrics.hiddenSubtitleLineHeight,
                    ),
            )
        }
        Icon(
            LucideGlyphs.ChevronRight.imageVector(),
            null,
            Modifier.size(TodayMetrics.hiddenIcon),
            tint = CfTheme.colors.mutedForeground,
        )
    }
}
