package net.chronoflow.core.datamock

import kotlinx.serialization.Serializable
import net.chronoflow.core.model.DataFailure

object MockRateLimiter {
    @Serializable
    data class Window(
        val count: Int,
        val expiresAt: Double,
    )

    fun hit(
        key: String,
        limit: Int = 5,
        seconds: Double = 60.0,
        now: Double,
        windows: Map<String, Window>,
    ): Map<String, Window> {
        val window = windows[key]?.takeIf { it.expiresAt > now } ?: Window(0, now + seconds)
        if (window.count >= limit) throw DataFailure.Throttled(maxOf(0.0, window.expiresAt - now), 429)
        return windows + (key to window.copy(count = window.count + 1))
    }
}
