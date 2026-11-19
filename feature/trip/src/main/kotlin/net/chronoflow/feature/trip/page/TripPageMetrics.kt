package net.chronoflow.feature.trip.page

import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.layout.BoardMetrics

/** Trip page metrics drawn by the Plan and PlanDayMenu boards without an existing token. */
@BoardMetrics("Plan", "PlanDayMenu")
object TripPageMetrics {
    val chipHeight = 28.dp
    val chipPaddingH = 10.dp
    val moreHeight = 28.dp
    val legendIcon = 18.dp
    val endIcon = 22.dp
    val menuWidth = 250.dp
    val menuItemMinHeight = 52.dp
    val menuCheckColumn = 20.dp
    val menuCheckIcon = 18.dp
    val menuItemGap = 10.dp
    val menuItemPaddingStart = 12.dp
    val chevron = 20.dp
}
