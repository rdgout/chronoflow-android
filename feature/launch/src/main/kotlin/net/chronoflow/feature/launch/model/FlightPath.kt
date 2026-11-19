package net.chronoflow.feature.launch.model

import kotlin.math.hypot
import kotlin.time.Duration
import kotlin.time.DurationUnit

class FlightPath(
    val size: LaunchSize,
) {
    private val layout = LaunchLayout(size)
    val start = layout.pathStart
    val control1 = layout.pathControl1
    val control2 = layout.pathControl2
    val end = layout.pathEnd
    private val distances = DoubleArray(1024)
    val length: Double
    val positionFractions = ((0..126).map { it / 126.0 } + 0.03).sorted()
    val positionKeyTimes =
        positionFractions.map { f ->
            if (f <= 0.03) 0.5 + f / 0.03 * 0.06 else 0.56 + (f - 0.03) / 0.97 * 0.84
        }
    val positionValues: List<LaunchPoint>
    val vertices: List<LaunchPoint> get() = positionValues

    init {
        var previous = start
        for (i in 1..1023) {
            val point = point(i / 1023.0)
            distances[i] = distances[i - 1] + hypot(point.x - previous.x, point.y - previous.y)
            previous = point
        }
        length = distances.last()
        positionValues = positionFractions.map(::pointAtFraction)
    }

    fun pointAtFraction(fraction: Double): LaunchPoint {
        require(fraction.isFinite())
        val distance = fraction.coerceIn(0.0, 1.0) * length
        var lower = 0
        var upper = distances.lastIndex
        while (upper - lower > 1) {
            val middle = (lower + upper) / 2
            if (distances[middle] < distance) lower = middle else upper = middle
        }
        val progress = (distance - distances[lower]) / (distances[upper] - distances[lower])
        return point((lower + progress) / distances.lastIndex)
    }

    private fun point(t: Double): LaunchPoint {
        val a = (1 - t) * (1 - t) * (1 - t)
        val b = 3 * (1 - t) * (1 - t) * t
        val c = 3 * (1 - t) * t * t
        val d = t * t * t
        return LaunchPoint(
            a * start.x + b * control1.x + c * control2.x + d * end.x,
            a * start.y + b * control1.y + c * control2.y + d * end.y,
        )
    }

    companion object {
        fun fraction(at: Duration): Double = fraction(at.toDouble(DurationUnit.SECONDS))

        fun fraction(seconds: Double): Double =
            when {
                seconds <= 0.5 -> 0.0
                seconds < 0.56 -> (seconds - 0.5) / 0.06 * 0.03
                seconds < 1.4 -> 0.03 + (seconds - 0.56) / 0.84 * 0.97
                else -> 1.0
            }
    }
}
