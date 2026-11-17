package net.chronoflow.feature.trip.timeline

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.plan.PreparedDay
import net.chronoflow.core.plan.drawing.DayDrawingCache
import net.chronoflow.core.testing.FakeTextMetrics
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds

class TimelineDrawingRepositoryTest {
    private val fixture = TimelineTestPlans.prepared("S3")
    private val executor = Executors.newSingleThreadExecutor { Thread(it, "cf-build-test") }
    private val scope = CoroutineScope(SupervisorJob())
    private val threads: MutableList<String> = Collections.synchronizedList(mutableListOf())

    @After fun tearDown() {
        scope.cancel()
        executor.shutdownNow()
    }

    private fun repository(cache: DayDrawingCache = DayDrawingCache()) =
        TimelineDrawingRepository(
            cache,
            palette = TimelineTestPlans::palette,
            metrics = {
                threads += Thread.currentThread().name.substringBefore(" @")
                FakeTextMetrics()
            },
            buildDispatcher = executor.asCoroutineDispatcher(),
            scope = scope,
            tracer = TimelineTracer.None,
        )

    private fun key(day: PreparedDay) = TimelineTestPlans.key(day, fixture.plan.revision, 411)

    private fun request(index: Int): DrawingRequest {
        val day = fixture.plan.days[index]
        return DrawingRequest(key(day), day, fixture.layouts.getValue(day.id))
    }

    @Test fun concurrentRequestsForOneKeyBuildOnce() =
        runTest(timeout = 60.seconds) {
            val cache = DayDrawingCache()
            val repository = repository(cache)
            val r = request(0)
            val drawings = (1..2).map { async { repository.drawing(r.key, r.day, r.layout) } }.awaitAll()
            assertSame(drawings[0], drawings[1])
            assertEquals(1, cache.buildCount)
        }

    @Test fun buildsRunOnTheInjectedDispatcher() =
        runTest(timeout = 60.seconds) {
            val repository = repository()
            val r = request(1)
            repository.drawing(r.key, r.day, r.layout)
            assertEquals(listOf("cf-build-test"), threads.toList())
        }

    @Test fun peekNeverBuilds() =
        runTest(timeout = 60.seconds) {
            val cache = DayDrawingCache()
            val repository = repository(cache)
            val r = request(0)
            assertNull(repository.peek(r.key, r.day))
            assertEquals(0, cache.buildCount)
            val built = repository.drawing(r.key, r.day, r.layout)
            assertSame(built, repository.peek(r.key, r.day))
        }

    @Test fun prefetchBuildsEveryRequestAndTheCacheStaysBounded() =
        runTest(timeout = 60.seconds) {
            val cache = DayDrawingCache(maxBytes = 64L * 1024 * 1024, maxEntries = 3)
            val repository = repository(cache)
            val requests = (0 until 4).map(::request)
            repository.prefetch(requests)
            // A later awaited request joins the running build, so this waits for the whole queue on the single worker.
            repository.drawing(requests.last().key, requests.last().day, requests.last().layout)
            val deadline = System.nanoTime() + 30_000_000_000L
            while (cache.buildCount < 4 && System.nanoTime() < deadline) Thread.yield()
            assertEquals(4, cache.buildCount)
            assertEquals(3, cache.size)
            assertTrue(cache.size <= cache.maxEntries)
        }

    @Test fun aDifferentDayWithAnEqualKeyRebuilds() =
        runTest(timeout = 60.seconds) {
            val cache = DayDrawingCache()
            val repository = repository(cache)
            val r = request(0)
            val first = repository.drawing(r.key, r.day, r.layout)
            val changed = r.day.copy(title = r.day.title + "!")
            val second = repository.drawing(r.key, changed, r.layout)
            assertTrue(first !== second)
            assertEquals(2, cache.buildCount)
            assertNotNull(repository.peek(r.key, changed))
            assertNull(repository.peek(r.key, r.day))
        }
}
