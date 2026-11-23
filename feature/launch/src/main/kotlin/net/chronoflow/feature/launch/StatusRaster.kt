package net.chronoflow.feature.launch

import androidx.compose.ui.graphics.ImageBitmap

data class StatusRaster(
    val image: ImageBitmap,
    val textWidthDp: Double,
    val ascentDp: Double,
    val descentDp: Double,
    val lineHeightDp: Double,
)
