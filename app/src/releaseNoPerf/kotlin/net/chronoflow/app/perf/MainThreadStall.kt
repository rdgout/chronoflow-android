package net.chronoflow.app.perf

import kotlin.time.Duration

object MainThreadStall {
    @Suppress("UNUSED_PARAMETER")
    fun schedule(
        duration: Duration?,
        t0Nanos: Long,
    ) = Unit
}
