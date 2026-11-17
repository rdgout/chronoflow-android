package net.chronoflow.feature.launch.model

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

interface LaunchClock {
    val now: Duration

    suspend fun sleepUntil(deadline: Duration)
}

class MediaLaunchClock(
    val t0Nanos: Long,
    private val nanoTime: () -> Long = System::nanoTime,
) : LaunchClock {
    override val now: Duration get() = (nanoTime() - t0Nanos).coerceAtLeast(0).nanoseconds

    override suspend fun sleepUntil(deadline: Duration) {
        val remaining = deadline - now
        if (remaining > Duration.ZERO) delay(remaining)
        currentCoroutineContext().ensureActive()
    }
}
