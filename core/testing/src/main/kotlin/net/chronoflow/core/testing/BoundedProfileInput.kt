package net.chronoflow.core.testing

import java.io.Closeable
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class BoundedProfileInput(
    private val stepTimeoutMillis: Long = 10_000,
    journeyTimeoutMillis: Long = 120_000,
    private val nowNanos: () -> Long = System::nanoTime,
) : Closeable {
    private val deadline = nowNanos() + TimeUnit.MILLISECONDS.toNanos(journeyTimeoutMillis)

    // UiAutomation's synchronous Binder input call can ignore interruption.
    private val worker =
        Executors.newSingleThreadExecutor { task -> Thread(task, "profile-input").apply { isDaemon = true } }

    fun <T> step(
        label: String,
        action: () -> T,
    ): T {
        val remaining = deadline - nowNanos()
        if (remaining <= 0) throw AssertionError("profile journey deadline reached before '$label'")
        val timeout = minOf(remaining, TimeUnit.MILLISECONDS.toNanos(stepTimeoutMillis))
        val pending = worker.submit(Callable(action))
        return try {
            pending.get(timeout, TimeUnit.NANOSECONDS)
        } catch (failure: TimeoutException) {
            pending.cancel(true)
            throw AssertionError(
                "profile step '$label' exceeded its ${TimeUnit.NANOSECONDS.toMillis(timeout)} ms deadline",
                failure,
            )
        } catch (failure: ExecutionException) {
            throw failure.cause ?: failure
        } catch (interrupted: InterruptedException) {
            pending.cancel(true)
            Thread.currentThread().interrupt()
            throw interrupted
        }
    }

    override fun close() {
        worker.shutdownNow()
    }
}
