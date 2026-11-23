@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.trips

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.plan.checkIdentity
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.plan.withoutOptionalSteps
import net.chronoflow.core.sync.SyncResult
import net.chronoflow.core.sync.SyncState
import net.chronoflow.core.sync.refresh.RefreshFeedbackKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TripsViewModelTest {
    @get:Rule internal val main = MainDispatcherRule()

    private fun TestScope.collect(model: TripsViewModel) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            model.state.collect {}
        }

    @Test fun loadingUntilSynced() =
        runTest(timeout = 60.seconds) {
            val deps =
                FakeTripsDependencies(testScheduler).apply {
                    syncState.value = false
                    records.value =
                        listOf(tripRecord())
                }
            val fixture = ModelFixture(deps)
            try {
                collect(fixture.model)
                runCurrent()
                assertEquals(TripsUiState.Loading, fixture.model.state.value)
                deps.syncState.value = true
                runCurrent()
                assertTrue(fixture.model.state.value is TripsUiState.Content)
            } finally {
                fixture.close()
            }
        }

    @Test fun statusMovesWithTheClock() =
        runTest(timeout = 60.seconds) {
            val deps = FakeTripsDependencies(testScheduler)
            var reads = 0
            val record = tripRecord().copy(planStartUtc = tripsNow().plusSeconds(60).toString())
            deps.records.value =
                object : AbstractList<net.chronoflow.core.model.store.TripRecord>() {
                    override val size: Int get() = 1

                    override fun get(index: Int): net.chronoflow.core.model.store.TripRecord {
                        reads++
                        return record
                    }
                }
            val fixture = ModelFixture(deps)
            try {
                collect(fixture.model)
                runCurrent()
                assertEquals(
                    TripStatus.Upcoming,
                    fixture.model
                        .cards()
                        .single()
                        .status,
                )
                val decodedReads = reads
                deps.fakeClock.advance(tripsNow().plusSeconds(60))
                runCurrent()
                assertEquals(
                    TripStatus.Active,
                    fixture.model
                        .cards()
                        .single()
                        .status,
                )
                assertEquals(decodedReads, reads)
            } finally {
                fixture.close()
            }
        }

    @Test fun nextOnlyForActiveTrips() =
        runTest(timeout = 60.seconds) {
            val deps =
                FakeTripsDependencies(testScheduler).apply {
                    records.value =
                        listOf(
                            tripRecord("active"),
                            tripRecord("upcoming").copy(planStartUtc = tripsNow().plusSeconds(60).toString()),
                        )
                    installPlan("active")
                    installPlan("upcoming")
                }
            val fixture = ModelFixture(deps)
            try {
                collect(fixture.model)
                runCurrent()
                assertNotNull(
                    fixture.model
                        .cards()
                        .single { it.id == "active" }
                        .next,
                )
                assertNull(
                    fixture.model
                        .cards()
                        .single { it.id == "upcoming" }
                        .next,
                )
                assertFalse(
                    deps.snapshots
                        .getValue("upcoming")
                        .subscriptionCount.value > 0,
                )
            } finally {
                fixture.close()
            }
        }

    @Test fun stillActiveDetailSurvivesAnActiveIdsChange() =
        runTest(timeout = 60.seconds) {
            val deps =
                FakeTripsDependencies(testScheduler).apply {
                    records.value = listOf(tripRecord("kept"))
                    installPlan("kept")
                }
            val fixture = ModelFixture(deps)
            try {
                val emissions = mutableListOf<TripsUiState>()
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    fixture.model.state.collect { emissions += it }
                }
                runCurrent()
                val next =
                    fixture.model
                        .cards()
                        .single()
                        .next
                assertNotNull(next)
                val before = emissions.size
                deps.installPlan("added")
                deps.snapshotGates["added"] = CompletableDeferred()
                deps.records.value = listOf(tripRecord("kept"), tripRecord("added"))
                runCurrent()
                assertEquals(2, fixture.model.cards().size)
                val changed = emissions.drop(before).filterIsInstance<TripsUiState.Content>()
                assertTrue(changed.isNotEmpty())
                changed.forEach { state ->
                    assertEquals(
                        next,
                        state.sections
                            .flatMap { it.trips }
                            .single { it.id == "kept" }
                            .next,
                    )
                }
                deps.snapshotGates.getValue("added").complete(Unit)
                runCurrent()
                assertEquals(
                    next,
                    fixture.model
                        .cards()
                        .single { it.id == "kept" }
                        .next,
                )
                assertNotNull(
                    fixture.model
                        .cards()
                        .single { it.id == "added" }
                        .next,
                )
            } finally {
                fixture.close()
            }
        }

    @Test fun nextLabelNowAndIn() =
        runTest(timeout = 60.seconds) {
            val deps = FakeTripsDependencies(testScheduler)
            val original = tripsDetail()
            val block = original.plan.blocks.first { it.actionable && it.checkKey != null }
            val now = tripsNow()
            val currentBlock = block.copy(startUtc = now.toString(), endUtc = now.plusSeconds(4000).toString())
            val current = original.copy(plan = original.plan.copy(blocks = listOf(currentBlock)))
            deps.records.value = listOf(tripRecord())
            deps.installPlan(detail = current)
            val fixture = ModelFixture(deps)
            try {
                collect(fixture.model)
                runCurrent()
                assertEquals(
                    now,
                    fixture.model
                        .cards()
                        .single()
                        .next
                        ?.startUtc,
                )
                val futureBlock = currentBlock.copy(startUtc = now.plusSeconds(1750).toString())
                val future = current.copy(plan = current.plan.copy(blocks = listOf(futureBlock)))
                deps.snapshots.getValue("t1").value = snapshot("t1", future)
                runCurrent()
                assertEquals(
                    now.plusSeconds(1750),
                    fixture.model
                        .cards()
                        .single()
                        .next
                        ?.startUtc,
                )
            } finally {
                fixture.close()
            }
        }

    @Test fun checkedStepIsSkipped() =
        runTest(timeout = 60.seconds) {
            val deps = FakeTripsDependencies(testScheduler)
            val original = tripsDetail()
            val blocks =
                withoutOptionalSteps(original.plan)
                    .blocks
                    .filter { it.actionable && it.checkKey != null && planDate(it.endUtc)!! > tripsNow() }
                    .distinctBy { checkIdentity(it.checkKey!!) }
                    .take(2)
            assertEquals(2, blocks.size)
            val detail = original.copy(plan = original.plan.copy(blocks = blocks))
            deps.records.value = listOf(tripRecord())
            deps.installPlan(detail = detail)
            val fixture = ModelFixture(deps)
            try {
                collect(fixture.model)
                runCurrent()
                val before =
                    fixture.model
                        .cards()
                        .single()
                        .next
                val key = blocks.first().checkKey!!
                val pending =
                    CheckOffChange(
                        "t1",
                        detail.plan.meta.inputHash,
                        key.type,
                        key.localDate,
                        key.ordinal,
                        CheckStatus.Done,
                    )
                deps.snapshots.getValue("t1").value = snapshot("t1", detail).copy(pendingChecks = listOf(pending))
                runCurrent()
                val after =
                    fixture.model
                        .cards()
                        .single()
                        .next
                assertNotNull(before)
                assertNotNull(after)
                assertNotEquals(before, after)
                assertEquals(blocks[1].type, after?.type)
            } finally {
                fixture.close()
            }
        }

    @Test fun minuteTickWithoutChangesDoesNotEmit() =
        runTest(timeout = 60.seconds) {
            val deps =
                FakeTripsDependencies(testScheduler).apply {
                    records.value =
                        listOf(
                            tripRecord("completed").copy(planEndUtc = tripsNow().minusSeconds(60).toString()),
                            tripRecord("upcoming").copy(planStartUtc = tripsNow().plusSeconds(3600).toString()),
                        )
                }
            val fixture = ModelFixture(deps)
            try {
                val emissions = mutableListOf<TripsUiState>()
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    fixture.model.state.collect {
                        emissions +=
                            it
                    }
                }
                runCurrent()
                val count = emissions.size
                deps.fakeClock.advance(tripsNow().plusSeconds(60))
                runCurrent()
                assertEquals(count, emissions.size)
            } finally {
                fixture.close()
            }
        }

    @Test fun languageChangeReformats() =
        runTest(timeout = 60.seconds) {
            val deps = FakeTripsDependencies(testScheduler).apply { records.value = listOf(tripRecord()) }
            val fixture = ModelFixture(deps)
            try {
                collect(fixture.model)
                runCurrent()
                val english = fixture.model.cards().single()
                fixture.model.setLanguage(Language.Nl)
                runCurrent()
                assertNotEquals(
                    english.times,
                    fixture.model
                        .cards()
                        .single()
                        .times,
                )
            } finally {
                fixture.close()
            }
        }

    @Test fun refreshDelegatesAndReportsFeedback() =
        runTest(timeout = 60.seconds) {
            val deps = FakeTripsDependencies(testScheduler).apply { refreshGate = CompletableDeferred() }
            val fixture = ModelFixture(deps)
            try {
                fixture.model.refresh()
                fixture.model.refresh()
                runCurrent()
                assertEquals(1, deps.refreshCalls)
                assertTrue(fixture.model.refreshState.value.refreshing)
                deps.refreshGate!!.complete(SyncResult(SyncState.Idle, 0))
                runCurrent()
                assertFalse(fixture.model.refreshState.value.refreshing)
                assertEquals(
                    RefreshFeedbackKey.UpToDate,
                    fixture.model.refreshState.value.feedback
                        .single()
                        .key,
                )
            } finally {
                fixture.close()
            }
        }

    @Test fun formatterIsLoadedOffMain() =
        runTest(timeout = 60.seconds) {
            val deps = FakeTripsDependencies(testScheduler)
            val fixture = ModelFixture(deps)
            try {
                collect(fixture.model)
                runCurrent()
                assertTrue(deps.formatterOnWorker)
            } finally {
                fixture.close()
            }
        }
}
