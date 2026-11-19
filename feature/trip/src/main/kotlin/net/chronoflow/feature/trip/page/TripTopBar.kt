
package net.chronoflow.feature.trip.page

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import net.chronoflow.core.designsystem.component.CfIconButton
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.MaterialSymbols
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.semantics.ellipsizable
import net.chronoflow.core.designsystem.system.CfBackButton
import net.chronoflow.core.designsystem.system.CfCenterAlignedTopAppBar
import net.chronoflow.core.designsystem.system.CfDropdownMenu
import net.chronoflow.core.designsystem.system.CfDropdownMenuItem
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.plan.PreparedDay
import net.chronoflow.core.plan.PreparedPlan
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.ui.R
import net.chronoflow.feature.trip.timeline.LocalTimelineProbe
import net.chronoflow.feature.trip.timeline.TimelineProbe.Kind

@Composable
fun TripTopBar(
    title: TripTitleParts?,
    prepared: PreparedPlan?,
    ready: Boolean,
    anchorDay: PreparedDay?,
    headerScrolled: Boolean,
    onBack: () -> Unit,
    onPickDay: (Int) -> Unit,
    onActions: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    CfCenterAlignedTopAppBar(
        title = { DayMenuAnchor(title, prepared, ready, anchorDay, headerScrolled, onPickDay) },
        modifier = modifier,
        navigationIcon = {
            CfBackButton(
                stringResource(R.string.screens_my_trips),
                onBack,
                Modifier.testTag("trip.back"),
            )
        },
        actions = {
            CfIconButton(
                MaterialSymbols.MoreVert,
                stringResource(R.string.plan_trip_actions),
                { onActions?.invoke() },
                Modifier.testTag("trip.actions.open"),
                enabled = onActions != null,
            )
        },
    )
}

@Composable
private fun DayMenuAnchor(
    title: TripTitleParts?,
    prepared: PreparedPlan?,
    ready: Boolean,
    anchorDay: PreparedDay?,
    headerScrolled: Boolean,
    onPickDay: (Int) -> Unit,
) {
    val probe = LocalTimelineProbe.current
    SideEffect { probe?.composed(Kind.ANCHOR, anchorDay?.id ?: "") }
    val days = prepared?.days.orEmpty()
    var expanded by remember(days) { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val colors = CfTheme.colors
    val goTo = stringResource(R.string.plan_go_to_a_day)
    val code =
        title?.let { stringResource(R.string.plan_x_x, it.fromIata, it.toIata) }
            ?: stringResource(R.string.plan_loading_your_plan)
    val dayTitle = anchorDay?.let { stringResource(R.string.plan_day_title_date, it.title, it.date) }
    val description =
        if (ready && title != null && prepared != null) {
            if (headerScrolled && anchorDay != null) {
                stringResource(
                    R.string.plan_x_to_x_x_x_go_to_a_day,
                    title.fromName,
                    title.toName,
                    anchorDay.title,
                    anchorDay.date,
                )
            } else {
                stringResource(R.string.plan_x_to_x_x_go_to_a_day, title.fromName, title.toName, prepared.spokenDates)
            }
        } else {
            code
        }
    val subtitle = if (headerScrolled && dayTitle != null) dayTitle else prepared?.subtitle.orEmpty()
    LaunchedEffect(expanded) {
        if (!expanded) return@LaunchedEffect
        val index = days.indexOfFirst { it.id == anchorDay?.id }.coerceAtLeast(0)
        snapshotFlow { scroll.viewportSize }.first { it > 0 }
        scroll.scrollTo(index * with(density) { TripPageMetrics.menuItemMinHeight.roundToPx() })
    }
    Box {
        Column(
            Modifier
                .minimumInteractiveComponentSize()
                .then(
                    if (ready) {
                        Modifier.clickable(role = Role.Button, onClickLabel = goTo) {
                            expanded =
                                true
                        }
                    } else {
                        Modifier
                    },
                ).semantics(mergeDescendants = true) {
                    contentDescription = description
                    dayTitle?.let {
                        stateDescription =
                            it
                    }
                }.testTag(if (ready) "trip.dayMenu" else "trip.title"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(code, style = CfTheme.type.planTitle, maxLines = 1, color = colors.foreground)
            if (ready) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PlanMetrics.smallGap.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        subtitle,
                        Modifier.weight(1f, fill = false).ellipsizable().testTag("trip.subtitle"),
                        style = CfTheme.type.planSubtitle,
                        color = colors.mutedForeground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        LucideGlyphs.ChevronDown.imageVector(),
                        null,
                        Modifier.size(PlanMetrics.sectionGap.dp),
                        tint = colors.mutedForeground,
                    )
                }
            }
        }
        CfDropdownMenu(
            expanded,
            { expanded = false },
            Modifier.width(TripPageMetrics.menuWidth).semantics {
                testTagsAsResourceId =
                    true
            },
            scrollState = scroll,
        ) {
            Text(
                goTo,
                Modifier.fillMaxWidth().padding(
                    start = PlanMetrics.gutter.dp,
                    end = PlanMetrics.gutter.dp,
                    top = PlanMetrics.rowGap.dp,
                    bottom = PlanMetrics.smallGap.dp,
                ),
                style = CfTheme.type.planListLine,
                color = colors.mutedForeground,
                textAlign = TextAlign.Center,
            )
            days.forEachIndexed { index, day ->
                val chosen = day.id == anchorDay?.id
                CfDropdownMenuItem(
                    onClick = {
                        expanded = false
                        onPickDay(index)
                    },
                    modifier =
                        Modifier
                            .heightIn(
                                min = TripPageMetrics.menuItemMinHeight,
                            ).testTag("trip.dayMenu.day.$index")
                            .semantics {
                                selected =
                                    chosen
                            },
                    contentPadding =
                        PaddingValues(
                            start = TripPageMetrics.menuItemPaddingStart,
                            end = PlanMetrics.gutter.dp,
                        ),
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(TripPageMetrics.menuItemGap),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.width(TripPageMetrics.menuCheckColumn)) {
                                if (chosen) {
                                    Icon(
                                        LucideGlyphs.Check.imageVector(strokeWidth = TripPageMetrics.MENU_CHECK_STROKE),
                                        null,
                                        Modifier
                                            .size(
                                                TripPageMetrics.menuCheckIcon,
                                            ).testTag("trip.dayMenu.check.$index"),
                                        tint = colors.foreground,
                                    )
                                }
                            }
                            Column {
                                Text(
                                    day.title,
                                    style = CfTheme.type.planTitle.copy(fontWeight = FontWeight.Normal),
                                    color = colors.foreground,
                                )
                                Text(
                                    day.date,
                                    style = CfTheme.type.planListLine.copy(fontFeatureSettings = "tnum"),
                                    color = colors.mutedForeground,
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}
