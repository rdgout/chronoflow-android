package net.chronoflow.app.perf

import androidx.activity.ComponentActivity
import net.chronoflow.feature.trip.timeline.TimelineJankTagger

object PerfJank {
    fun tagger(activity: ComponentActivity): TimelineJankTagger? = JankStatsInstaller.tagger(activity)

    fun install(activity: ComponentActivity) = JankStatsInstaller.install(activity)
}
