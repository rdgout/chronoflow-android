package net.chronoflow.app.perf

import androidx.activity.ComponentActivity
import net.chronoflow.feature.trip.timeline.TimelineJankTagger

/** Release ships no JankStats. It is compiled into the release variant only, from `src/releaseNoPerf`. */
object PerfJank {
    @Suppress("UNUSED_PARAMETER")
    fun tagger(activity: ComponentActivity): TimelineJankTagger? = null

    @Suppress("UNUSED_PARAMETER")
    fun install(activity: ComponentActivity) = Unit
}
