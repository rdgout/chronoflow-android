package net.chronoflow.feature.launch.model

import com.sun.management.ThreadMXBean
import net.chronoflow.core.testing.DeviceProfiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.management.ManagementFactory

class LaunchFrameSamplerTests {
    @Test fun sampleIntoAgreesWithSampleEvery10Milliseconds() {
        val small = DeviceProfiles.small
        for (size in listOf(
            LaunchSize(390.0, 844.0),
            LaunchSize(small.widthDp.toDouble(), small.heightDp.toDouble()),
        )) {
            for (reduced in listOf(false, true)) {
                val t = LaunchTimeline.opening(size, reduced)
                val sampler = LaunchFrameSampler(t)
                val f = LaunchFrame()
                for (ms in 0..3200 step 10) {
                    val seconds = ms / 1000.0
                    sampler.sampleInto(seconds, f)
                    assertEquals(t.scalar(LayerID.Container, KeyPath.OPACITY, seconds), f.containerOpacity, 0.0)
                    assertEquals(t.scalar(LayerID.Container, KeyPath.SCALE, seconds), f.containerScale, 0.0)
                    assertEquals(t.scalar(LayerID.Sky, KeyPath.OPACITY, seconds), f.skyOpacity, 0.0)
                    assertEquals(t.scalar(LayerID.Band, KeyPath.OPACITY, seconds), f.bandOpacity, 0.0)
                    assertEquals(t.scalar(LayerID.Halo, KeyPath.OPACITY, seconds), f.haloOpacity, 0.0)
                    assertEquals(
                        t.scalar(LayerID.Halo, KeyPath.TRANSLATION_Y, seconds),
                        f.haloTranslationY,
                        0.0,
                    )
                    assertEquals(t.scalar(LayerID.Sun, KeyPath.OPACITY, seconds), f.sunOpacity, 0.0)
                    assertEquals(
                        t.scalar(LayerID.Sun, KeyPath.TRANSLATION_Y, seconds),
                        f.sunTranslationY,
                        0.0,
                    )
                    assertEquals(t.scalar(LayerID.Path, KeyPath.OPACITY, seconds), f.pathOpacity, 0.0)
                    assertEquals(t.scalar(LayerID.Path, KeyPath.STROKE_END, seconds), f.strokeEnd, 0.0)
                    assertEquals(t.scalar(LayerID.Plane, KeyPath.OPACITY, seconds), f.planeOpacity, 0.0)
                    assertEquals(t.scalar(LayerID.Moon, KeyPath.OPACITY, seconds), f.moonOpacity, 0.0)
                    assertEquals(t.scalar(LayerID.Moon, KeyPath.SCALE, seconds), f.moonScale, 0.0)
                    assertEquals(t.scalar(LayerID.Wordmark, KeyPath.OPACITY, seconds), f.wordmarkOpacity, 0.0)
                    assertEquals(
                        t.scalar(LayerID.Wordmark, KeyPath.TRANSLATION_Y, seconds),
                        f.wordmarkTranslationY,
                        0.0,
                    )
                    assertEquals(t.scalar(LayerID.Status, KeyPath.OPACITY, seconds), f.statusOpacity, 0.0)
                    val moon = t.point(LayerID.Moon, seconds)
                    assertEquals(moon.x, f.moonX, 0.0)
                    assertEquals(moon.y, f.moonY, 0.0)
                    val plane = if (reduced) LaunchPoint(0.0, 0.0) else t.point(LayerID.Plane, seconds)
                    assertEquals(plane.x, f.planeX, 0.0)
                    assertEquals(plane.y, f.planeY, 0.0)
                }
            }
        }
    }

    @Test fun firstAnimatedChangeComesWithinOneFrameAfter300Milliseconds() {
        val t = LaunchTimeline.opening(LaunchSize(390.0, 844.0), false)
        val sampler = LaunchFrameSampler(t)
        val f = LaunchFrame()
        for (i in 0..36) {
            sampler.sampleInto(i / 120.0, f)
            assertEquals(0.0, f.skyOpacity, 0.0)
            assertEquals(422.0, f.moonY, 0.0)
            assertEquals(1.0, f.moonScale, 0.0)
        }
        sampler.sampleInto(37 / 120.0, f)
        assertTrue(f.skyOpacity > 0 && f.moonY < 422 && f.moonScale < 1)
    }

    @Test fun settledAfterIs1500MillisecondsAndZeroWithReduceMotion() {
        for (reduced in listOf(false, true)) {
            val t = LaunchTimeline.opening(LaunchSize(390.0, 844.0), reduced)
            assertEquals(if (reduced) 0.0 else 1.5, LaunchFrameSampler(t).settledAfterSeconds, 0.0)
            t.replace(
                LaunchTimeline.Track(
                    LayerID.StatusDot,
                    KeyPath.OPACITY,
                    doubleArrayOf(2.0, 3.0),
                    doubleArrayOf(0.0, 1.0),
                    repeatInterval = 1.0,
                ),
            )
            assertEquals(Double.POSITIVE_INFINITY, LaunchFrameSampler(t).settledAfterSeconds, 0.0)
        }
    }

    @Test fun sampleIntoAllocatesNothing() {
        val sampler = LaunchFrameSampler(LaunchTimeline.opening(LaunchSize(390.0, 844.0), false))
        val out = LaunchFrame()
        val bean = ManagementFactory.getThreadMXBean() as ThreadMXBean
        bean.isThreadAllocatedMemoryEnabled = true
        val id = Thread.currentThread().threadId()
        sampleFrames(sampler, out, 20_000)
        val extraBytes = LongArray(5)
        // A one-time VM cost can affect one round; a per-call allocation affects every round.
        for (round in extraBytes.indices) {
            val baselineStart = bean.getThreadAllocatedBytes(id)
            val baselineEnd = bean.getThreadAllocatedBytes(id)
            val start = bean.getThreadAllocatedBytes(id)
            sampleFrames(sampler, out, 10_000)
            val end = bean.getThreadAllocatedBytes(id)
            extraBytes[round] = end - start - (baselineEnd - baselineStart)
        }
        assertTrue(
            "allocated ${extraBytes.contentToString()} bytes beyond empty baseline",
            extraBytes.min() <= 0L,
        )
        assertTrue(out.moonY < 422)
    }

    private fun sampleFrames(
        sampler: LaunchFrameSampler,
        out: LaunchFrame,
        count: Int,
    ) {
        var i = 0
        while (i < count) {
            sampler.sampleInto(i.toDouble() / count * 1.6, out)
            i++
        }
    }
}
