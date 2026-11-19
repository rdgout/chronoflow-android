package net.chronoflow.core.model.store

import java.time.Instant
import java.time.ZoneOffset

object StoreTime {
    fun millis(instant: Instant): String =
        base(instant) + "." + (instant.nano / 1_000_000).toString().padStart(3, '0') + "Z"

    fun seconds(instant: Instant): String = base(instant) + "Z"

    private fun base(instant: Instant): String {
        val utc = instant.atOffset(ZoneOffset.UTC)

        fun Int.two() = toString().padStart(2, '0')
        return utc.year.toString().padStart(4, '0') + "-" + utc.monthValue.two() + "-" + utc.dayOfMonth.two() +
            "T" + utc.hour.two() + ":" + utc.minute.two() + ":" + utc.second.two()
    }
}
