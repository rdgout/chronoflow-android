package net.chronoflow.feature.trip.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.plan.trippage.TripPageLayout
import net.chronoflow.core.plan.trippage.TripPageState
import net.chronoflow.core.ui.R
import net.chronoflow.feature.trip.page.TripPageEvent
import net.chronoflow.feature.trip.page.TripPageScreenState

@Composable
internal fun TripPageStates(
    state: TripPageScreenState,
    onEvent: (TripPageEvent) -> Unit,
) {
    val safe = LocalSafeInsets.current
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    BoxWithConstraints(Modifier.fillMaxSize().testTag("trip.page")) {
        val gutter = TripPageLayout.resolve(maxWidth.value.toDouble()).gutter.dp
        val left = with(density) { safe.getLeft(this, direction).toDp() }
        val right = with(density) { safe.getRight(this, direction).toDp() }
        val bottom = with(density) { safe.getBottom(this).toDp() }
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxSize().testTag("trip.scroll"),
            contentPadding = PaddingValues(start = gutter + left, end = gutter + right, bottom = gutter + bottom),
            verticalArrangement = Arrangement.spacedBy(PlanMetrics.sectionGap.dp),
        ) {
            if (state.ui.page.metadata
                    .isNotEmpty()
            ) {
                item("meta", "meta") {
                    Text(
                        state.ui.page.metadata,
                        Modifier.fillMaxWidth().testTag("trip.meta"),
                        style = CfTheme.type.planSubtitle,
                        color = CfTheme.colors.mutedForeground,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            if (state.ui.state == TripPageState.NoPlan) {
                item("noPlan", "stateCard") { NoPlanCard() }
            } else {
                if (state.ui.state == TripPageState.RemoteLoading || state.ui.state == TripPageState.RemoteOffline) {
                    item("remoteSummary", "remoteSummary") { RemoteSummaryCard(state.ui.page, state.summaryClock) }
                }
                if (state.ui.state == TripPageState.RemoteOffline) {
                    item("offline", "stateCard") { OfflineCard { onEvent(TripPageEvent.Retry) } }
                } else {
                    item("skeleton", "skeleton") { TripPageSkeleton(state.ui.state == TripPageState.RemoteLoading) }
                }
            }
        }
    }
}

@Composable
private fun NoPlanCard() {
    TripPageStateCard(
        LucideGlyphs.Calendar,
        CfTheme.colors.dawnText,
        stringResource(R.string.plan_no_plan_yet),
        CfTheme.type.planCardTitle,
        stringResource(R.string.plan_your_flights_are_saved_check_your_an_0357a9),
        "trip.noPlan.title",
        Modifier.testTag("trip.noPlan"),
    ) {
        CfButton(
            stringResource(R.string.plan_check_answers_and_make_plan),
            {},
            Modifier.testTag("trip.noPlan.makePlan"),
            style = CfButtonStyle.Primary,
            size = CfButtonSize.Lg,
            enabled = false,
            fillWidth = true,
            minHeight = PlanMetrics.minTouchTarget.dp,
        )
        Text(
            stringResource(R.string.plan_making_the_plan_needs_a_connection),
            style = CfTheme.type.planListLine,
            color = CfTheme.colors.foreground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OfflineCard(onRetry: () -> Unit) {
    TripPageStateCard(
        LucideGlyphs.CloudUpload,
        CfTheme.colors.mutedForeground,
        stringResource(R.string.plan_this_plan_downloads_when_youre_online),
        CfTheme.type.planRemoteTitle,
        stringResource(R.string.plan_plans_for_active_trips_and_trips_in_aae908),
        "trip.remote.offline.title",
        Modifier.testTag("trip.remote.offline"),
    ) {
        CfButton(
            stringResource(R.string.plan_try_again),
            onRetry,
            Modifier.testTag("trip.remote.retry"),
            style = CfButtonStyle.Outline,
            size = CfButtonSize.Touch,
        )
        Text(
            stringResource(R.string.plan_your_other_trips_keep_working_offline),
            style = CfTheme.type.planListLine,
            color = CfTheme.colors.foreground,
            textAlign = TextAlign.Center,
        )
    }
}
