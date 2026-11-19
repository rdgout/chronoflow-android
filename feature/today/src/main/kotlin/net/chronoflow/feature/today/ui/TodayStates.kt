package net.chronoflow.feature.today.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.component.CfSkeleton
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyph
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.clock.MinuteClock
import net.chronoflow.feature.today.TodayUiState
import androidx.compose.ui.semantics.testTag as semanticsTestTag

@Composable
internal fun PlanLoadingView(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.screens_loading_today)
    Column(
        modifier
            .clearAndSetSemantics {
                contentDescription = description
                semanticsTestTag = "plan.loading"
            },
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.space4.dp),
    ) {
        CfSkeleton(Modifier.fillMaxWidth().height((PlanUITokens.space1 * 62).dp), CfTheme.shapes.x3l)
        CfSkeleton(
            Modifier.fillMaxWidth(TodayMetrics.loadingLineFraction).height(TodayMetrics.loadingLineHeight),
            CfTheme.shapes.full,
        )
        CfSkeleton(Modifier.fillMaxWidth().height(PlanUITokens.space16.dp), CfTheme.shapes.x2l)
        CfSkeleton(Modifier.fillMaxWidth().height(PlanUITokens.space16.dp), CfTheme.shapes.x2l)
        CfSkeleton(Modifier.fillMaxWidth().height(PlanUITokens.space16.dp), CfTheme.shapes.x2l)
    }
}

@Composable
private fun StateIcon(
    glyph: LucideGlyph,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.size(PlanUITokens.space14.dp).background(CfTheme.colors.muted, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(glyph.imageVector(), null, Modifier.size(PlanUITokens.space6.dp), tint = CfTheme.colors.dawnText)
    }
}

@Composable
internal fun TodayEmptyCard(
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .padding(top = PlanUITokens.space6.dp)
            .todayDashedBorder(thick = true)
            .testTag("today.empty")
            .padding(vertical = PlanUITokens.space8.dp, horizontal = PlanUITokens.space5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
    ) {
        StateIcon(LucideGlyphs.Moon)
        Text(
            stringResource(R.string.screens_no_trips_yet),
            Modifier.semantics { heading() },
            color = CfTheme.colors.foreground,
            style = CfTheme.type.style(CfTypeScale.screenCardTitle),
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.screens_add_your_flights_and_you_get_a_da_737cc7),
            color = CfTheme.colors.mutedForeground,
            style = CfTheme.type.style(CfTypeScale.screenBodyLg),
            textAlign = TextAlign.Center,
        )
        CfButton(
            stringResource(R.string.screens_plan_a_trip),
            onCreate,
            Modifier.testTag("today.create"),
            style = CfButtonStyle.Primary,
            size = CfButtonSize.Lg,
            fillWidth = true,
        )
    }
}

@Composable
internal fun TodayCompletedCard(
    state: TodayUiState.Completed,
    clock: MinuteClock,
    language: Language,
    formatter: CopyLineFormatter,
    onCreate: () -> Unit,
    onPast: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .padding(top = PlanUITokens.space6.dp)
            .todayCard()
            .testTag("today.completed")
            .padding(vertical = PlanUITokens.space8.dp, horizontal = PlanUITokens.space5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
    ) {
        StateIcon(LucideGlyphs.CircleCheckBig)
        Text(
            stringResource(R.string.screens_no_trips_coming_up),
            Modifier.semantics { heading() },
            color = CfTheme.colors.foreground,
            style = CfTheme.type.style(CfTypeScale.screenCardTitle),
            textAlign = TextAlign.Center,
        )
        CompletedBodyText(state.route, state.endDate, clock, language, formatter)
        Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space1.dp)) {
            CfButton(
                stringResource(R.string.screens_plan_a_trip),
                onCreate,
                Modifier.testTag("today.create"),
                style = CfButtonStyle.Primary,
                size = CfButtonSize.Lg,
                minHeight = PlanUITokens.actionMinHeight.dp,
                fillWidth = true,
            )
            CfButton(
                stringResource(R.string.screens_see_past_trips),
                onPast,
                Modifier.testTag("today.past"),
                style = CfButtonStyle.Ghost,
                size = CfButtonSize.Lg,
                minHeight = PlanUITokens.actionMinHeight.dp,
                fillWidth = true,
            )
        }
    }
}

@Composable
internal fun TodayFutureNote(
    state: TodayUiState.Future,
    language: Language,
    formatter: CopyLineFormatter,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.todayCard().testTag("today.future").padding(PlanUITokens.space5.dp),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
    ) {
        Text(
            stringResource(R.string.screens_nothing_to_do_today),
            Modifier.semantics {
                heading()
            },
            color = CfTheme.colors.foreground,
            style = CfTheme.type.style(CfTypeScale.screenSectionTitle),
        )
        val days = state.preparationDays
        val preparation =
            if (days == null ||
                days <= 0
            ) {
                ""
            } else {
                " " +
                    pluralStringResource(
                        R.plurals.screens_your_count_preparation_days_start_df4f1b,
                        days,
                        days,
                        formatter.formatWeekdayDate(state.planStartUtc, language, state.planStartZone),
                    )
            }
        Text(
            stringResource(R.string.screens_keep_your_usual_sleep_times) + preparation,
            color = CfTheme.colors.mutedForeground,
            style = CfTheme.type.screenBody,
        )
    }
}
