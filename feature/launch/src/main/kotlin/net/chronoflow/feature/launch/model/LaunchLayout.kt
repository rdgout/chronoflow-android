package net.chronoflow.feature.launch.model

class LaunchLayout(
    val size: LaunchSize,
    safeAreaInsets: MoonTarget.Insets = MoonTarget.Insets(0.0, 0.0, 0.0, 0.0),
) {
    init {
        require(size.width > 0 && size.height > 0 && size.width.isFinite() && size.height.isFinite())
    }

    private val cy = size.height / 2
    val bounds = LaunchRect(0.0, 0.0, size.width, size.height)
    val safeBounds =
        LaunchRect(
            safeAreaInsets.left,
            safeAreaInsets.top,
            (size.width - safeAreaInsets.left - safeAreaInsets.right).coerceAtLeast(0.0),
            (size.height - safeAreaInsets.top - safeAreaInsets.bottom).coerceAtLeast(0.0),
        )
    val moonCentre = LaunchPoint(size.width / 2, cy)
    val sunCentre = LaunchPoint(size.width / 2, boardY(LaunchMetrics.SUN_BOTTOM_OFFSET))
    val band =
        LaunchRect(
            0.0,
            boardY(LaunchMetrics.BAND_BOTTOM_OFFSET),
            size.width,
            size.height - boardY(LaunchMetrics.BAND_BOTTOM_OFFSET),
        )
    val wordmarkCentre = LaunchPoint(size.width / 2, cy + LaunchMetrics.WORDMARK_OFFSET_Y)
    val status = LaunchRect(0.0, cy + LaunchMetrics.STATUS_OFFSET_Y, size.width, LaunchMetrics.STATUS_HEIGHT)
    val pathStart = LaunchPoint(-LaunchMetrics.PATH_OVERHANG, boardY(LaunchMetrics.PATH_BOTTOM_OFFSET))
    val pathControl1 =
        LaunchPoint(LaunchMetrics.PATH_CONTROL1_X * size.width / LaunchMetrics.REFERENCE_WIDTH, band.minY)
    val pathControl2 =
        LaunchPoint(LaunchMetrics.PATH_CONTROL2_X * size.width / LaunchMetrics.REFERENCE_WIDTH, band.minY)
    val pathEnd = LaunchPoint(size.width + LaunchMetrics.PATH_OVERHANG, pathStart.y)

    private fun boardY(bottomOffset: Double): Double = cy + (LaunchMetrics.REFERENCE_CENTRE_Y - bottomOffset)
}
