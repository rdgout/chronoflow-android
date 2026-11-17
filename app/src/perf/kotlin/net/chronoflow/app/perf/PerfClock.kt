package net.chronoflow.app.perf

import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * `cf.freezeAt` as an ISO-8601 instant freezes the plan clock (OQ-1d-2). A number keeps the launch-timeline meaning of 0e
 * and anything else is ignored, so the launch timeline is unaffected.
 */
fun perfClockInstant(extra: String?): Instant? =
    try {
        extra?.let { Instant.parse(it) }
    } catch (_: DateTimeParseException) {
        null
    }
