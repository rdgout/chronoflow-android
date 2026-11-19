package net.chronoflow.core.ui.trip

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfBadge
import net.chronoflow.core.designsystem.component.CfBadgeVariant
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.effect.softShadow
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.tnum
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.ui.clock.MinuteClock
import net.chronoflow.core.ui.plan.CategorySwatch
import net.chronoflow.core.ui.plan.CategorySwatchSize
import net.chronoflow.core.ui.trip.MetaChip

@Composable
fun TripCard(
    card: TripCardUi,
    onClick: () -> Unit,
    clock: MinuteClock,
    formatter: CopyLineFormatter,
    modifier: Modifier = Modifier,
    language: Language = Language.En,
) {
    val probe = LocalTripCardProbe.current
    androidx.compose.runtime.SideEffect { probe?.composed("card.${card.id}") }
    val colors = CfTheme.colors
    val shape = RoundedCornerShape(PlanUITokens.radius3Xl.dp)
    val completed = card.status == TripStatus.Completed
    CompositionLocalProvider(LocalContentColor provides colors.cardForeground) {
        Column(
            modifier
                .then(if (completed) Modifier else Modifier.softShadow(shape, colors.isDark))
                .graphicsLayer { alpha = if (completed) PlanUITokens.completedCardOpacity else 1f }
                .background(colors.card, shape)
                .border(
                    if (card.status == TripStatus.Active) AuthTokens.focusWidth.dp else AuthTokens.borderWidth.dp,
                    if (card.status == TripStatus.Active) colors.dawn else colors.border,
                    shape,
                ).cfClickable(onClick, Role.Button, focusShape = shape)
                .semantics(mergeDescendants = true) {}
                .testTag("trip.${card.id}")
                .padding(PlanUITokens.space5.dp),
            verticalArrangement = Arrangement.spacedBy(PlanUITokens.space4.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
                verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
            ) {
                CfBadge(
                    tripsStatusText(card.status),
                    variant =
                        when (card.status) {
                            TripStatus.Active -> CfBadgeVariant.Dawn
                            TripStatus.Upcoming -> CfBadgeVariant.Outline
                            else -> CfBadgeVariant.Secondary
                        },
                    icon = if (completed) LucideGlyphs.Check.imageVector() else null,
                )
                card.flightNumber?.let { MetaChip(it) }
                Text(
                    card.offset,
                    Modifier.weight(1f),
                    color = colors.mutedForeground,
                    style = CfTheme.type.style(CfTypeScale.screenMeta).tnum(),
                    textAlign = TextAlign.End,
                )
            }
            Airports(card.from, card.to)
            Row(
                horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    card.times,
                    Modifier.weight(1f),
                    color = colors.mutedForeground,
                    style = CfTheme.type.screenBody.tnum(),
                )
                if (card.next == null) Chevron()
            }
            card.next?.let { next ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(colors.surface2, RoundedCornerShape(PlanUITokens.radius2Xl.dp))
                        .heightIn(min = PlanUITokens.space14.dp)
                        .padding(
                            horizontal = PlanUITokens.space3.dp,
                            vertical = TripCardMetrics.NEXT_VERTICAL_PADDING.dp,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategorySwatch(next.type, size = CategorySwatchSize.Small)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PlanUITokens.space1.dp)) {
                        NextEyebrow(next.startUtc, clock, formatter, language, card.id)
                        Text(next.text, color = colors.foreground, style = CfTheme.type.style(CfTypeScale.screenBodyLg))
                    }
                    Chevron()
                }
            }
        }
    }
}

@Composable
private fun Chevron() {
    Icon(
        LucideGlyphs.ChevronRight.imageVector(),
        null,
        Modifier.size(PlanUITokens.space5.dp),
        tint = CfTheme.colors.mutedForeground,
    )
}

@Composable
private fun Airports(
    from: AirportUi,
    to: AirportUi,
) {
    val gap = PlanUITokens.space3.dp
    val plane = PlanUITokens.space4.dp
    val dashGap = PlanUITokens.space1.dp
    val preferredConnector = (PlanUITokens.space6 * 2 + PlanUITokens.space4 + PlanUITokens.space1 * 2).dp
    Layout(content = {
        Airport(from, false)
        Row(horizontalArrangement = Arrangement.spacedBy(dashGap), verticalAlignment = Alignment.CenterVertically) {
            ConnectorDash(Modifier.weight(1f))
            Icon(LucideGlyphs.Plane.imageVector(), null, Modifier.size(plane), tint = CfTheme.colors.mutedForeground)
            ConnectorDash(Modifier.weight(1f))
        }
        Airport(to, true)
    }) { measurables, constraints ->
        val available = (constraints.maxWidth - gap.roundToPx() * 2).coerceAtLeast(0)
        val sideMin =
            maxOf(
                measurables[0].minIntrinsicWidth(Constraints.Infinity),
                measurables[2].minIntrinsicWidth(Constraints.Infinity),
            )
        val minimumConnector = plane.roundToPx() + dashGap.roundToPx() * 2
        // The dashes yield before the airport labels are constrained on a narrow window.
        val connectorWidth = (available - sideMin * 2).coerceIn(minimumConnector, preferredConnector.roundToPx())
        val sideWidth = ((available - connectorWidth) / 2).coerceAtLeast(0)
        val sides = Constraints(maxWidth = sideWidth)
        val first = measurables[0].measure(sides)
        val connector = measurables[1].measure(Constraints.fixedWidth(connectorWidth))
        val last = measurables[2].measure(sides)
        val height = maxOf(first.height, connector.height, last.height)
        layout(constraints.maxWidth, height) {
            first.placeRelative(0, (height - first.height) / 2)
            connector.placeRelative(sideWidth + gap.roundToPx(), (height - connector.height) / 2)
            last.placeRelative(constraints.maxWidth - last.width, (height - last.height) / 2)
        }
    }
}

@Composable
private fun Airport(
    airport: AirportUi,
    trailing: Boolean,
) {
    Column(horizontalAlignment = if (trailing) Alignment.End else Alignment.Start) {
        Text(
            airport.iata,
            color = CfTheme.colors.cardForeground,
            style = CfTheme.type.style(CfTypeScale.screenPageTitle),
        )
        airport.city?.let {
            Text(
                it,
                color = CfTheme.colors.mutedForeground,
                style = CfTheme.type.screenBody,
                textAlign = if (trailing) TextAlign.End else TextAlign.Start,
            )
        }
    }
}

@Composable
private fun ConnectorDash(modifier: Modifier = Modifier) {
    val color = CfTheme.colors.mutedForeground
    androidx.compose.foundation.layout.Box(
        modifier.height(AuthTokens.borderWidth.dp).drawWithCache {
            val dash = PlanUITokens.space1.dp.toPx()
            val effect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))
            val width = AuthTokens.borderWidth.dp.toPx()
            onDrawBehind {
                drawLine(
                    color,
                    Offset(0f, size.height / 2),
                    Offset(size.width, size.height / 2),
                    width,
                    pathEffect = effect,
                )
            }
        },
    )
}
