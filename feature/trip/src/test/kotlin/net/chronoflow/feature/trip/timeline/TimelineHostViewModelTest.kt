package net.chronoflow.feature.trip.timeline

import androidx.compose.ui.unit.Density
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.chronoflow.core.model.contracts.CheckKeyView
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.plan.trippage.NowLineValue
import net.chronoflow.feature.trip.timeline.host.TimelineHostState
import net.chronoflow.feature.trip.timeline.host.TimelineSourceInput
import net.chronoflow.feature.trip.timeline.host.timelineAvailableWidthDp
import net.chronoflow.feature.trip.timeline.host.timelineDrawingKey
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/** The host's state holder on a fake clock and fixture inputs, without Compose. */
@OptIn(ExperimentalCoroutinesApi::class)
class TimelineHostViewModelTest {
    @Before fun setMain() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After fun resetMain() {
        Dispatchers.resetMain()
    }

    @Test fun isReadyWithEveryDayAndOpensOnTheDayContainingNow() {
        val probe = TimelineTestHost()
        val host = TimelineTestHost(clockStart = probe.dayStart(33).plusSeconds(7_200))
        assertTrue(host.viewModel.state.value is TimelineHostState.Ready)
        assertEquals(60, host.model.prepared.days.size)
        assertEquals(
            host.model.prepared.days[33]
                .id,
            host.model.openingDayId,
        )
    }

    @Test fun selectChangesTheAnchorOnlyWhenTheDayChanges() =
        runTest(timeout = 60.seconds) {
            val host = TimelineTestHost()
            val seen = mutableListOf<String?>()
            val job =
                backgroundScope.launch(
                    UnconfinedTestDispatcher(testScheduler),
                ) { host.viewModel.anchorDayId.toList(seen) }
            host.viewModel.select("a")
            host.viewModel.select("a")
            host.viewModel.select("b")
            host.viewModel.select("b")
            assertEquals(listOf(null, "a", "b"), seen)
            job.cancel()
        }

    @Test fun aMinuteTickWritesOneDayAndABoundaryWritesTwo() =
        runTest(timeout = 60.seconds) {
            val host = TimelineTestHost()
            val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { host.viewModel.runClock() }
            val ids =
                host.model.prepared.days
                    .map { it.id }
            val clocks = ids.map { host.viewModel.clocks.clock(it) }
            assertEquals(NowLineValue(0.0, "07:00"), clocks[0].value.value?.copy(text = "07:00"))
            assertNull(clocks[1].value.value)
            val before = clocks.map { it.value.value }
            host.clock.advance(host.dayStart(0).plusSeconds(60))
            val tick = clocks.map { it.value.value }
            assertEquals(setOf(0), before.indices.filter { before[it] != tick[it] }.toSet())
            host.clock.advance(host.dayStart(1))
            val boundary = clocks.map { it.value.value }
            assertEquals(setOf(0, 1), tick.indices.filter { tick[it] != boundary[it] }.toSet())
            job.cancel()
        }

    @Test fun anUnchangedClockValueKeepsItsHolderValueInstance() =
        runTest(timeout = 60.seconds) {
            val host = TimelineTestHost()
            val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { host.viewModel.runClock() }
            val clock =
                host.viewModel.clocks.clock(
                    host.model.prepared.days[0]
                        .id,
                )
            val first = clock.value.value
            host.clock.advance(host.dayStart(0))
            assertSame(first, clock.value.value)
            job.cancel()
        }

    @Test fun prefetchAroundBuildsTheTwoDaysOnEachSide() {
        val host = TimelineTestHost()
        val prepared = host.model.prepared
        val key =
            timelineDrawingKey(
                prepared.days[10].id,
                prepared.revision,
                timelineAvailableWidthDp(411.0, 0.0, 0.0),
                Density(2.625f),
                dark = false,
                compact = true,
                language = Language.En,
            )
        host.viewModel.prefetchAround(10, key)
        assertEquals(4, host.cache.buildCount)
        for (offset in listOf(-2, -1, 1, 2)) {
            val day = prepared.days[10 + offset]
            assertNotNull(host.cache.peek(key.copy(dayId = day.id), day))
        }
        assertNull(host.cache.peek(key, prepared.days[10]))
        host.viewModel.prefetchAround(0, key.copy(dayId = prepared.days[0].id))
        assertEquals(4 + 2, host.cache.buildCount)
    }

    @Test fun aCheckOffKeepsEveryOtherDayInstance() =
        runTest(timeout = 60.seconds) {
            val host = TimelineTestHost()
            val first = host.model.prepared
            val block =
                first.days
                    .flatMap { it.blocks }
                    .first { it.block.actionable && it.block.checkKey != null }
            val key: CheckKeyView = checkNotNull(block.block.checkKey)
            host.inputs.value =
                TimelineSourceInput(
                    host.bytes,
                    listOf(CheckMark(key, CheckStatus.Done, "")),
                    TimeFormat.Value24,
                    emptyList(),
                )
            val second = host.model.prepared
            val changed = first.days.indices.filter { first.days[it] !== second.days[it] }
            assertTrue("a check-off changes at least its day", changed.isNotEmpty())
            assertTrue("and no more than the days of that block", changed.size <= 2)
            assertEquals(60, second.days.size)
        }
}
