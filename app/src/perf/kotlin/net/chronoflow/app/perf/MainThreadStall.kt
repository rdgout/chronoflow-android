package net.chronoflow.app.perf

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlin.time.Duration

object MainThreadStall {
    fun schedule(
        duration: Duration?,
        t0Nanos: Long,
    ) {
        if (duration == null) return
        val elapsedMs = (System.nanoTime() - t0Nanos) / 1_000_000
        Handler(Looper.getMainLooper()).postDelayed(
            { SystemClock.sleep(duration.inWholeMilliseconds) },
            (
                300 -
                    elapsedMs
            ).coerceAtLeast(0),
        )
    }
}
