package net.chronoflow.feature.launch.model

import net.chronoflow.core.designsystem.layout.BoardMetrics

@BoardMetrics("SplashScreen")
object LaunchMetrics {
    const val REFERENCE_WIDTH = 390.0
    const val REFERENCE_CENTRE_Y = 422.0
    const val SUN_BOTTOM_OFFSET = 194.0
    const val BAND_BOTTOM_OFFSET = 374.0
    const val PATH_BOTTOM_OFFSET = 204.0
    const val PATH_OVERHANG = 10.0
    const val PATH_CONTROL1_X = 90.0
    const val PATH_CONTROL2_X = 300.0
    const val WORDMARK_OFFSET_Y = 60.0
    const val STATUS_OFFSET_Y = 96.0
    const val STATUS_HEIGHT = 20.0
    const val STATUS_SIZE = 14.0
    const val STATUS_GAP = 6.0
    const val STATUS_DOTS_WIDTH = 18.0
    const val HALO_RADIUS = 250f
    const val SUN_RADIUS = 90f
    const val SUN_GRADIENT_RADIUS = 127.3f
    const val SUN_SOLID_RADIUS = 70f
    const val SUN_CLEAR_RADIUS = 91.6f
    const val PATH_WIDTH = 1.5f
    const val PATH_ALPHA = 0.55f
    const val PLANE_RADIUS = 24f
    const val PLANE_DOT_RADIUS = 4f
    val bandPositions = floatArrayOf(0f, 0.40f, 0.75f, 1f)
    val bandAlphas = floatArrayOf(0f, 0.22f, 0.70f, 1f)
}
