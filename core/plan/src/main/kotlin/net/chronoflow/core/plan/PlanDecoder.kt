package net.chronoflow.core.plan

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.store.PlanVersionRecord

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

    suspend fun detail(record: PlanVersionRecord): TripDetail =
        withContext(worker) {
            mutex.withLock {
                val key = Key(record.tripId, record.inputHash, record.detailVersion)
                val cached = cache[key]
                if (cached != null && cached.bytes.contentEquals(record.detail)) return@withLock cached.detail
                val detail = json.decodeFromString(TripDetail.serializer(), record.detail.decodeToString())
                decodeCount++
                cache.keys.removeAll { it.trip == record.tripId }
                cache[key] = Cached(record.detail, detail)
                detail
            }
        }

    suspend fun evict(tripID: String) {
        mutex.withLock { cache.keys.removeAll { it.trip == tripID } }
    }
}
