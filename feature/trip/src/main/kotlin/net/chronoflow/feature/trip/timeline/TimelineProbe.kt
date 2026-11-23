@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package net.chronoflow.feature.trip.timeline

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Test-only observation of the timeline's invalidation (iOS `TimelineInvalidationTests` keeps its probes in the test
 * target). Production holds only this interface and a null local; the counting implementation lives in `src/test`.
 */
interface TimelineProbe {
    enum class Kind { STATIC, CLOCK, TARGETS, HEADER, ANCHOR, SUMMARY_CLOCK, PILLS, COMPACT, LIST_ROW }

    fun composed(
        kind: Kind,
        dayId: String,
    )

    fun drew(
        kind: Kind,
        dayId: String,
    )
}

val LocalTimelineProbe = staticCompositionLocalOf<TimelineProbe?> { null }
