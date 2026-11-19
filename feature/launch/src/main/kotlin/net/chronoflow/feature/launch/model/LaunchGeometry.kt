package net.chronoflow.feature.launch.model

import kotlin.math.max
import kotlin.math.min

data class LaunchPoint(
    val x: Double,
    val y: Double,
)

data class LaunchSize(
    val width: Double,
    val height: Double,
)

data class LaunchRect(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
) {
    val minX: Double get() = x
    val minY: Double get() = y
    val maxX: Double get() = x + width
    val maxY: Double get() = y + height

    fun union(other: LaunchRect): LaunchRect {
        val left = min(minX, other.minX)
        val top = min(minY, other.minY)
        return LaunchRect(left, top, max(maxX, other.maxX) - left, max(maxY, other.maxY) - top)
    }

    fun contains(
        other: LaunchRect,
        tolerance: Double = 0.0,
    ): Boolean =
        other.minX >= minX - tolerance && other.minY >= minY - tolerance &&
            other.maxX <= maxX + tolerance && other.maxY <= maxY + tolerance

    fun offset(
        dx: Double,
        dy: Double,
    ): LaunchRect = copy(x = x + dx, y = y + dy)
}
