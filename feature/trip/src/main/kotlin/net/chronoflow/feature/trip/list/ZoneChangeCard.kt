package net.chronoflow.feature.trip.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.effect.skyBackground
import net.chronoflow.core.designsystem.generated.CfDarkColors
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.tnum
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.plan.trippage.ListRow
import net.chronoflow.feature.trip.timeline.LocalTimelineProbe
import net.chronoflow.feature.trip.timeline.TimelineProbe

@Composable
fun ZoneChangeCard(
    row: ListRow,
    onBlock: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CfTheme.colors
    val shape = RoundedCornerShape(PlanMetrics.cardRadius.dp)
    val probe = LocalTimelineProbe.current
    SideEffect { probe?.composed(TimelineProbe.Kind.LIST_ROW, row.key) }
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .skyBackground(colors === CfDarkColors)
            .cfClickable(onClick = { onBlock(row.blockId) }, focusShape = shape)
            .testTag("trip.zone.${row.blockId}")
            .clearAndSetSemantics {
                contentDescription = row.label
                role = Role.Button
                onClick {
                    onBlock(row.blockId)
                    true
                }
            }.padding(PlanMetrics.zonePadding.dp),
        horizontalArrangement = Arrangement.spacedBy(PlanMetrics.sectionGap.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.size(PlanMetrics.touch.dp).background(colors.dawn, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                LucideGlyphs.MapPin.imageVector(),
                null,
                Modifier.size(PlanMetrics.zonePadding.dp),
                tint = colors.dawnForeground,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PlanMetrics.rowGap.dp)) {
            Text(row.title, style = CfTheme.type.planCardTitle, color = colors.skyTitle)
            row.zoneInstruction?.let { ZonePlate(it, CfTheme.type.authHint, "trip.zone.${row.blockId}.instruction") }
            row.zoneComparison?.let {
                ZonePlate(
                    it,
                    CfTheme.type.planSubtitle.tnum(),
                    "trip.zone.${row.blockId}.comparison",
                )
            }
        }
    }
}

@Composable
private fun ZonePlate(
    text: String,
    style: TextStyle,
    tag: String,
) {
    Text(
        text,
        Modifier
            .background(CfTheme.colors.skyCopyBackground, RoundedCornerShape(PlanMetrics.textPlateRadius.dp))
            .padding(horizontal = PlanMetrics.rowGap.dp, vertical = PlanMetrics.smallGap.dp)
            .testTag(tag),
        style = style,
        color = CfTheme.colors.skyCopy,
    )
}
