package net.chronoflow.feature.trip.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.chronoflow.core.designsystem.generated.CfColors
import net.chronoflow.core.plan.PreparedDay
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.ui.R
import net.chronoflow.feature.trip.timeline.host.TimelineHostModel
import net.chronoflow.feature.trip.timeline.host.TimelineHostViewModel

/** The plain pinned day line; the designed header is 6b's. It reads no scroll state: the list pins it. */
@Composable
internal fun DayHeaderLine(
    day: PreparedDay,
    colors: CfColors,
    minHeight: Dp,
) {
    val probe = LocalTimelineProbe.current
    SideEffect { probe?.composed(TimelineProbe.Kind.HEADER, day.id) }
    Box(
        Modifier
            .fillMaxWidth()
            .background(colors.card)
            .heightIn(min = minHeight)
            .semantics(mergeDescendants = true) { heading() },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = stringResource(R.string.plan_day_title_date, day.title, day.date),
            style = TimelineStyles.planListTitle,
            color = colors.foreground,
        )
    }
}

/**
 * The minimal day menu (iOS `TripPageView.swift:266-342`): the route title with the accessibility label and value of
 * l.317-324, and an M3 menu of every day. It is the only reader of the anchor day and of the "scrolled" boundary, so
 * scrolling recomposes it at most once per boundary.
 */
@Composable
internal fun DayMenu(
    viewModel: TimelineHostViewModel,
    model: TimelineHostModel,
    listState: LazyListState,
    thresholdPx: Int,
    colors: CfColors,
    onPick: (Int) -> Unit,
) {
    val anchorId by viewModel.anchorDayId.collectAsStateWithLifecycle()
    val probe = LocalTimelineProbe.current
    SideEffect { probe?.composed(TimelineProbe.Kind.ANCHOR, anchorId ?: "") }
    val index = model.index
    val days = model.prepared.days
    val trip = model.detail.trip
    val anchorPosition = anchorId?.let { index.position(it) }
    val anchor = anchorPosition?.let { days[it] }
    val scrolled by remember(listState, index, anchorId, thresholdPx) {
        derivedStateOf {
            val id = anchorId
            if (id == null) {
                false
            } else {
                val dayItem = index.dayIndex(id)
                listState.layoutInfo.visibleItemsInfo.any { it.index == dayItem && it.offset < thresholdPx }
            }
        }
    }
    val from = trip.from.city ?: trip.from.iata
    val to = trip.to.city ?: trip.to.iata
    val description =
        if (scrolled && anchor != null) {
            stringResource(R.string.plan_x_to_x_x_x_go_to_a_day, from, to, anchor.title, anchor.date)
        } else {
            stringResource(R.string.plan_x_to_x_x_go_to_a_day, from, to, model.prepared.spokenDates)
        }
    val value = anchor?.let { stringResource(R.string.plan_day_title_date, it.title, it.date) }
    val goTo = stringResource(R.string.plan_go_to_a_day)
    var expanded by remember { mutableStateOf(false) }
    val menuScroll = rememberScrollState()
    val density = LocalDensity.current
    LaunchedEffect(expanded) {
        if (expanded && anchorPosition != null) {
            menuScroll.scrollTo(anchorPosition * with(density) { PlanMetrics.minTouchTarget.dp.roundToPx() })
        }
    }
    Box {
        Text(
            text = stringResource(R.string.plan_x_x, trip.from.iata, trip.to.iata),
            style = TimelineStyles.planListTitle,
            color = colors.foreground,
            modifier =
                Modifier
                    .testTag("trip.dayMenu")
                    .minimumInteractiveComponentSize()
                    .clickable(role = Role.Button, onClickLabel = goTo) { expanded = true }
                    .semantics(mergeDescendants = true) {
                        contentDescription = description
                        if (value != null) stateDescription = value
                    },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = colors.popover,
            scrollState = menuScroll,
        ) {
            Text(
                text = goTo,
                style = TimelineStyles.planSubtitle,
                color = colors.mutedForeground,
                modifier = Modifier.padding(MenuDefaults.DropdownMenuItemContentPadding),
            )
            for ((position, day) in days.withIndex()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.plan_day_title_date, day.title, day.date)) },
                    onClick = {
                        expanded = false
                        onPick(position)
                    },
                    colors = MenuDefaults.itemColors(textColor = colors.foreground),
                    modifier =
                        Modifier
                            .testTag("trip.dayMenu.day.$position")
                            .semantics { selected = day.id == anchorId },
                )
            }
        }
    }
}
