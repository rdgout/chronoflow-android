package net.chronoflow.core.wizard.model

import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class WizardFlightTiming(
    val form: WizardForm,
    val airports: Map<String, ApiAirportSuggestion>,
) {
    data class Field(
        val index: Int,
        val isArrival: Boolean,
    ) {
        val position: Int get() = index * 2 + if (isArrival) 1 else 0

        companion object {
            fun departure(index: Int): Field = Field(index, false)

            fun arrival(index: Int): Field = Field(index, true)

            internal fun at(position: Int): Field = Field(position / 2, position % 2 != 0)
        }
    }

    fun minimumLocal(field: Field): LocalDateTime? {
        if (field.index !in form.legs.indices || field.position <= 0) return null
        val previous = instant(Field.at(field.position - 1)) ?: return null
        val zone = zone(field) ?: return null
        return WizardDateTime.localDate(previous, zone)
    }

    fun initialLocal(
        field: Field,
        now: Instant,
    ): LocalDateTime {
        value(field)?.let(WizardDateTime::date)?.let { return it }
        val zone = zone(field)
        if (field.index in form.legs.indices && zone != null) {
            for (position in (0 until field.position).reversed()) {
                val previous = instant(Field.at(position)) ?: continue
                WizardDateTime.localDate(previous, zone)?.let { return it }
            }
        }
        val localNow = zone?.let { WizardDateTime.localDate(now, it) } ?: LocalDateTime.ofInstant(now, ZoneOffset.UTC)
        return maxOf(localNow, minimumLocal(field) ?: localNow)
    }

    fun instant(field: Field): Instant? {
        val value = value(field) ?: return null
        val zone = zone(field) ?: return null
        return WizardDateTime.instant(value, zone)
    }

    private fun value(field: Field): String? =
        form.legs.getOrNull(field.index)?.let {
            if (field.isArrival) it.arriveLocal else it.departLocal
        }

    private fun zone(field: Field): String? =
        form.legs.getOrNull(field.index)?.let {
            airports[if (field.isArrival) it.toIata else it.fromIata]?.timezone
        }
}
