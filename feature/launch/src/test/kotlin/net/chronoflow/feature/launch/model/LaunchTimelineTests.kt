package net.chronoflow.feature.launch.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal fun LaunchTimeline.scalar(
    layer: LayerID,
    key: String,
    seconds: Double,
): Double = (sample(seconds.seconds).getValue(layer)[key] as TrackValue.Scalar).value

internal fun LaunchTimeline.point(
    layer: LayerID,
    seconds: Double,
): LaunchPoint = (sample(seconds.seconds).getValue(layer)[KeyPath.POSITION] as TrackValue.Point).point

class LaunchTimelineTests {
    private val size = LaunchSize(390.0, 844.0)

    @Test fun everyOpeningScalarKeyAndCurve() {
        val t = LaunchTimeline.opening(size, false)
        assertEquals(19, t.tracks.size)
        assertEquals(
            19,
            t.tracks
                .map { it.layer to it.keyPath }
                .distinct()
                .size,
        )

        fun track(
            layer: LayerID,
            key: String,
            times: List<Double>,
            values: List<Double>,
            curves: List<CubicBezier> =
                List(times.size - 1) { CubicBezier.linear },
        ) {
            val actual = t.tracks.single { it.layer == layer && it.keyPath == key }
            assertEquals(times, actual.keyTimes.toList())
            assertEquals(values.map { TrackValue.Scalar(it) }, actual.values)
            assertEquals(curves, actual.curves.toList())
        }
        val ease = listOf(CubicBezier.linear, CubicBezier.easeOut)
        for (layer in listOf(
            LayerID.Sky,
            LayerID.Band,
        )) {
            track(layer, KeyPath.OPACITY, listOf(0.0, 0.3, 1.1), listOf(0.0, 0.0, 1.0), ease)
        }
        for (layer in listOf(LayerID.Sun, LayerID.Halo)) {
            track(layer, KeyPath.OPACITY, listOf(0.0, 0.4, 1.3), listOf(0.0, 0.0, 1.0), ease)
            track(layer, KeyPath.TRANSLATION_Y, listOf(0.0, 0.4, 1.3), listOf(110.0, 110.0, 0.0), ease)
        }
        track(LayerID.Container, KeyPath.OPACITY, listOf(0.0), listOf(1.0))
        track(LayerID.Container, KeyPath.SCALE, listOf(0.0), listOf(1.0))
        track(LayerID.Moon, KeyPath.OPACITY, listOf(0.0), listOf(1.0))
        track(LayerID.Moon, KeyPath.SCALE, listOf(0.0, 0.3, 1.1), listOf(1.0, 1.0, 0.86), ease)
        track(LayerID.Path, KeyPath.OPACITY, listOf(0.0), listOf(1.0))
        track(LayerID.Path, KeyPath.STROKE_END, listOf(0.0, 0.5, 0.56, 1.4), listOf(0.0, 0.0, 0.03, 1.0))
        track(LayerID.Plane, KeyPath.OPACITY, listOf(0.0, 0.5, 0.56, 1.4, 1.5), listOf(0.0, 0.0, 1.0, 1.0, 0.0))
        track(LayerID.Wordmark, KeyPath.OPACITY, listOf(0.0, 1.0, 1.4), listOf(0.0, 0.0, 1.0), ease)
        track(LayerID.Wordmark, KeyPath.TRANSLATION_Y, listOf(0.0, 1.0, 1.4), listOf(8.0, 8.0, 0.0), ease)
        for (layer in listOf(LayerID.Status, LayerID.StatusDot)) track(layer, KeyPath.OPACITY, listOf(0.0), listOf(0.0))
        val moon = t.tracks.single { it.layer == LayerID.Moon && it.keyPath == KeyPath.POSITION }
        assertEquals(listOf(0.0, 0.3, 1.1), moon.keyTimes.toList())
        assertEquals(
            listOf(LaunchPoint(195.0, 422.0), LaunchPoint(195.0, 422.0), LaunchPoint(195.0, 394.0)).map {
                TrackValue.Point(it)
            },
            moon.values,
        )
        assertEquals(ease, moon.curves.toList())
        val flight = FlightPath(size)
        val plane = t.tracks.single { it.layer == LayerID.Plane && it.keyPath == KeyPath.POSITION }
        assertEquals(listOf(0.0) + flight.positionKeyTimes, plane.keyTimes.toList())
        assertEquals((listOf(flight.start) + flight.positionValues).map { TrackValue.Point(it) }, plane.values)
        assertTrue(plane.curves.all { it == CubicBezier.linear })
        t.tracks.forEach { assertNull(it.repeatInterval) }
    }

