package net.chronoflow.feature.settings.bodyclock

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow

@Composable
internal fun rememberBodyClockRatingField(
    viewModel: BodyClockViewModel,
    value: String,
    loaded: Boolean,
): TextFieldState {
    val binding = remember(viewModel, loaded) { BodyClockRatingField(value) }
    val current by rememberUpdatedState(value)
    if (loaded) {
        LaunchedEffect(viewModel, binding) {
            snapshotFlow { binding.state.text.toString() }.collect { text ->
                binding.edit(text)?.let { viewModel.event(BodyClockEvent.Rating(it)) }
            }
        }
        LaunchedEffect(binding) { snapshotFlow { current }.collect(binding::receive) }
    }
    return binding.state
}

internal class BodyClockRatingField(
    initial: String,
) {
    val state = TextFieldState(initial)
    private var model = initial
    private var field = initial

    fun edit(value: String): String? {
        if (value == field) return null
        field = value
        return value
    }

    fun receive(value: String) {
        if (value == model) return
        if (state.text.toString().replace(',', '.') == model) {
            field = value
            state.setTextAndPlaceCursorAtEnd(value)
        }
        model = value
    }
}
