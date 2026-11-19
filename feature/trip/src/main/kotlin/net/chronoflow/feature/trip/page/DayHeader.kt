package net.chronoflow.feature.trip.page

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.plan.PreparedDay
import net.chronoflow.core.plan.PreparedNotice
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.ui.R
import net.chronoflow.feature.trip.timeline.LocalTimelineProbe
import net.chronoflow.feature.trip.timeline.TimelineProbe.Kind

@Composable
fun DayHeader(
    day: PreparedDay,
    modifier: Modifier = Modifier,
) {
    val probe = LocalTimelineProbe.current
    SideEffect { probe?.composed(Kind.HEADER, day.id) }
    var expanded by rememberSaveable(day.id) { mutableStateOf(false) }
    var notesOpen by rememberSaveable(day.id) { mutableStateOf(false) }
    val colors = CfTheme.colors
    val type = CfTheme.type
    val title = stringResource(R.string.plan_day_title_date, day.title, day.date)
    Column(
        modifier.fillMaxWidth().background(colors.background).padding(vertical = PlanMetrics.rowGap.dp),
        verticalArrangement = Arrangement.spacedBy(PlanMetrics.smallGap.dp),
    ) {
        HeadingZoneLayout(heading = {
            val heading =
                buildAnnotatedString {
                    withStyle(
                        type.planDayTitle.toSpanStyle().merge(SpanStyle(color = colors.foreground)),
                    ) { append(day.title) }
                    withStyle(
                        type.planListLine.toSpanStyle().merge(
                            SpanStyle(color = colors.mutedForeground, fontFeatureSettings = "tnum"),
                        ),
                    ) {
                        append(title.removePrefix(day.title))
                    }
                }
            Text(
                heading,
                Modifier.semantics { heading() }.testTag("trip.dayHeader.${day.id}"),
                style = type.planDayTitle,
            )
        }, zone = {
            Text(
                day.zone,
                Modifier.background(colors.muted, CircleShape).padding(PlanMetrics.smallGap.dp),
                style = type.planSubtitle,
                color = colors.foreground,
            )
        })
        if (day.warningCount > 0) {
            NoticeChip(
                pluralStringResource(R.plurals.plan_n_warnings, day.warningCount, day.warningCount),
                true,
                { expanded = !expanded },
                Modifier.testTag("trip.dayHeader.warnings.${day.id}"),
            )
        }
        if (expanded) {
            day.status?.let {
                Text(
                    it,
                    Modifier.testTag("trip.dayHeader.status.${day.id}"),
                    style = type.authStrong,
                    color = colors.dawnText,
                )
            }
            Text(
                day.summary,
                Modifier.testTag("trip.dayHeader.summary.${day.id}"),
                style = type.planSubtitle.copy(fontFeatureSettings = "tnum"),
                color = colors.foreground,
            )
            Text(
                day.progress,
                Modifier.testTag("trip.dayHeader.progress.${day.id}"),
                style = type.planSubtitle,
                color = colors.foreground,
            )
            day.headline?.let { Text(it, style = type.planSubtitle, color = colors.dawnText) }
            if (day.warningCount == 0 && day.notices.isNotEmpty()) {
                NoticeChip(
                    pluralStringResource(R.plurals.plan_n_notes, day.notices.size, day.notices.size),
                    false,
                    { notesOpen = !notesOpen },
                    Modifier.testTag("trip.dayHeader.notes.${day.id}"),
                )
            }
            day.shownNotices(expanded, notesOpen).forEachIndexed { index, notice ->
                DayNotice(notice, Modifier.testTag("trip.dayHeader.notice.${day.id}.$index"))
            }
        }
        val more = stringResource(if (expanded) R.string.plan_show_less else R.string.plan_show_more)
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = TripPageMetrics.moreHeight)
                .minimumInteractiveComponentSize()
                .clickable(role = Role.Button) { expanded = !expanded }
                .semantics { stateDescription = more }
                .testTag("trip.dayHeader.more.${day.id}"),
            contentAlignment = Alignment.Center,
        ) {
            Text(more, style = type.planSubtitle, color = colors.mutedForeground)
        }
    }
}

@Composable
private fun NoticeChip(
    text: String,
    warning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CfTheme.colors
    Box(
        modifier.minimumInteractiveComponentSize().clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .height(TripPageMetrics.chipHeight)
                .background(
                    if (warning) colors.dawn.copy(alpha = PlanMetrics.warningOpacity.toFloat()) else colors.card,
                    CircleShape,
                ).border(
                    PlanMetrics.border.dp,
                    if (warning) colors.dawn.copy(alpha = PlanMetrics.tagOpacity.toFloat()) else colors.border,
                    CircleShape,
                ).padding(horizontal = TripPageMetrics.chipPaddingH),
            horizontalArrangement = Arrangement.spacedBy(PlanMetrics.smallGap.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                (if (warning) LucideGlyphs.TriangleAlert else LucideGlyphs.Info).imageVector(),
                null,
                Modifier.size(PlanMetrics.blockIcon.dp),
                tint = if (warning) colors.dawnText else colors.foreground,
            )
            Text(text, style = CfTheme.type.settingsBadge, color = colors.foreground)
        }
    }
}

@Composable
private fun DayNotice(
    notice: PreparedNotice,
    modifier: Modifier = Modifier,
) {
    val colors = CfTheme.colors
    val shape = RoundedCornerShape(PlanMetrics.rowRadius.dp)
    Row(
        modifier
            .fillMaxWidth()
            .background(
                if (notice.warning) colors.dawn.copy(alpha = PlanMetrics.warningOpacity.toFloat()) else colors.card,
                shape,
            ).border(
                PlanMetrics.border.dp,
                if (notice.warning) colors.dawn.copy(alpha = PlanMetrics.tagOpacity.toFloat()) else colors.border,
                shape,
            ).padding(PlanMetrics.sectionGap.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(PlanMetrics.sectionGap.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            (if (notice.warning) LucideGlyphs.TriangleAlert else LucideGlyphs.Info).imageVector(),
            null,
            Modifier.size(PlanMetrics.icon.dp),
            tint = if (notice.warning) colors.dawnText else colors.mutedForeground,
        )
        Text(notice.text, Modifier.weight(1f), style = CfTheme.type.planListLine, color = colors.foreground)
    }
}
