package net.chronoflow.app.perf

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.PerformanceMetricsState
import net.chronoflow.feature.trip.timeline.TimelineJankTagger

/** JankStats for the debug and perf builds: logs jank frames and tags them with the visible day. */
internal object JankStatsInstaller {
    fun install(activity: ComponentActivity) {
        JankStats.createAndTrack(activity.window) { frame ->
            if (frame.isJank) Log.w(LOG_TAG, frame.toString())
        }
    }

    fun tagger(activity: ComponentActivity) =
        TimelineJankTagger { index, mode ->
            PerformanceMetricsState.getHolderForHierarchy(activity.window.decorView).state?.putState(
                "timeline",
                "day=$index;mode=$mode",
            )
        }

    private const val LOG_TAG = "CfJank"
}
