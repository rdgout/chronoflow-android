@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.trips

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.MutableCreationExtras
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.model.store.TripPlanSnapshot
import net.chronoflow.core.model.store.TripRecord
import net.chronoflow.core.plan.CopyCatalog
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.PlanClock
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.sync.SyncResult
import net.chronoflow.core.sync.SyncState
import net.chronoflow.core.sync.refresh.RefreshDebugProbe
import net.chronoflow.core.testing.FakePlanClock
import net.chronoflow.core.testing.Fixtures
import net.chronoflow.core.testing.sync.card
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.coroutines.CoroutineContext

internal class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

internal class TripsWorker(
    private val dispatcher: CoroutineDispatcher,
) : CoroutineDispatcher() {
    var running = false

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        dispatcher.dispatch(context) {
            running = true
            try {
                block.run()
            } finally {
                running = false
            }
        }
    }
}

internal class FakeTripsDependencies(
    scheduler: TestCoroutineScheduler,
) : TripsDependencies {
    override val worker = TripsWorker(StandardTestDispatcher(scheduler))
    val records = MutableStateFlow<List<TripRecord>>(emptyList())
    val syncState = MutableStateFlow(true)
    val prefs = MutableStateFlow(TripsPreferences())
    val snapshots = mutableMapOf<String, MutableStateFlow<TripPlanSnapshot>>()
    val fakeClock = FakePlanClock(tripsNow())
    override var clock: PlanClock = fakeClock
    override val decoder = PlanDecoder(worker)
    var formatterOnWorker = false
    private var copyFormatter: CopyLineFormatter? = null
    var refreshCalls = 0
    var refreshGate: CompletableDeferred<SyncResult?>? = null
    override val refreshProbe = RefreshDebugProbe.None

    override fun tripRecords() = records

    override fun synced() = syncState

    override fun preferences() = prefs

    override fun planSnapshots(tripId: String) =
        snapshots.getOrPut(tripId) {
            MutableStateFlow(TripPlanSnapshot(plan = null))
        }

    override suspend fun formatter(): CopyLineFormatter {
        formatterOnWorker = worker.running
        return copyFormatter ?: CopyLineFormatter(CopyCatalog.load(worker)).also { copyFormatter = it }
    }

    override suspend fun refreshNow(): SyncResult? {
        refreshCalls++
        return refreshGate?.await()
            ?: SyncResult(SyncState.Idle, 0)
    }

    fun installPlan(
        id: String = "t1",
        detail: TripDetail = tripsDetail(),
    ) {
        snapshots[id] = MutableStateFlow(snapshot(id, detail))
    }
}

internal fun tripsDetail(): TripDetail =
    ApiJson.response.decodeFromString(TripDetail.serializer(), Fixtures.text("trips/detail.json"))

internal fun tripsNow() =
    planDate(
        tripsDetail()
            .plan.days
            .first()
            .startUtc,
    )!!

internal fun tripRecord(id: String = "t1"): TripRecord {
    val detail = tripsDetail()
    val card = card(id).card.copy(from = detail.trip.from, to = detail.trip.to, departureUtc = detail.trip.departureUtc)
    return TripRecord(
        id,
        ApiJson.response.encodeToString(TripCardView.serializer(), card),
        currentInputHash = detail.plan.meta.inputHash,
        departureUtc = card.departureUtc,
        planStartUtc =
            detail.plan.days
                .first()
                .startUtc,
        planEndUtc =
            detail.plan.days
                .last()
                .endUtc,
    )
}

internal fun snapshot(
    id: String,
    detail: TripDetail,
) = TripPlanSnapshot(
    trip = tripRecord(id),
    plan =
        PlanVersionRecord(
            id,
            detail.plan.meta.inputHash,
            ApiJson.response.encodeToString(TripDetail.serializer(), detail).encodeToByteArray(),
            "now",
            detail.detailVersion,
        ),
)

internal class ModelFixture(
    deps: TripsDependencies,
) {
    private val store = ViewModelStore()
    val model =
        ViewModelProvider.create(
            store,
            TripsViewModel.Factory,
            MutableCreationExtras().apply {
                set(TripsDependenciesKey, deps)
            },
        )[TripsViewModel::class.java]

    init {
        model.setLanguage(Language.En)
    }

    fun close() = store.clear()
}

internal fun TripsViewModel.cards() = (state.value as TripsUiState.Content).sections.flatMap { it.trips }
