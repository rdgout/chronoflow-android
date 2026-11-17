@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package net.chronoflow.core.designsystem.haptics

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

enum class HapticEvent { Selection, ToggleOn, ToggleOff, Light, Success, Warning, Error, Medium }

fun interface Haptics {
    fun perform(event: HapticEvent)
}

object NoHaptics : Haptics {
    override fun perform(event: HapticEvent) = Unit
}

val LocalHaptics = staticCompositionLocalOf<Haptics> { error("LocalHaptics is provided by ChronoFlowTheme") }

class AndroidHaptics(
    private val feedback: HapticFeedback,
) : Haptics {
    override fun perform(event: HapticEvent) {
        feedback.performHapticFeedback(
            when (event) {
                HapticEvent.Selection -> HapticFeedbackType.SegmentTick
                HapticEvent.ToggleOn -> HapticFeedbackType.ToggleOn
                HapticEvent.ToggleOff -> HapticFeedbackType.ToggleOff
                HapticEvent.Light -> HapticFeedbackType.ContextClick
                HapticEvent.Success -> HapticFeedbackType.Confirm
                HapticEvent.Warning, HapticEvent.Error -> HapticFeedbackType.Reject
                HapticEvent.Medium -> HapticFeedbackType.LongPress
            },
        )
    }
}
