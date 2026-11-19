@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.today

import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.step.StepSheetState
import net.chronoflow.core.testing.FakePlanClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

class TodayViewModelTest {
    @Test fun cancelledRefreshStopsRefreshingWithoutFeedback() =
        check { rig ->
            rig.model.refresh { throw CancellationException("foreign") }
            assertTrue(rig.model.refreshState.value.feedback.isEmpty())
            assertFalse(rig.model.refreshState.value.refreshing)
        }

    @Test fun refreshKeepsTheExistingResultCapsulePolicy() =
        check { rig ->
            rig.model.refresh {
                net.chronoflow.core.sync
                    .SyncResult(net.chronoflow.core.sync.SyncState.Idle, 0)
            }
            assertEquals(
                net.chronoflow.core.sync.refresh.RefreshFeedbackKey.UpToDate,
                rig.model.refreshState.value.feedback
                    .single()
                    .key,
            )
        }

    @Test fun formatterFailureKeepsLoading() =
        runTest(timeout = 60.seconds) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val model =
                TodayViewModel(
                    FakeTodaySource(),
                    FakePlanClock(todayNow),
                    { throw IOException("unavailable") },
                    dispatcher,
                    dispatcher,
                )
            val store = ViewModelStore().apply { put("today", model) }
            try {
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect() }
                runCurrent()
                assertEquals(TodayUiState.Loading, model.uiState.value)
            } finally {
                store.clear()
            }
        }

    private class Rig(
        scope: TestScope,
        val source: FakeTodaySource = FakeTodaySource(),
    ) {
        private val dispatcher = StandardTestDispatcher(scope.testScheduler)
        val clock = FakePlanClock(todayNow)
        val model = TodayViewModel(source, clock, { todayFormatter(dispatcher) }, dispatcher, dispatcher)
        private val store = ViewModelStore().apply { put("today", model) }

        init {
            scope.backgroundScope.launch(UnconfinedTestDispatcher(scope.testScheduler)) { model.uiState.collect() }
            scope.backgroundScope.launch(UnconfinedTestDispatcher(scope.testScheduler)) { model.now.collect() }
            scope.backgroundScope.launch(
                UnconfinedTestDispatcher(scope.testScheduler),
            ) { model.stepSheetState.collect() }
        }

        fun close() = store.clear()
    }

    private fun check(block: suspend TestScope.(Rig) -> Unit) =
        runTest(timeout = 60.seconds) {
            val rig = Rig(this)
            try {
                block(rig)
            } finally {
                rig.close()
            }
        }

    @Test fun firstTickLeavesLoading() =
        check {
            assertEquals(TodayUiState.Loading, it.model.uiState.value)
            runCurrent()
            assertTrue(it.model.uiState.value is TodayUiState.Active)
            assertEquals(todayNow, it.model.now.value)
        }

    @Test fun quietMinuteEmitsOnlyNow() =
        check { rig ->
            runCurrent()
            rig.model.uiState.test {
                assertTrue(awaitItem() is TodayUiState.Active)
                rig.clock.advance(todayNow.plusSeconds(60))
                runCurrent()
                expectNoEvents()
                assertEquals(todayNow.plusSeconds(60), rig.model.now.value)
            }
        }

    @Test fun boundaryTickEmitsOnce() =
        check { rig ->
            runCurrent()
            rig.model.uiState.test {
                val before = awaitItem()
                rig.clock.advance(todayNow.plusSeconds(3600))
                runCurrent()
                assertNotEquals(before, awaitItem())
                expectNoEvents()
            }
        }

    @Test fun concurrentCheckIsIgnored() =
        check { rig ->
            val gate = CompletableDeferred<Unit>()
            rig.source.beforeWrite = { gate.await() }
            runCurrent()
            rig.model.openStep("row0")
            runCurrent()
            rig.model.setCheck("row0", CheckStatus.Done)
            rig.model.setCheck("row0", CheckStatus.Skipped)
            runCurrent()
            assertEquals(1, rig.source.writes.size)
            assertTrue(rig.model.changingCheck.value)
            gate.complete(Unit)
            runCurrent()
            assertFalse(rig.model.changingCheck.value)
        }

    @Test fun heroChangeKeepsSelectionDetailAndWriteTrip() =
        check { rig ->
            val next = todayTrip("next", start = todayNow.plusSeconds(60), end = todayNow.plusSeconds(86400))
            rig.source.details["next"] =
                MutableStateFlow(todayDetail("next").copy(plan = todayDetail("next").plan.copy(blocks = emptyList())))
            rig.source.trips.value = listOf(todayRecord(todayTrip(end = todayNow.plusSeconds(60))), todayRecord(next))
            runCurrent()
            rig.model.openStep("row0")
            runCurrent()
            assertEquals(1, rig.source.collectors["hero"])
            rig.clock.advance(todayNow.plusSeconds(61))
            runCurrent()
            assertEquals("next", (rig.model.uiState.value as TodayUiState.Active).hero.id)
            assertEquals(
                "hero",
                rig.model.stepSheet.value!!
                    .tripId,
            )
            assertTrue(rig.model.stepSheetState.value is StepSheetState.Step)
            rig.model.setCheck("row0", CheckStatus.Done)
            runCurrent()
            assertEquals(
                "hero",
                rig.source.writes
                    .single()
                    .tripId,
            )
            assertEquals(
                "hero",
                rig.source.writes
                    .single()
                    .detail.trip.id,
            )
        }

    @Test fun openStepWithoutHeroDoesNothing() =
        check { rig ->
            rig.source.trips.value = emptyList()
            runCurrent()
            rig.model.openStep("row0")
            assertNull(rig.model.stepSheet.value)
        }

    @Test fun languageReformatsWithoutATick() =
        check { rig ->
            runCurrent()
            val before =
                (rig.model.uiState.value as TodayUiState.Active)
                    .panel!!
                    .current
                    .single()
                    .title
            rig.model.setLanguage(Language.Nl)
            runCurrent()
            val after =
                (rig.model.uiState.value as TodayUiState.Active)
                    .panel!!
                    .current
                    .single()
                    .title
            assertNotEquals(before, after)
            assertEquals(todayNow, rig.model.now.value)
        }

    @Test fun foreignCancellationKeepsSheetAndReleasesBusy() =
        check { rig ->
            runCurrent()
            rig.model.openStep("row0")
            runCurrent()
            rig.source.beforeWrite = { throw CancellationException("foreign") }
            rig.model.setCheck("row0", CheckStatus.Done)
            runCurrent()
            assertFalse(rig.model.changingCheck.value)
            assertEquals(StepSelection("hero", "row0"), rig.model.stepSheet.value)
            rig.source.beforeWrite = {}
            rig.model.setCheck("row0", CheckStatus.Done)
            runCurrent()
            assertEquals(2, rig.source.writes.size)
        }

    @Test fun writeFailureKeepsSheetAndReleasesBusy() =
        check { rig ->
            runCurrent()
            rig.model.openStep("row0")
            runCurrent()
            rig.source.beforeWrite = { throw IOException("offline") }
            rig.model.setCheck("row0", CheckStatus.Done)
            runCurrent()
            assertFalse(rig.model.changingCheck.value)
            assertEquals(StepSelection("hero", "row0"), rig.model.stepSheet.value)
        }

    @Test fun evictedHeroDetailRemovesOnlyThePanel() =
        check { rig ->
            runCurrent()
            rig.source.details
                .getValue("hero")
                .value = null
            runCurrent()
            val state = rig.model.uiState.value as TodayUiState.Active
            assertEquals("hero", state.hero.id)
            assertNull(state.panel)
        }
}
