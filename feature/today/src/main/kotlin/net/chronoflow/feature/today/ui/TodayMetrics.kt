package net.chronoflow.feature.today.ui

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import net.chronoflow.core.designsystem.layout.BoardMetrics

@BoardMetrics("Today", "TodayRowStates")
internal object TodayMetrics {
    val nextRowVerticalPadding = 10.dp
    val loadingLineHeight = 20.dp
    val loadingLineFraction = 0.4f
    val progressHeight = 6.dp
    val rowRadius = 16.dp
    val rowTitleLineHeight = 22.sp
    val hiddenIcon = 18.dp
    val hiddenSubtitleSize = 13.sp
    val hiddenSubtitleLineHeight = 18.sp
    val sectionTracking = 0.05.em
    val statusIcon = 12.dp
    const val MARK_STROKE = 2.5f
    val markGlyphDone = 16.dp
    val markGlyphSkipped = 14.dp
}
