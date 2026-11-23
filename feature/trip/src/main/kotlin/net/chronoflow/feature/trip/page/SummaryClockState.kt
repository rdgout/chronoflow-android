package net.chronoflow.feature.trip.page

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.chronoflow.core.plan.trippage.DualClockValue

@Stable
class SummaryClockState {
    var values by mutableStateOf<List<DualClockValue>>(emptyList())
        private set

    fun apply(values: List<DualClockValue>) {
        this.values = values
    }
}
