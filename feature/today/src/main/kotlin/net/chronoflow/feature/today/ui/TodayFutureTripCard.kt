package net.chronoflow.feature.today.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfBadge
import net.chronoflow.core.designsystem.component.CfBadgeVariant
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.tnum
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.clock.MinuteClock
import net.chronoflow.core.ui.plan.CategorySwatch
import net.chronoflow.core.ui.plan.CategorySwatchSize
import net.chronoflow.feature.today.TodayUiState

@Composable
internal fun TodayFutureTripCard(
    state: TodayUiState.Future,
    clock: MinuteClock,
    language: Language,
    formatter: CopyLineFormatter,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = state.hero.card
    val colors = CfTheme.colors
    Column(
        modifier
            .todayCard(shadow = true)
            .cfClickable(onOpen, Role.Button, focusShape = CfTheme.shapes.x3l)
            .semantics(mergeDescendants = true) {}
            .testTag("trip.${card.id}")
            .padding(PlanUITokens.space5.dp),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.futureCardSpacing.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CfBadge(stringResource(R.string.screens_upcoming), variant = CfBadgeVariant.Outline)
            Spacer(Modifier.weight(1f))
            DaysUntilText(card.departureUtc, card.from.zone, clock, card.id)
        }
        Text(
            stringResource(R.string.screens_from_to, card.from.iata, card.to.iata),
            Modifier.semantics { heading() },
            color = colors.cardForeground,
            style = CfTheme.type.style(CfTypeScale.screenPageTitle),
        )
        val date = formatter.formatWeekdayDate(card.departureUtc, language, card.from.zone)
        val offset = formatter.formatOffset(card.offsetDifferenceMinutes.toDouble(), language)
        val meta =
            card.flightNumber?.let { stringResource(R.string.screens_date_flightnumber_offset, date, it, offset) }
                ?: stringResource(R.string.screens_date_offset, date, offset)
        Text(meta, color = colors.mutedForeground, style = CfTheme.type.screenBody.tnum())
        state.next?.let { next ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(colors.surface2, CfTheme.shapes.x2l)
                    .heightIn(min = PlanUITokens.space14.dp)
                    .padding(horizontal = PlanUITokens.space3.dp, vertical = TodayMetrics.nextRowVerticalPadding),
                horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategorySwatch(next.type, size = CategorySwatchSize.Small)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PlanUITokens.space1.dp)) {
                    PlanStartsEyebrow(state.planStartUtc, state.planStartZone, clock, card.id, language, formatter)
                    Text(
                        next.title + stringResource(R.string.screens_time, next.startText),
                        color = colors.foreground,
                        style = CfTheme.type.style(CfTypeScale.screenBodyLg).copy(fontWeight = FontWeight.Medium),
                    )
                }
                Icon(
                    LucideGlyphs.ChevronRight.imageVector(),
                    null,
                    Modifier.size(PlanUITokens.space5.dp),
                    tint = colors.mutedForeground,
                )
            }
        }
    }
}
