package net.chronoflow.feature.today

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.plan.trippage.PlanPreferences
import net.chronoflow.feature.today.data.TodaySource

internal class FakeTodaySource : TodaySource {
    val synced = MutableStateFlow(true)
    val preferences = MutableStateFlow(PlanPreferences())
    val trips = MutableStateFlow(listOf(todayRecord()))
    val details = mutableMapOf("hero" to MutableStateFlow<TripDetail?>(todayDetail()))
    val collectors = mutableMapOf<String, Int>()

    data class Write(
        val tripId: String,
        val detail: TripDetail,
        val blockId: String,
        val status: CheckStatus?,
    )

    val writes = mutableListOf<Write>()
    var beforeWrite: suspend () -> Unit = {}

    override fun synced() = synced

    override fun preferences() = preferences

    override fun trips() = trips

    override fun detail(tripId: String): Flow<TripDetail?> =
        flow {
            collectors[tripId] = (collectors[tripId] ?: 0) + 1
            try {
                emitAll(details.getOrPut(tripId) { MutableStateFlow(null) })
            } finally {
                collectors[tripId] = collectors.getValue(tripId) - 1
            }
        }

    override suspend fun setCheck(
        tripId: String,
        detail: TripDetail,
        blockId: String,
        status: CheckStatus?,
    ) {
        writes += Write(tripId, detail, blockId, status)
        beforeWrite()
    }
}