    @Test fun samplesAtAllSpecifiedOpeningTimes() {
        val t = LaunchTimeline.opening(size, false)
        val references =
            listOf(
                doubleArrayOf(0.0, 422.0, 1.0, 0.0, 0.0, 0.0, 0.0),
                doubleArrayOf(0.3, 422.0, 1.0, 0.0, 0.0, 0.0, 0.0),
                doubleArrayOf(0.7, 402.829990752, 0.9041499538, 0.6846431874, 0.0, 1.0, 0.4883490027),
                doubleArrayOf(1.1, 394.0, 0.86, 1.0, 0.3781381308, 1.0, 0.9245001910),
                doubleArrayOf(1.4, 394.0, 0.86, 1.0, 1.0, 1.0, 1.0),
                doubleArrayOf(1.5, 394.0, 0.86, 1.0, 1.0, 0.0, 1.0),
            )
        for (r in references) {
            val s = r[0]
            assertEquals(r[1], t.point(LayerID.Moon, s).y, 1e-6)
            assertEquals(r[2], t.scalar(LayerID.Moon, KeyPath.SCALE, s), 1e-6)
            for (layer in listOf(
                LayerID.Sky,
                LayerID.Band,
            )) {
                assertEquals(r[3], t.scalar(layer, KeyPath.OPACITY, s), 1e-6)
            }
            assertEquals(r[4], t.scalar(LayerID.Wordmark, KeyPath.OPACITY, s), 1e-6)
            assertEquals(8 * (1 - r[4]), t.scalar(LayerID.Wordmark, KeyPath.TRANSLATION_Y, s), 1e-6)
            assertEquals(r[5], t.scalar(LayerID.Plane, KeyPath.OPACITY, s), 1e-12)
            for (layer in listOf(LayerID.Sun, LayerID.Halo)) {
                assertEquals(r[6], t.scalar(layer, KeyPath.OPACITY, s), 1e-6)
                assertEquals(110 * (1 - r[6]), t.scalar(layer, KeyPath.TRANSLATION_Y, s), 1e-6)
            }
            assertEquals(1.0, t.scalar(LayerID.Container, KeyPath.OPACITY, s), 0.0)
            assertEquals(1.0, t.scalar(LayerID.Container, KeyPath.SCALE, s), 0.0)
            assertEquals(0.0, t.scalar(LayerID.Status, KeyPath.OPACITY, s), 0.0)
        }
        assertEquals(t.sample(0.seconds), t.sample((-10).milliseconds))
        assertEquals(t.sample(1500.milliseconds), t.sample(20.seconds))
        assertEquals(0.5, t.scalar(LayerID.Plane, KeyPath.OPACITY, 0.53), 1e-12)
        assertEquals(0.5, t.scalar(LayerID.Plane, KeyPath.OPACITY, 1.45), 1e-12)
    }

    @Test fun sampledPlaneFollowsStrokeEvery10Milliseconds() {
        for (size in listOf(size, LaunchSize(375.0, 667.0), LaunchSize(590.0, 900.0))) {
            val t = LaunchTimeline.opening(size, false)
            val flight = FlightPath(size)
            val vertices = flight.positionValues
            val lengths = DoubleArray(vertices.size)
            for (i in 1 until vertices.size) {
                lengths[i] =
                    lengths[i - 1] + hypot(vertices[i].x - vertices[i - 1].x, vertices[i].y - vertices[i - 1].y)
            }
            for (ms in 0..1500 step 10) {
                val s = ms / 1000.0
                val f = t.scalar(LayerID.Path, KeyPath.STROKE_END, s)
                assertEquals(FlightPath.fraction(s), f, 1e-12)
                val plane = t.point(LayerID.Plane, s)
                val tip = flight.pointAtFraction(f)
                assertTrue(hypot(plane.x - tip.x, plane.y - tip.y) < 0.02)
                val distance = f * lengths.last()
                val upper = lengths.indexOfFirst { it >= distance }.let { if (it < 0) lengths.lastIndex else it }
                val lower = (upper - 1).coerceAtLeast(0)
                val span = lengths[upper] - lengths[lower]
                val p = if (span == 0.0) 0.0 else (distance - lengths[lower]) / span
                val x = vertices[lower].x + (vertices[upper].x - vertices[lower].x) * p
                val y = vertices[lower].y + (vertices[upper].y - vertices[lower].y) * p
                assertTrue(hypot(plane.x - x, plane.y - y) < 0.02)
            }
        }
    }

    @Test fun reduceMotionHoldsOnlyTheLaunchFrame() {
        val t = LaunchTimeline.opening(size, true)
        assertEquals(18, t.tracks.size)
        assertTrue(t.tracks.all { it.keyTimes.contentEquals(doubleArrayOf(0.0)) })
        for (ms in listOf(0, 300, 700, 1100, 1400, 1500, 8000)) {
            assertEquals(t.sample(0.seconds), t.sample(ms.milliseconds))
            for (layer in LayerID.entries) {
                assertEquals(
                    if (layer == LayerID.Container ||
                        layer == LayerID.Moon
                    ) {
                        1.0
                    } else {
                        0.0
                    },
                    t.scalar(layer, KeyPath.OPACITY, ms / 1000.0),
                    0.0,
                )
            }
        }
        assertEquals(LaunchPoint(195.0, 422.0), t.point(LayerID.Moon, 0.0))
        for (layer in listOf(
            LayerID.Sun,
            LayerID.Halo,
        )) {
            assertEquals(0.0, t.scalar(layer, KeyPath.TRANSLATION_Y, 0.0), 0.0)
        }
    }

    @Test fun pureValuesAreUsableOnAnotherThread() =
        runTest(timeout = 60.seconds) {
            val result =
                withContext(
                    Dispatchers.Default,
                ) { LaunchTimeline.opening(LaunchSize(427.0, 911.0), false).sample(700.milliseconds) }
            assertEquals(TrackValue.Scalar(1.0), result.getValue(LayerID.Moon)[KeyPath.OPACITY])
        }
}
