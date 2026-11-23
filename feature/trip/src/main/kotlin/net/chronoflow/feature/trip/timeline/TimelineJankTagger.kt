@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package net.chronoflow.feature.trip.timeline

import androidx.compose.runtime.staticCompositionLocalOf

/** Tells the debug and perf builds' JankStats which day is on screen. Release provides no tagger. */
fun interface TimelineJankTagger {
    fun onVisibleDay(
        index: Int,
        mode: String,
    )
}

val LocalTimelineJankTagger = staticCompositionLocalOf<TimelineJankTagger?> { null }
