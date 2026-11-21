package net.chronoflow.feature.settings.security

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

interface ClipboardPort {
    fun set(
        label: String,
        text: String,
        sensitive: Boolean,
    ): Long

    fun currentTimestamp(): Long?

    fun clear()
}

class SensitiveClipboard(
    private val port: ClipboardPort,
    private val scope: CoroutineScope,
    private val label: String,
) {
    private var timer: Job? = null
    private var pending: Long? = null

    fun copy(text: String) {
        val stamp = port.set(label, text, sensitive = true)
        pending = null
        timer?.cancel()
        timer =
            scope.launch {
                delay(60.seconds)
                clearIfOurs(stamp)
            }
    }

    fun foreground() {
        pending?.let(::clearIfOurs)
    }

    private fun clearIfOurs(stamp: Long) {
        when (port.currentTimestamp()) {
            stamp -> {
                port.clear()
                pending = null
            }

            null -> {
                pending = stamp
            }

            else -> {
                pending = null
            }
        }
    }

    override fun toString(): String = "SensitiveClipboard"
}
