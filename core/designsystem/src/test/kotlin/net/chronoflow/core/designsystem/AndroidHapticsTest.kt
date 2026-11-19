package net.chronoflow.core.designsystem

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import net.chronoflow.core.designsystem.haptics.AndroidHaptics
import net.chronoflow.core.designsystem.haptics.HapticEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidHapticsTest {
    @Test fun everyEventMapsToThePlatformFeedback() {
        val received = mutableListOf<HapticFeedbackType>()
        val feedback =
            object : HapticFeedback {
                override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                    received += hapticFeedbackType
                }
            }
        val haptics = AndroidHaptics(feedback)
        HapticEvent.entries.forEach(haptics::perform)
        assertEquals(
            listOf(
                HapticFeedbackType.SegmentTick,
                HapticFeedbackType.ToggleOn,
                HapticFeedbackType.ToggleOff,
                HapticFeedbackType.ContextClick,
                HapticFeedbackType.Confirm,
                HapticFeedbackType.Reject,
                HapticFeedbackType.Reject,
                HapticFeedbackType.LongPress,
            ),
            received,
        )
    }
}
