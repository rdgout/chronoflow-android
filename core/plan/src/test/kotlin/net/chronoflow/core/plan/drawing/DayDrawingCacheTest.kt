package net.chronoflow.core.plan.drawing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.PreparedDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class DayDrawingCacheTest {
    private val day: PreparedDay =
        DrawingFixtures
            .prepared("S3")
            .plan.days
            .first()
    private val key = DrawingFixtures.key(day, "rev", 411, density = 2f)

    private fun drawing(
        key: DayDrawingKey,
        bytes: Int = 1000,
    ) = DayDrawing(
        key,
        100,
        100,
        ByteArray(bytes),
        FloatArray(0),
        IntArray(0),
        IntArray(0),
        emptyArray(),
        ByteArray(0),
        ByteArray(0),
        FloatArray(0),
        emptyList(),
        HitIndex.Empty,
    )

    @Test fun anEqualKeyWithTheSameDayReturnsTheSameInstanceWithoutRebuilding() {
        val cache = DayDrawingCache()
        val first = cache.getOrBuild(key, day) { drawing(key) }
        val second = cache.getOrBuild(key.copy(), day) { drawing(key) }
        assertSame(first, second)
        assertEquals(1, cache.buildCount)
    }

    @Test fun everyKeyFieldMisses() {
        val cache = DayDrawingCache()
        cache.put(key, day, drawing(key))
        val variants =
            listOf(
                key.copy(dark = !key.dark),
                key.copy(availableWidthPx = key.availableWidthPx + 1),
                key.copy(textScale = 1.3f),
                key.copy(density = 3f),
                key.copy(compact = !key.compact),
                key.copy(language = Language.Nl),
                key.copy(revision = "other"),
            )
        for (variant in variants) assertNull(variant.toString(), cache.get(variant, day))
        assertNotNull(cache.get(key, day))
    }

    @Test fun anEqualKeyWithAnUnequalDayMisses() {
        val cache = DayDrawingCache()
        cache.put(key, day, drawing(key))
        assertNull(cache.get(key, day.copy(title = day.title + "!")))
        assertNotNull(cache.get(key, day.copy()))
    }

    @Test fun nineEntriesLeaveEight() {
        val cache = DayDrawingCache()
        for (i in 1..9) cache.put(key.copy(revision = "r$i"), day, drawing(key.copy(revision = "r$i")))
        assertEquals(8, cache.size)
        assertNull(cache.peek(key.copy(revision = "r1"), day))
        assertNotNull(cache.peek(key.copy(revision = "r9"), day))
    }

    @Test fun byteOverflowEvictsTheLeastRecentlyUsed() {
        val cache = DayDrawingCache(maxBytes = 3000)
        val keys = (1..3).map { key.copy(revision = "r$it") }
        val a = drawing(keys[0], 800)
        val unit = a.estimatedBytes
        val small = DayDrawingCache(maxBytes = unit * 2L + 10)
        small.put(keys[0], day, a)
        small.put(keys[1], day, drawing(keys[1], 800))
        small.get(keys[0], day)
        small.put(keys[2], day, drawing(keys[2], 800))
        assertNull(small.peek(keys[1], day))
        assertNotNull(small.peek(keys[0], day))
        assertTrue(small.totalBytes <= small.maxBytes)
        assertEquals(0, cache.size)
    }

    @Test fun peekDoesNotTouchTheOrder() {
        val cache = DayDrawingCache(maxEntries = 2)
        val keys = (1..3).map { key.copy(revision = "r$it") }
        cache.put(keys[0], day, drawing(keys[0]))
        cache.put(keys[1], day, drawing(keys[1]))
        cache.peek(keys[0], day)
        cache.put(keys[2], day, drawing(keys[2]))
        assertNull(cache.peek(keys[0], day))
    }

    @Test fun aSixHundredKibDrawingIsReturnedButNotCached() {
        val cache = DayDrawingCache()
        val big = cache.getOrBuild(key, day) { drawing(key, 600 * 1024) }
        assertTrue(big.estimatedBytes > cache.maxDrawingBytes)
        assertEquals(0, cache.size)
        assertEquals(0L, cache.totalBytes)
        assertNotSame(big, cache.getOrBuild(key, day) { drawing(key, 600 * 1024) })
    }

    @Test fun concurrentBuildsNeverExceedTheCaps() =
        runTest(timeout = 60.seconds) {
            val cache = DayDrawingCache(maxBytes = 20_000)
            withContext(Dispatchers.Default) {
                (0 until 4)
                    .map { worker ->
                        async {
                            repeat(200) { i ->
                                val k = key.copy(revision = "r${(i + worker) % 20}")
                                cache.getOrBuild(k, day) { drawing(k, 900) }
                                assertTrue(cache.size <= cache.maxEntries)
                                assertTrue(cache.totalBytes <= cache.maxBytes)
                            }
                        }
                    }.awaitAll()
            }
            assertTrue(cache.size <= 8)
        }
}
