@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.shell

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.MutableCreationExtras
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.session.SessionSnapshot
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.core.model.store.TripRecord
import net.chronoflow.core.plan.CopyCatalog
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.trippage.PlanPreferences
import net.chronoflow.core.sync.session.MemorySessionStore
import net.chronoflow.core.sync.session.RecordingAccountStore
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionTeardown
import net.chronoflow.core.sync.session.sessionResult
import net.chronoflow.core.testing.FakePlanClock
import net.chronoflow.core.testing.sync.card
import net.chronoflow.core.testing.sync.detail
import net.chronoflow.feature.today.TodayDependencies
import net.chronoflow.feature.today.TodayDependenciesKey
import net.chronoflow.feature.today.TodayViewModel
import net.chronoflow.feature.today.data.TodaySource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class TodaySessionModelTest {
    @Test fun aNewSessionEpochReplacesTheModelAndUsesItsCurrentMutationPermit() =
        runTest(timeout = 60.seconds) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val controller =
                SessionController(
                    MemorySessionStore(),
                    RecordingAccountStore(),
                    SessionTeardown(sleep = {}),
                    dispatcher,
                )
            val store = ViewModelStore()
            val formatter = CopyLineFormatter(CopyCatalog.load(dispatcher))
            val now = Instant.parse("2026-11-10T21:00:00Z")

            fun model(
                snapshot: SessionSnapshot,
                source: EpochSource,
            ): TodayViewModel {
                val dependencies =
                    TodayDependencies(
                        source,
                        FakePlanClock(now),
                        { formatter },
                        dispatcher,
                        dispatcher,
                    )
                val extras = MutableCreationExtras().apply { set(TodayDependenciesKey, dependencies) }
                val provider = ViewModelProvider.create(store, TodayViewModel.Factory, extras)
                val key = todayViewModelKey(snapshot.epoch)
                return if (key == null) {
                    provider[TodayViewModel::class.java]
                } else {
                    provider[key, TodayViewModel::class.java]
                }
            }
            try {
                controller.accept(sessionResult())
                val firstSnapshot = controller.activeSession()!!
                val firstSource = EpochSource(controller, firstSnapshot)
                val first = model(firstSnapshot, firstSource)
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { first.uiState.collect() }
                runCurrent()
                first.openStep("sleep")
                runCurrent()
                first.setCheck("sleep", CheckStatus.Done)
                runCurrent()
                assertEquals(1, firstSource.writes)
                assertSame(first, model(firstSnapshot, firstSource))

                controller.accept(sessionResult())
                val secondSnapshot = controller.activeSession()!!
                assertNull(controller.mutationPermit(firstSnapshot))
                val secondSource = EpochSource(controller, secondSnapshot)
                val second = model(secondSnapshot, secondSource)
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { second.uiState.collect() }
                runCurrent()
                second.openStep("sleep")
                runCurrent()
                second.setCheck("sleep", CheckStatus.Done)
                runCurrent()
                assertEquals(1, secondSource.writes)
                assertNotSame(first, second)
                assertSame(second, model(secondSnapshot, secondSource))
                assertEquals(1, firstSource.writes)
            } finally {
                store.clear()
                controller.close()
            }
        }
}

private class EpochSource(
    private val controller: SessionController,
    private val snapshot: SessionSnapshot,
) : TodaySource {
    var writes = 0
    private val item = card()
    private val record =
        TripRecord(
            item.card.id,
            ApiJson.response.encodeToString(TripCardView.serializer(), item.card),
            departureUtc = item.card.departureUtc,
            planStartUtc = item.planStartUtc,
            planEndUtc = item.planEndUtc,
        )

    override fun synced() = flowOf(true)

    override fun preferences() = flowOf(PlanPreferences())

    override fun trips() = flowOf(listOf(record))

    override fun detail(tripId: String) = flowOf(detail())

    override suspend fun setCheck(
        tripId: String,
        detail: TripDetail,
        blockId: String,
        status: CheckStatus?,
    ) {
        controller.mutationPermit(snapshot) ?: throw StaleSessionException()
        writes++
    }
}
