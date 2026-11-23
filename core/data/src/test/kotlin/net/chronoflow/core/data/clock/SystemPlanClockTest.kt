package net.chronoflow.core.data.clock

import android.app.Application
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class SystemPlanClockTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val start = Instant.parse("2026-10-13T08:00:30Z").toEpochMilli()

    private fun TestScope.clock() = SystemPlanClock(app) { start + testScheduler.currentTime }

    private fun at(text: String) = Instant.parse(text)

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun registered() =
        shadowOf(app).registeredReceivers.filter { it.intentFilter.hasAction(Intent.ACTION_TIME_TICK) }

    @Test fun systemClockRefreshesForTimeZoneNotification() =
        runTest(timeout = 60.seconds) {
            val emitted = mutableListOf<Instant>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { clock().values().collect { emitted += it } }
            runCurrent()
            val first = emitted.single()
            app.sendBroadcast(Intent(Intent.ACTION_TIMEZONE_CHANGED))
            idle()
            assertEquals(2, emitted.size)
            assertTrue(emitted[1] >= first)
            job.cancel()
        }

    @Test fun emitsOnCollectionAndAtTheNextMinuteBoundary() =
        runTest(timeout = 60.seconds) {
            val emitted = mutableListOf<Instant>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { clock().values().collect { emitted += it } }
            runCurrent()
            assertEquals(listOf(at("2026-10-13T08:00:30Z")), emitted)
            advanceTimeBy(30_015)
            runCurrent()
            assertEquals(at("2026-10-13T08:01:00.015Z"), emitted.last())
            assertEquals(2, emitted.size)
            job.cancel()
        }

    @Test fun registersOneReceiverForTheFourActionsWhileCollecting() =
        runTest(timeout = 60.seconds) {
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { clock().values().collect { } }
            runCurrent()
            val receivers = registered()
            assertEquals(1, receivers.size)
            assertEquals(
                setOf(
                    Intent.ACTION_TIME_TICK,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    Intent.ACTION_DATE_CHANGED,
                ),
                receivers
                    .single()
                    .intentFilter
                    .actionsIterator()
                    .asSequence()
                    .toSet(),
            )
            job.cancel()
            runCurrent()
            assertTrue(registered().isEmpty())
        }

    @Test fun timeZoneChangeEmitsAndKeepsTheBoundaryTimer() =
        runTest(timeout = 60.seconds) {
            val emitted = mutableListOf<Instant>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { clock().values().collect { emitted += it } }
            runCurrent()
            app.sendBroadcast(Intent(Intent.ACTION_TIMEZONE_CHANGED))
            idle()
            assertEquals(2, emitted.size)
            advanceTimeBy(30_015)
            runCurrent()
            assertEquals(3, emitted.size)
            assertEquals(at("2026-10-13T08:01:00.015Z"), emitted.last())
            job.cancel()
        }

    @Test fun timeTickInAnEmittedMinuteEmitsNothing() =
        runTest(timeout = 60.seconds) {
            val emitted = mutableListOf<Instant>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { clock().values().collect { emitted += it } }
            runCurrent()
            app.sendBroadcast(Intent(Intent.ACTION_TIME_TICK))
            idle()
            assertEquals(1, emitted.size)
            job.cancel()
        }

    @Test fun timeTickAfterTheBoundaryEmitsOnceAndMovesTheTimer() =
        runTest(timeout = 60.seconds) {
            val emitted = mutableListOf<Instant>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { clock().values().collect { emitted += it } }
            runCurrent()
            advanceTimeBy(90_005)
            runCurrent()
            assertEquals(listOf("2026-10-13T08:00:30Z", "2026-10-13T08:01:00.015Z").map(::at), emitted)
            app.sendBroadcast(Intent(Intent.ACTION_TIME_TICK))
            idle()
            assertEquals(at("2026-10-13T08:02:00.005Z"), emitted.last())
            assertEquals(3, emitted.size)
            advanceTimeBy(60_010)
            runCurrent()
            assertEquals(at("2026-10-13T08:03:00.015Z"), emitted.last())
            assertEquals(4, emitted.size)
            job.cancel()
        }
}
