package net.chronoflow.core.datamock

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val isoFormat =
    DateTimeFormatter
        .ofPattern(
            "uuuu-MM-dd'T'HH:mm:ss'Z'",
            java.util.Locale.ROOT,
        ).withZone(ZoneOffset.UTC)

internal fun seconds(now: Instant): Double = now.toEpochMilli() / 1000.0

internal fun iso(instant: Instant): String = isoFormat.format(instant)

internal fun instant(text: String): Instant =
    Instant.parse(
        if (text.length == 17 &&
            text.endsWith('Z')
        ) {
            text.dropLast(1) + ":00Z"
        } else {
            text
        },
    )
