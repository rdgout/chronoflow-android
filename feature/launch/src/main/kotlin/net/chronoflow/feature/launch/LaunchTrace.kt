package net.chronoflow.feature.launch

import android.os.Trace

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
