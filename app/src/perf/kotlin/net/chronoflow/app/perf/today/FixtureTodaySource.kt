package net.chronoflow.app.perf.today

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.store.TripRecord
import net.chronoflow.core.plan.checkIdentity
import net.chronoflow.core.plan.trippage.planPreferences
import net.chronoflow.feature.today.data.TodaySource
import java.time.Instant

internal class FixtureTodaySource(
    private val worker: CoroutineDispatcher,
    private val now: () -> Instant = Instant::now,
) : TodaySource {
    private val account by lazy {
        MockAppSeed
            .make(now(), walkthroughSeen = true)
            .accounts.values
            .minBy { it.snapshot.id }
    }
    private val marks = MutableStateFlow<Map<String, List<CheckMark>>>(emptyMap())

    override fun synced() = flow { emit(true) }.flowOn(worker)

    override fun preferences() = flow { emit(planPreferences(account.snapshot.settings.preferences)) }.flowOn(worker)

    override fun trips() =
        flow {
            emit(
                account.trips.map { item ->
                    TripRecord(
                        id = item.card.id,
                        card = ApiJson.response.encodeToString(TripCardView.serializer(), item.card),
                        currentInputHash = item.planInputHash,
                        departureUtc = item.card.departureUtc,
                        planStartUtc = item.planStartUtc,
                        planStartZone = item.planStartZone,
                        preparationDays = item.preparationDays,
                        planEndUtc = item.planEndUtc,
                        planEndDate = item.planEndDate,
                    )
                },
            )
        }.flowOn(worker)

    override fun detail(tripId: String) =
        marks
            .map { changes ->
                account.details[tripId]?.let { detail ->
                    detail.copy(checkState = detail.checkState.copy(marks = changes[tripId] ?: detail.checkState.marks))
                }
            }.flowOn(worker)

    override suspend fun setCheck(
        tripId: String,
        detail: TripDetail,
        blockId: String,
        status: CheckStatus?,
    ) {
        withContext(worker) {
            val block = detail.plan.blocks.firstOrNull { it.id == blockId && it.actionable } ?: return@withContext
            val key = block.checkKey ?: return@withContext
            marks.update { current ->
                val remaining =
                    (current[tripId] ?: detail.checkState.marks).filter {
                        checkIdentity(it.key) !=
                            checkIdentity(key)
                    }
                current +
                    (tripId to if (status == null) remaining else remaining + CheckMark(key, status, now().toString()))
            }
        }
    }
}
