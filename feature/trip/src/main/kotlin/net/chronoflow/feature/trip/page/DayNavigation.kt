package net.chronoflow.feature.trip.page

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfIconButton
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.LocalReduceMotion
import net.chronoflow.core.plan.PreparedDay
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.ui.R
import net.chronoflow.feature.trip.timeline.LocalTimelineProbe
import net.chronoflow.feature.trip.timeline.TimelineProbe.Kind

@Composable
fun DayNavigation(
    days: List<PreparedDay>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val probe = LocalTimelineProbe.current
    SideEffect { probe?.composed(Kind.PILLS, selectedId ?: "") }
    val rowState = rememberLazyListState()
    val reduceMotion = LocalReduceMotion.current
    val selectedIndex = days.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    val description = stringResource(R.string.plan_plan_days)
    val colors = CfTheme.colors
    LaunchedEffect(selectedIndex, days, reduceMotion) {
        if (days.isEmpty()) return@LaunchedEffect
        if (rowState.layoutInfo.visibleItemsInfo.none { it.index == selectedIndex }) {
            rowState.scrollToItem(
                selectedIndex,
            )
        }
        val info = rowState.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == selectedIndex } ?: return@LaunchedEffect
        val delta = item.offset + item.size / 2f - (info.viewportStartOffset + info.viewportEndOffset) / 2f
        if (reduceMotion) rowState.scrollBy(delta) else rowState.animateScrollBy(delta)
    }
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(PlanMetrics.smallGap.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CfIconButton(
            LucideGlyphs.ChevronLeft.imageVector(),
            stringResource(R.string.plan_previous_day),
            { onSelect(days[selectedIndex - 1].id) },
            Modifier.testTag("trip.days.previous"),
            enabled =
                selectedIndex > 0,
            iconSize = TripPageMetrics.chevron,
        )
        LazyRow(
            Modifier.weight(1f).semantics { contentDescription = description }.testTag("trip.days"),
            state = rowState,
            horizontalArrangement = Arrangement.spacedBy(PlanMetrics.rowGap.dp),
        ) {
            itemsIndexed(days, key = { _, day -> day.id }, contentType = { _, _ -> "dayPill" }) { index, day ->
                val chosen = day.id == selectedId
                val spoken = stringResource(R.string.plan_title_comma_date, day.title, day.date)
                val ink = if (chosen) colors.dawnForeground else colors.foreground
                Column(
                    Modifier
                        .minimumInteractiveComponentSize()
                        .heightIn(min = PlanMetrics.touch.dp)
                        .background(if (chosen) colors.dawn else colors.card, CircleShape)
                        .border(PlanMetrics.border.dp, colors.border, CircleShape)
                        .clickable(role = Role.Tab) { onSelect(day.id) }
                        .semantics(mergeDescendants = true) {
                            contentDescription = spoken
                            selected = chosen
                        }.padding(horizontal = PlanMetrics.gutter.dp)
                        .testTag("trip.days.pill.$index"),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(day.title, style = CfTheme.type.authStrong, color = ink)
                    Text(day.date, style = CfTheme.type.planSubtitle, color = ink)
                }
            }
        }
        CfIconButton(
            LucideGlyphs.ChevronRight.imageVector(),
            stringResource(R.string.plan_next_day),
            { onSelect(days[selectedIndex + 1].id) },
            Modifier.testTag("trip.days.next"),
            enabled = selectedIndex + 1 < days.size,
            iconSize = TripPageMetrics.chevron,
        )
    }
}
