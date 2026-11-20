@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.designsystem.system

import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.chronoflow.core.designsystem.theme.CfTheme

@Composable
fun CfTimePicker(
    state: TimePickerState,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = cfTimePickerColors(CfTheme.colors.popover)
    if (compact) {
        TimeInput(state, modifier, colors = colors)
    } else {
        TimePicker(state, modifier, colors = colors)
    }
}
