package net.chronoflow.feature.launch.model

import kotlin.math.abs

data class CubicBezier(
    val x1: Double,
    val y1: Double,
    val x2: Double,
    val y2: Double,
) {
    init {
        require(x1 in 0.0..1.0 && x2 in 0.0..1.0 && y1.isFinite() && y2.isFinite())
    }

    fun value(progress: Double): Double {
        require(progress.isFinite())
        if (progress <= 0) return 0.0
        if (progress >= 1) return 1.0
        if (x1 == y1 && x2 == y2) return progress
        var lower = 0.0
        var upper = 1.0
        var parameter = progress
        repeat(8) {
            val error = coordinate(parameter, x1, x2) - progress
            if (abs(error) < 1e-13) return coordinate(parameter, y1, y2)
            if (error < 0) lower = parameter else upper = parameter
            val slope = derivative(parameter, x1, x2)
            if (abs(slope) <= 1e-10) return bisect(lower, upper, progress)
            val next = parameter - error / slope
            if (next <= lower || next >= upper) return bisect(lower, upper, progress)
            parameter = next
        }
        return bisect(lower, upper, progress)
    }

    private fun bisect(
        low: Double,
        high: Double,
        progress: Double,
    ): Double {
        var lower = low
        var upper = high
        var parameter = progress
        repeat(48) {
            parameter = (lower + upper) / 2
            val x = coordinate(parameter, x1, x2)
            if (abs(x - progress) < 1e-13) return coordinate(parameter, y1, y2)
            if (x < progress) lower = parameter else upper = parameter
        }
        return coordinate(parameter, y1, y2)
    }

    private fun coordinate(
        t: Double,
        a: Double,
        b: Double,
    ): Double {
        val inverse = 1 - t
        return 3 * inverse * inverse * t * a + 3 * inverse * t * t * b + t * t * t
    }

    private fun derivative(
        t: Double,
        a: Double,
        b: Double,
    ): Double = 3 * (1 - t) * (1 - t) * a + 6 * (1 - t) * t * (b - a) + 3 * t * t * (1 - b)

    companion object {
        val easeOut = CubicBezier(0.0, 0.0, 0.58, 1.0)
        val easeInOut = CubicBezier(0.42, 0.0, 0.58, 1.0)
        val intro = CubicBezier(0.2, 0.8, 0.2, 1.0)
        val linear = CubicBezier(0.0, 0.0, 1.0, 1.0)
    }
}
