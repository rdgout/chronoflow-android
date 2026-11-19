package net.chronoflow.core.testing.junit

import java.io.File
import java.io.IOException
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

internal const val HANG_EXIT_CODE = 86

internal fun hangCapMillis(
    tier: String,
    seconds: String?,
): Long? {
    if (tier == "timing") return null
    val cap = seconds?.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 && it <= Long.MAX_VALUE / 1_000.0 } ?: 60.0
    return (cap * 1_000).toLong().coerceAtLeast(1)
}

internal class TestHangWatchdog(
    private val capMillis: Long,
    private val directory: File,
) : AutoCloseable {
    private val executor =
        ScheduledThreadPoolExecutor(1) { task ->
            Thread(task, "cf-test-hang-watchdog").apply { isDaemon = true }
        }.apply { removeOnCancelPolicy = true }
    private val active = mutableMapOf<String, ScheduledFuture<*>>()

    @Synchronized fun started(uniqueId: String) {
        active.remove(uniqueId)?.cancel(false)
        active[uniqueId] = executor.schedule({ expired(uniqueId) }, capMillis, TimeUnit.MILLISECONDS)
    }

    @Synchronized fun finished(uniqueId: String) {
        active.remove(uniqueId)?.cancel(false)
    }

    private fun expired(uniqueId: String) {
        synchronized(this) {
            if (active.remove(uniqueId) == null) return
        }
        try {
            val evidence =
                buildString {
                    appendLine("cf test hang: $uniqueId exceeded ${capMillis}ms")
                    Thread.getAllStackTraces().entries.sortedBy { it.key.id }.forEach { (thread, stack) ->
                        appendLine()
                        appendLine("Thread ${thread.id} ${thread.name} ${thread.state}")
                        stack.forEach { appendLine("    at $it") }
                    }
                }
            val name = uniqueId.substringAfterLast('/').replace(Regex("[^a-zA-Z0-9_.-]"), "_").take(80)
            val file = File(directory, "hang-$name-${Integer.toHexString(uniqueId.hashCode())}.txt")
            try {
                directory.mkdirs()
                file.writeText(evidence)
            } catch (_: IOException) {
                System.err.println("cf test hang: could not write hang evidence file")
            } catch (_: SecurityException) {
                System.err.println("cf test hang: permission denied writing hang evidence file")
            }
            System.err.print(evidence)
            System.err.flush()
        } finally {
            // Coroutine cancellation and interrupting Robolectric's main looper cannot stop a busy test reliably.
            Runtime.getRuntime().halt(HANG_EXIT_CODE)
        }
    }

    @Synchronized override fun close() {
        active.values.forEach { it.cancel(false) }
        active.clear()
        executor.shutdownNow()
    }
}
