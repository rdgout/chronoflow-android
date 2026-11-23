package net.chronoflow.feature.launch.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class WelcomeTimelineTests {
    private val size = LaunchSize(390.0, 844.0)
    private val target = LaunchRect(24.0, 64.0, 28.0, 28.0)
    private val opening get() = LaunchTimeline.opening(size, false)

    private fun track(
        t: LaunchTimeline,
        layer: LayerID,
        key: String,
    ) = t.tracks.single {
        it.layer == layer &&
            it.keyPath == key
    }

    private fun frame(
        t: LaunchTimeline,
        ms: Int,
    ) = LaunchFrame().also { LaunchFrameSampler(t).sampleInto(ms / 1000.0, it) }

    @Test fun everyWelcomeEndingKey() {
        val t = opening.withWelcomeExit(50.milliseconds, size, target)

        data class Row(
            val layer: LayerID,
            val key: String,
            val start: Double,
            val end: Double,
            val value: TrackValue,
            val curve: CubicBezier,
        )
        val rows =
            listOf(
                Row(LayerID.Band, KeyPath.OPACITY, 1.6, 2.3, TrackValue.Scalar(0.0), CubicBezier.easeOut),
                Row(LayerID.Sun, KeyPath.OPACITY, 1.6, 2.2, TrackValue.Scalar(0.0), CubicBezier.easeOut),
                Row(LayerID.Halo, KeyPath.OPACITY, 1.6, 2.2, TrackValue.Scalar(0.0), CubicBezier.easeOut),
                Row(LayerID.Sun, KeyPath.TRANSLATION_Y, 1.6, 2.2, TrackValue.Scalar(120.0), CubicBezier.easeOut),
                Row(LayerID.Halo, KeyPath.TRANSLATION_Y, 1.6, 2.2, TrackValue.Scalar(120.0), CubicBezier.easeOut),
                Row(LayerID.Path, KeyPath.OPACITY, 1.6, 2.0, TrackValue.Scalar(0.0), CubicBezier.linear),
                Row(LayerID.Wordmark, KeyPath.OPACITY, 1.6, 1.9, TrackValue.Scalar(0.0), CubicBezier.easeOut),
                Row(LayerID.Wordmark, KeyPath.TRANSLATION_Y, 1.6, 1.9, TrackValue.Scalar(-6.0), CubicBezier.easeOut),
                Row(
                    LayerID.Moon,
                    KeyPath.POSITION,
                    1.6,
                    2.3,
                    TrackValue.Point(LaunchPoint(38.0, 78.0)),
                    CubicBezier.easeInOut,
                ),
                Row(LayerID.Moon, KeyPath.SCALE, 1.6, 2.3, TrackValue.Scalar(28.0 / 104), CubicBezier.easeInOut),
                Row(LayerID.Container, KeyPath.OPACITY, 2.3, 2.55, TrackValue.Scalar(0.0), CubicBezier.easeOut),
            )
        for (row in rows) {
            val actual = track(t, row.layer, row.key)
            assertEquals(row.start, actual.keyTimes[actual.keyTimes.lastIndex - 1], 1e-12)
            assertEquals(row.end, actual.keyTimes.last(), 1e-12)
            assertEquals(row.value, actual.values.last())
            assertEquals(row.curve, actual.curves.last())
        }
        assertEquals(listOf(TrackValue.Scalar(1.0)), track(t, LayerID.Container, KeyPath.SCALE).values)
        assertEquals(
            t.tracks.size,
            t.tracks
                .map { it.layer to it.keyPath }
                .toSet()
                .size,
        )
    }

    @Test fun flightUsesOneStraightLineAndProgressForPositionAndScale() {
        val t = opening.withWelcomeExit(1600.milliseconds, size, target)
        for (ms in 1600..2300 step 10) {
            val p = CubicBezier.easeInOut.value((ms - 1600) / 700.0)
            val f = frame(t, ms)
            assertEquals(195 + (38 - 195) * p, f.moonX, 0.01)
            assertEquals(394 + (78 - 394) * p, f.moonY, 0.01)
            assertEquals(0.86 + (28.0 / 104 - 0.86) * p, f.moonScale, 1e-10)
        }
        assertEquals(1.0, frame(t, 2300).containerOpacity, 1e-12)
        assertEquals(1 - 0.6846431874, frame(t, 2425).containerOpacity, 1e-9)
        assertEquals(0.0, frame(t, 2550).containerOpacity, 1e-12)
    }

    @Test fun targetCanChangeBeforeFlightWithoutDamagingTheOpening() {
        val o = opening
        val t = o.withWelcomeExit(50.milliseconds, size, target)
        for (original in o.tracks) {
            val merged = track(t, original.layer, original.keyPath)
            assertEquals(original.keyTimes.toList(), merged.keyTimes.take(original.keyTimes.size))
            assertEquals(original.values, merged.values.take(original.values.size))
            assertEquals(original.curves.toList(), merged.curves.take(original.curves.size))
        }
        for (ms in 0..1600 step 10) assertEquals(o.sample(ms.milliseconds), t.sample(ms.milliseconds))
        val moved = LaunchRect(71.0, 109.0, 42.0, 42.0)
        val rebuilt = t.withWelcomeExit(50.milliseconds, size, moved)
        assertEquals(o.withWelcomeExit(50.milliseconds, size, moved).tracks, rebuilt.tracks)
        assertEquals(rebuilt.tracks, rebuilt.withWelcomeExit(50.milliseconds, size, moved).tracks)
        val f = frame(rebuilt, 2300)
        assertEquals(moved.midX, f.moonX, 1e-12)
        assertEquals(moved.midY, f.moonY, 1e-12)
        assertEquals(moved.width / 104, f.moonScale, 1e-12)
    }

    @Test fun lateResolutionShiftsTheEntireEndingAndRetainsStatus() {
        val t = opening.withStatus(2.seconds).withWelcomeExit(4.seconds, size, target)
        assertEquals(195.0, frame(t, 4000).moonX, 0.0)
        assertEquals(394.0, frame(t, 4000).moonY, 0.0)
        assertEquals(38.0, frame(t, 4700).moonX, 0.01)
        assertEquals(78.0, frame(t, 4700).moonY, 0.01)
        assertEquals(0.0, frame(t, 4950).containerOpacity, 1e-12)
        assertEquals(1.0, frame(t, 4900).statusOpacity, 0.0)
        assertEquals(
            t.tracks.size,
            t.tracks
                .map { it.layer to it.keyPath }
                .toSet()
                .size,
        )
    }

    @Test fun reduceMotionUsesOnlyTheImmediateFade() {
        val o = LaunchTimeline.opening(size, true)
        val t = o.withWelcomeExit(50.milliseconds, size, target)
        assertEquals(o.withReadyExit(50.milliseconds).tracks, t.tracks)
        assertEquals(195.0, frame(t, 350).moonX, 0.0)
        assertEquals(422.0, frame(t, 350).moonY, 0.0)
        assertEquals(1.0, frame(t, 350).moonScale, 0.0)
    }

    @Test fun welcomeSettlesAtExitPlus950() {
        val early = opening.withWelcomeExit(1600.milliseconds, size, target)
        assertEquals(2.55, LaunchFrameSampler(early).settledAfterSeconds, 1e-9)
        assertEquals(
            4.95,
            LaunchFrameSampler(opening.withWelcomeExit(4.seconds, size, target)).settledAfterSeconds,
            1e-9,
        )
        assertEquals(2.55, requireNotNull(LaunchHandOffTimeline.exitEndSeconds(early)), 1e-9)
    }

    @Test fun invalidTargetIsRejectedEvenWithReduceMotion() {
        for (rm in listOf(false, true)) {
            val o = LaunchTimeline.opening(size, rm)
            val invalid =
                listOf(
                    LaunchRect(0.0, 0.0, 0.0, 0.0),
                    target.copy(x = Double.NaN),
                    target.copy(width = Double.POSITIVE_INFINITY),
                )
            for (rect in invalid) {
                assertTrue(
                    runCatching {
                        o.withWelcomeExit(
                            50.milliseconds,
                            size,
                            rect,
                        )
                    }.exceptionOrNull() is IllegalArgumentException,
                )
            }
        }
    }
}
