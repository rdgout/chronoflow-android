package net.chronoflow.feature.launch.model

import kotlin.math.ceil

class StatusLineLayout(
    width: Double,
    textWidth: Double,
    ascent: Double,
    descent: Double,
    lineHeight: Double,
    scale: Double,
) {
    private val rounded = ceil(textWidth * scale) / scale
    private val left = (width - rounded - LaunchMetrics.STATUS_GAP - LaunchMetrics.STATUS_DOTS_WIDTH) / 2
    val text = LaunchRect(left, (lineHeight - ascent - descent) / 2, rounded, lineHeight)
    val dots = LaunchRect(left + rounded + LaunchMetrics.STATUS_GAP, 0.0, LaunchMetrics.STATUS_DOTS_WIDTH, lineHeight)
    val ink: LaunchRect get() = text.union(dots)
}
