package net.chronoflow.core.plan

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.AdaptationView
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.model.store.TripPlanSnapshot
import net.chronoflow.core.model.store.TripRecord
import java.time.Instant

class PlanDecoder(
    private val worker: CoroutineDispatcher,
    private val json: Json = ApiJson.response,
) {
    private data class Key(
        val trip: String,
        val inputHash: String,
        val version: String?,
    )

    private class Cached(
        val bytes: ByteArray,
        val detail: TripDetail,
    )

    private val mutex = Mutex()
    private val cache = HashMap<Key, Cached>()

    @Volatile
    var decodeCount: Int = 0
        private set

    suspend fun trips(
        records: List<TripRecord>,
        now: Instant,
    ): TripsDecodeResult =
        withContext(worker) {
            val values = ArrayList<TripListValue>()
            var firstError: Throwable? = null
            for (record in records) {
                if (record.deletedAt != null || record.deletedLocally) continue
                try {
                    val card = card(record)
                    values.add(
                        TripListValue(
                            card,
                            record.planStartUtc,
                            record.planEndUtc,
                            tripDisplayStatus(card, record.planStartUtc, record.planEndUtc, now),
                            record.planStartZone,
                            record.planEndDate,
                            record.preparationDays,
                        ),
                    )
                } catch (error: SerializationException) {
                    if (firstError == null) firstError = error
                } catch (error: IllegalArgumentException) {
                    if (firstError == null) firstError = error
                }
            }
            TripsDecodeResult(values, firstError)
        }

    suspend fun card(record: TripRecord): TripCardView =
        withContext(worker) {
            json.decodeFromString(TripCardView.serializer(), record.card)
        }

    suspend fun adaptation(record: TripRecord): AdaptationView? =
        withContext(worker) {
            record.adaptation?.let { json.decodeFromString(AdaptationView.serializer(), it) }
        }

    suspend fun detail(record: PlanVersionRecord): TripDetail =
        withContext(worker) {
            mutex.withLock {
                val key = Key(record.tripId, record.inputHash, record.detailVersion)
                val cached = cache[key]
                if (cached != null && cached.bytes.contentEquals(record.detail)) return@withLock cached.detail
                val detail = json.decodeFromString(TripDetail.serializer(), record.detail.decodeToString())
                decodeCount++
                cache.keys.removeAll { it.trip == record.tripId }
                cache[key] = Cached(record.detail.copyOf(), detail)
                detail
            }
        }

    suspend fun evict(tripID: String) {
        mutex.withLock { cache.keys.removeAll { it.trip == tripID } }
    }

    suspend fun detail(snapshot: TripPlanSnapshot): TripDetail? {
        val record = snapshot.plan ?: return null
        return withContext(worker) {
            val decoded = detail(record)
            decoded.copy(
                checkState = mergedCheckState(decoded.plan, record.inputHash, snapshot.checks, snapshot.pendingChecks),
            )
        }
    }
}
