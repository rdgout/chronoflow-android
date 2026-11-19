package net.chronoflow.feature.trip.timeline

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import net.chronoflow.core.plan.trippage.NowLineValue

/** The now line of one day. Only `NowLine` reads it; the page writes it once per tick, and only when it changed. */
@Stable
class DayClock internal constructor() {
    internal val value = mutableStateOf<NowLineValue?>(null)
}

/** iOS `TripPlanModel.dayClock(for:)`: one stable holder per day id. Main thread only. */
class DayClocks {
    private val clocks = HashMap<String, DayClock>()

    fun clock(dayId: String): DayClock = clocks.getOrPut(dayId) { DayClock() }

    /** Drops the holders of days that left the plan. */
    fun retain(dayIds: Set<String>) {
        clocks.keys.retainAll(dayIds)
    }

    /** Writes each holder only if its value changed; a day absent from [values] has no now line. */
    fun apply(values: Map<String, NowLineValue>) {
        for ((id, clock) in clocks) {
            val next = values[id]
            if (clock.value.value != next) clock.value.value = next
        }
        for ((id, next) in values) {
            if (id !in clocks) clock(id).value.value = next
        }
    }
}
