package net.chronoflow.feature.launch.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LaunchTimelineTrackTests {
    @Test fun appendKeepsTheOpeningAndWithoutExitDropsIt() {
        val t = LaunchTimeline.opening(LaunchSize(390.0, 844.0), false)
        val before = t.sample(700.milliseconds)
        t.append(LayerID.Container, KeyPath.OPACITY, 1.5, 1.9, TrackValue.Scalar(0.0), CubicBezier.easeOut)
        assertEquals(before, t.sample(700.milliseconds))
        assertEquals(0.0, t.scalar(LayerID.Container, KeyPath.OPACITY, 1.9), 0.0)
        assertEquals(1.0, t.withoutExit().scalar(LayerID.Container, KeyPath.OPACITY, 1.9), 0.0)
        val track = t.tracks.single { it.layer == LayerID.Container && it.keyPath == KeyPath.OPACITY }
        assertEquals(listOf(0.0, 1.5, 1.9), track.keyTimes.toList())
        assertThrows(IllegalArgumentException::class.java) {
            t.append(LayerID.Container, KeyPath.OPACITY, 1.0, 2.0, TrackValue.Scalar(0.0), CubicBezier.linear)
        }
    }

    @Test fun repeatAndReplicaSamplingSurviveWithoutExit() {
        val t = LaunchTimeline.opening(LaunchSize(390.0, 844.0), false)
        val track =
            LaunchTimeline.Track(
                LayerID.StatusDot,
                KeyPath.OPACITY,
                doubleArrayOf(2.0, 2.6, 3.2),
                doubleArrayOf(1.0, 0.0, 1.0),
                repeatInterval = 1.2,
                replicaCount = 3,
                replicaDelay = 0.2,
            )
        t.replace(track)
        assertEquals(track, t.withoutExit().tracks.single { it.layer == LayerID.StatusDot })
        assertEquals(0.5, track.scalarAt(3.5), 1e-12)
        val values = t.sample(2.4.seconds).getValue(LayerID.StatusDot)
        assertEquals(listOf(1.0 / 3, 2.0 / 3, 1.0).size, values.replicaOpacities.size)
        for (i in 0..2) assertEquals(track.scalarAt(2.4 - i * 0.2), values.replicaOpacities[i], 1e-12)
    }

    @Test fun constructorRejectsUnsortedTimesAndCountMismatch() {
        assertThrows(IllegalArgumentException::class.java) {
            LaunchTimeline.Track(LayerID.Moon, KeyPath.OPACITY, doubleArrayOf(1.0, 0.0), doubleArrayOf(1.0, 0.0))
        }
        assertThrows(IllegalArgumentException::class.java) {
            LaunchTimeline.Track(LayerID.Moon, KeyPath.OPACITY, doubleArrayOf(0.0, 1.0), doubleArrayOf(1.0))
        }
        assertThrows(IllegalArgumentException::class.java) {
            LaunchTimeline.Track(LayerID.Moon, KeyPath.OPACITY, doubleArrayOf(0.0, 0.0), doubleArrayOf(1.0, 0.0))
        }
    }
}
