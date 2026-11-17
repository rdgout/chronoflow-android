package net.chronoflow.debug

import android.os.Process
import android.os.SystemClock
import android.util.Log
import java.util.Locale

object UITestTimeline {
    fun record(
        event: String,
        seed: TestSeed,
        details: String = "",
        allowUngated: Boolean = false,
    ) {
        if (seed.testId == null && !allowUngated) return
        val wall = String.format(Locale.ROOT, "%.3f", System.currentTimeMillis() / 1000.0)
        val uptime = String.format(Locale.ROOT, "%.3f", SystemClock.uptimeMillis() / 1000.0)
        val id = seed.testId ?: "ungated"
        Log.i(
            "UITestTimeline",
            "CF_UI_TIMELINE id=$id pid=${Process.myPid()} event=$event wall=$wall uptime=$uptime $details".trimEnd(),
        )
    }
}
