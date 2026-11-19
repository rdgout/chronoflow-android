package net.chronoflow.core.testing.compose

import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.Haptics

class RecordingHaptics : Haptics {
    private val recorded = mutableListOf<HapticEvent>()
    val events: List<HapticEvent> get() = recorded.toList()

    override fun perform(event: HapticEvent) {
        recorded += event
    }
}
