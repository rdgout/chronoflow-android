package net.chronoflow.feature.launch.model

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ReadyTimelineTests {
    private val opening get() = LaunchTimeline.opening(LaunchSize(390.0, 844.0), false)

    private fun track(
        t: LaunchTimeline,
        layer: LayerID,
        key: String = KeyPath.OPACITY,
    ) = t.tracks.single { it.layer == layer && it.keyPath == key }

    private fun same(
        a: LaunchTimeline,
        b: LaunchTimeline,
    ) = assertEquals(a.tracks, b.tracks)

    private fun unique(t: LaunchTimeline) =
        assertEquals(
            t.tracks.size,
            t.tracks
                .map {
                    it.layer to it.keyPath
                }.toSet()
                .size,
        )

    @Test fun readyExitKeysAndNeverCutShort() {
        for (ready in listOf(0, 50, 1499, 1500, 4600, 8000)) {
            val t = opening.withReadyExit(ready.milliseconds)
            val start = maxOf(1.5, ready / 1000.0)
            for ((key, end) in listOf(KeyPath.OPACITY to 0.0, KeyPath.SCALE to 1.08)) {
                val actual = track(t, LayerID.Container, key)
                assertArrayEquals(doubleArrayOf(0.0, start, start + 0.4), actual.keyTimes, 0.0)
                assertEquals(listOf(1.0, 1.0, end).map { TrackValue.Scalar(it) }, actual.values)
                assertEquals(listOf(CubicBezier.linear, CubicBezier.easeOut), actual.curves.toList())
            }
            assertEquals(1 - 0.6846431874, track(t, LayerID.Container).scalarAt(start + 0.2), 1e-9)
            assertEquals(
                1 + 0.08 * 0.6846431874,
                track(t, LayerID.Container, KeyPath.SCALE).scalarAt(start + 0.2),
                1e-9,
            )
            assertEquals(0.0, track(t, LayerID.Container).scalarAt(start + 1), 0.0)
            unique(t)
        }
    }

    @Test fun mergingReadyPreservesEveryOpeningKeyAndSample() {
        val original = opening
        val timeline = original.withReadyExit(1500.milliseconds)
        for (source in original.tracks) {
            val merged = track(timeline, source.layer, source.keyPath)
            assertEquals(source.keyTimes.toList(), merged.keyTimes.take(source.keyTimes.size))
            assertEquals(source.values, merged.values.take(source.values.size))
            assertEquals(source.curves.toList(), merged.curves.take(source.curves.size))
        }
        for (ms in 0..1500 step 10) assertEquals(original.sample(ms.milliseconds), timeline.sample(ms.milliseconds))
        same(timeline, timeline.withReadyExit(1500.milliseconds))
        same(original.withReadyExit(4600.milliseconds), timeline.withReadyExit(4600.milliseconds))
    }

    @Test fun slowStatusFadeAndLatchThroughExit() {
        val t = opening.withStatus(2.seconds).withReadyExit(4600.milliseconds)
        val status = track(t, LayerID.Status)
        assertArrayEquals(doubleArrayOf(0.0, 2.0, 2.3), status.keyTimes, 0.0)
        assertEquals(listOf(0.0, 0.0, 1.0).map { TrackValue.Scalar(it) }, status.values)
        assertEquals(listOf(CubicBezier.linear, CubicBezier.easeOut), status.curves.toList())
        assertEquals(0.0, status.scalarAt(1.999), 0.0)
        assertEquals(0.6846431874, status.scalarAt(2.15), 1e-9)
        for (ms in listOf(2300, 2600, 4600, 4900, 5000, 9000)) assertEquals(1.0, status.scalarAt(ms / 1000.0), 0.0)
        same(t, opening.withReadyExit(4600.milliseconds).withStatus(2.seconds))
        same(t, t.withStatus(2.seconds))
        unique(t)
    }

    @Test fun dotsRepeatFromMountWithDelayedReplicas() {
        val original = opening
        val timeline = original.withStatus(2.seconds)
        val dots = track(timeline, LayerID.StatusDot)
        assertArrayEquals(doubleArrayOf(0.0, 0.48, 0.96, 1.2), dots.keyTimes, 0.0)
        assertEquals(listOf(0.25, 1.0, 0.25, 0.25).map { TrackValue.Scalar(it) }, dots.values)
        assertEquals(listOf(CubicBezier.easeInOut, CubicBezier.easeInOut, CubicBezier.linear), dots.curves.toList())
        assertEquals(1.2, dots.repeatInterval)
        assertEquals(3, dots.replicaCount)
        assertEquals(0.2, dots.replicaDelay, 0.0)
        for (cycle in 0..7) {
            for (replica in 0..2) {
                val delay = cycle * 1.2 + replica * 0.2
                for ((offset, expected) in listOf(
                    0.0 to 0.25,
                    0.24 to 0.625,
                    0.48 to 1.0,
                    0.72 to 0.625,
                    0.96 to 0.25,
                    1.1 to 0.25,
                )) {
                    val sample = requireNotNull(timeline.sample((delay + offset).seconds)[LayerID.StatusDot])
                    assertEquals(3, sample.replicaOpacities.size)
                    assertEquals(expected, sample.replicaOpacities[replica], 1e-9)
                }
            }
        }
        assertEquals(
            timeline.sample(2600.milliseconds)[LayerID.StatusDot],
            opening.withStatus(1.seconds).sample(2600.milliseconds)[LayerID.StatusDot],
        )
        for (source in original.tracks.filter { it.layer != LayerID.Status && it.layer != LayerID.StatusDot }) {
            assertTrue(source in timeline.tracks)
        }
        assertEquals(listOf(0.0), track(original, LayerID.Status).keyTimes.toList())
    }

    @Test fun reduceMotionFadesImmediatelyWithConstantDots() {
        val opening = LaunchTimeline.opening(LaunchSize(375.0, 667.0), true)
        val t = opening.withStatus(2.seconds).withReadyExit(50.milliseconds)
        val exit = track(t, LayerID.Container)
        assertArrayEquals(doubleArrayOf(0.0, 0.05, 0.35), exit.keyTimes, 0.0)
        assertEquals(listOf(CubicBezier.linear, CubicBezier.easeOut), exit.curves.toList())
        assertEquals(0.0, exit.scalarAt(0.35), 0.0)
        assertEquals(1.0, track(t, LayerID.Container, KeyPath.SCALE).scalarAt(4.0), 0.0)
        for (ms in listOf(0, 480, 2000, 2300, 2600, 8000)) {
            val sample = t.sample(ms.milliseconds)
            assertEquals(listOf(1.0, 1.0, 1.0), sample[LayerID.StatusDot]?.replicaOpacities)
            assertEquals(
                TrackValue.Point(LaunchPoint(187.5, 333.5)),
                sample[LayerID.Moon]?.properties?.get(KeyPath.POSITION),
            )
        }
        assertNull(track(t, LayerID.StatusDot).repeatInterval)
        assertEquals(1.0, track(t, LayerID.Status).scalarAt(2.3), 0.0)
        assertEquals(listOf(0.0, 0.3), track(opening.withReadyExit(0.seconds), LayerID.Container).keyTimes.toList())
        assertEquals(listOf(0.0, 0.3), track(opening.withStatus(0.seconds), LayerID.Status).keyTimes.toList())
        unique(t)
    }
}
