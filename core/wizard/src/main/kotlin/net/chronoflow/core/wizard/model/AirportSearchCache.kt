package net.chronoflow.core.wizard.model

import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import java.time.Duration
import java.time.Instant
import java.util.Locale

class AirportSearchCache(
    private val capacity: Int = 64,
    private val ttl: Duration = Duration.ofSeconds(3600),
) {
    private class Entry(
        val airports: List<ApiAirportSuggestion>,
        val storedAt: Instant,
    )

    private val entries = LinkedHashMap<String, Entry>(capacity, 0.75f, true)

    init {
        require(capacity > 0)
        require(!ttl.isNegative && !ttl.isZero)
    }

    @Synchronized
    fun get(
        query: String,
        now: Instant,
    ): List<ApiAirportSuggestion>? {
        val key = key(query)
        val entry = entries[key] ?: return null
        if (Duration.between(entry.storedAt, now) >= ttl) {
            entries.remove(key)
            return null
        }
        return entry.airports
    }

    @Synchronized
    fun put(
        query: String,
        airports: List<ApiAirportSuggestion>,
        now: Instant,
    ) {
        entries[key(query)] = Entry(airports.toList(), now)
        if (entries.size > capacity) entries.remove(entries.keys.first())
    }

    private fun key(query: String): String = query.trim().lowercase(Locale.ROOT)
}
