package net.chronoflow.app.shell

import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import net.chronoflow.app.AppGraph
import net.chronoflow.app.LaunchPlatform
import net.chronoflow.app.SessionGraph
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.feature.trips.TripsDependencies
import net.chronoflow.feature.trips.TripsPreferences

internal class SessionTripsDependencies(
    private val appGraph: AppGraph,
    private val sessionGraph: SessionGraph,
) : TripsDependencies {
    override val clock = appGraph.planClock
    override val worker = appGraph.dispatchers.default
    override val decoder = PlanDecoder(worker)
    override val refreshProbe = LaunchPlatform.refreshProbe
    private val mutex = Mutex()
    private var cachedFormatter: CopyLineFormatter? = null

    override fun tripRecords() = sessionGraph.trips.values()

    override fun planSnapshots(tripId: String) = sessionGraph.trips.detailValues(tripId)

    override fun synced() = sessionGraph.accounts.values().map { it?.syncedAt != null }

    override fun preferences() =
        sessionGraph.settings
            .values()
            .map { records ->
                val raw = records.firstOrNull { it.section == "preferences" }?.value
                val value =
                    try {
                        raw?.let { ApiJson.response.parseToJsonElement(it) as? JsonObject }
                    } catch (_: SerializationException) {
                        null
                    }
                val hidden =
                    (value?.get("hiddenCategories") as? JsonArray).orEmpty().mapNotNull {
                        (it as? JsonPrimitive)?.contentOrNull?.let(BlockType::of)
                    }
                val time = (value?.get("timeFormat") as? JsonPrimitive)?.contentOrNull ?: "24"
                TripsPreferences(hidden, TimeFormat.of(time))
            }.flowOn(worker)

    override suspend fun formatter(): CopyLineFormatter =
        mutex.withLock {
            cachedFormatter ?: CopyLineFormatter(appGraph.copyCatalogs.catalog()).also { cachedFormatter = it }
        }

    override suspend fun refreshNow() = sessionGraph.sync.refreshNow()
}
