package net.chronoflow.feature.launch.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

class FlightPathTests {
    @Test fun boardGeometryAndArcLength() {
        val p = FlightPath(LaunchSize(390.0, 844.0))
        assertEquals(505.8, p.length, 0.2)
        assertEquals(LaunchPoint(-10.0, 640.0), p.start)
        assertEquals(LaunchPoint(90.0, 470.0), p.control1)
        assertEquals(LaunchPoint(300.0, 470.0), p.control2)
        assertEquals(LaunchPoint(400.0, 640.0), p.end)
        assertEquals(195.0, p.pointAtFraction(0.5).x, 1e-8)
        assertEquals(512.5, p.pointAtFraction(0.5).y, 1e-8)
        assertEquals(p.start, p.pointAtFraction(-1.0))
        assertEquals(p.end, p.pointAtFraction(2.0))
    }

    @Test fun boardCompositionAndWidthScaling() {
        for (size in listOf(LaunchSize(375.0, 667.0), LaunchSize(402.0, 874.0), LaunchSize(590.0, 900.0))) {
            val p = FlightPath(size)
            assertEquals(size.height / 2 + 218, p.start.y, 0.0)
            assertEquals(size.height / 2 + 48, p.control1.y, 0.0)
            assertEquals(size.height / 2 + 48, p.control2.y, 0.0)
            assertEquals(size.width + 10, p.end.x, 0.0)
            assertEquals(90 * size.width / 390, p.control1.x, 0.0)
            assertEquals(300 * size.width / 390, p.control2.x, 0.0)
            assertEquals(size.height / 2 + 90.5, p.pointAtFraction(0.5).y, 1e-8)
        }
    }

    @Test fun fractionCurveAnd128PositionKeys() {
        val samples =
            listOf(
                0.0 to 0.0,
                0.5 to 0.0,
                0.56 to 0.03,
                0.98 to 0.515,
                1.4 to 1.0,
                9.0 to 1.0,
            )
        for ((seconds, fraction) in samples) assertEquals(fraction, FlightPath.fraction(seconds), 1e-12)
        val p = FlightPath(LaunchSize(390.0, 844.0))
        assertEquals(128, p.positionValues.size)
        assertEquals(128, p.positionKeyTimes.size)
        assertEquals(0.5, p.positionKeyTimes.first(), 0.0)
        assertEquals(1.4, p.positionKeyTimes.last(), 0.0)
        assertTrue(p.positionKeyTimes.contains(0.56))
        assertTrue(p.positionKeyTimes.zipWithNext().all { (a, b) -> a < b })
        assertEquals(p.positionValues, p.vertices)
        for ((time, point) in p.positionKeyTimes.zip(p.positionValues)) {
            val expected = p.pointAtFraction(FlightPath.fraction(time))
            assertTrue(hypot(point.x - expected.x, point.y - expected.y) < 1e-8)
        }
    }

    @Test fun lookupAgreesWithIndependentDenseIntegrationEvery10Milliseconds() {
        for (size in listOf(LaunchSize(390.0, 844.0), LaunchSize(375.0, 667.0), LaunchSize(590.0, 900.0))) {
            val p = FlightPath(size)
            val points = ArrayList<LaunchPoint>()
            val distances = DoubleArray(32769)
            points.add(p.start)
            for (i in 1..32768) {
                val t = i / 32768.0
                val u = 1 - t
                val point =
                    LaunchPoint(
                        u * u * u * p.start.x + 3 * u * u * t * p.control1.x +
                            3 * u * t * t * p.control2.x + t * t * t * p.end.x,
                        u * u * u * p.start.y + 3 * u * u * t * p.control1.y +
                            3 * u * t * t * p.control2.y + t * t * t * p.end.y,
                    )
                distances[i] = distances[i - 1] + hypot(point.x - points.last().x, point.y - points.last().y)
                points.add(point)
            }
            assertEquals(distances.last(), p.length, 0.001)
            for (ms in 500..1400 step 10) {
                val f = FlightPath.fraction(ms / 1000.0)
                val distance = f * distances.last()
                val index = distances.indices.minBy { abs(distances[it] - distance) }
                val point = p.pointAtFraction(f)
                assertTrue(hypot(point.x - points[index].x, point.y - points[index].y) < 0.04)
            }
        }
    }
}
