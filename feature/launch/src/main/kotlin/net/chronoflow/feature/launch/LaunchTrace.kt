package net.chronoflow.feature.launch

import android.os.Trace

/** Async cookies: launch 1, first-commit 2, ready 3, exit 4; session 5 and intro 6 are reserved. */
object LaunchTrace : LaunchTracing by LaunchTraceSession(
    begin = { name, cookie -> Trace.beginAsyncSection(name, cookie) },
    end = { name, cookie -> Trace.endAsyncSection(name, cookie) },
) {
    inline fun <T> moonDecode(block: () -> T): T {
        Trace.beginSection("cf.launch.moonDecode")
        try {
            return block()
        } finally {
            Trace.endSection()
        }
    }
}
