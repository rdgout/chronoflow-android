package net.chronoflow.app.perf

import android.content.Intent
import androidx.compose.runtime.Composable
import net.chronoflow.app.AppGraph

/** Release ships no fixture entry. It is compiled into the release variant only, from `src/releaseNoPerf`. */
object TripPageEntry {
    @Suppress("UNUSED_PARAMETER")
    fun content(
        intent: Intent,
        graph: AppGraph,
    ): (@Composable () -> Unit)? = null
}
