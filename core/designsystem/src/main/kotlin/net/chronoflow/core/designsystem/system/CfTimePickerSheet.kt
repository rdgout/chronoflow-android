@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.designsystem.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.theme.CfTheme

@Composable
fun CfTimePickerSheet(
    title: String,
    subtitle: String?,
    footnote: String?,
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: (hour: Int, minute: Int, touched: Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "time-sheet",
    confirmTag: String = "time-sheet.done",
    dismissTag: String = "time-sheet.cancel",
    state: TimePickerState = rememberTimePickerState(initialHour, initialMinute, is24Hour),
) {
    val colors = CfTheme.colors
    val scheme = MaterialTheme.colorScheme
    val pickerScheme = remember(scheme, colors.card) { scheme.copy(surface = colors.card) }
    CfModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier
                .fillMaxWidth()
                .testTag(testTag)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(
                    top = PlanUITokens.space3.dp,
                    start = PlanUITokens.space5.dp,
                    end = PlanUITokens.space5.dp,
                    bottom = PlanUITokens.space8.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(PlanUITokens.space4.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space1.dp)) {
                Text(
                    title,
                    Modifier.testTag("$testTag.title").semantics { heading() },
                    style = CfTheme.type.style(CfTypeScale.screenCardTitle),
                    color = colors.foreground,
                )
                subtitle?.let {
                    Text(
                        it,
                        Modifier.testTag("$testTag.subtitle"),
                        style = CfTheme.type.style(CfTypeScale.screenBody),
                        color = colors.mutedForeground,
                    )
                }
            }
            // M3 1.4.0 draws a default Surface over the picker container.
            MaterialTheme(colorScheme = pickerScheme) {
                TimePicker(
                    state,
                    Modifier.align(Alignment.CenterHorizontally),
                    colors = cfTimePickerColors(colors.card),
                    layoutType = TimePickerDefaults.layoutType(),
                )
            }
            footnote?.let {
                Text(
                    it,
                    Modifier.fillMaxWidth().testTag("$testTag.footnote"),
                    style = CfTheme.type.style(CfTypeScale.screenBody),
                    color = colors.mutedForeground,
                    textAlign = TextAlign.Center,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
                CfButton(
                    confirmLabel,
                    { onConfirm(state.hour, state.minute, state.hour != initialHour || state.minute != initialMinute) },
                    Modifier.testTag(confirmTag),
                    size = CfButtonSize.Lg,
                    minHeight = PlanUITokens.actionMinHeight.dp,
                    fillWidth = true,
                )
                CfButton(
                    dismissLabel,
                    onDismiss,
                    Modifier.testTag(dismissTag),
                    style = CfButtonStyle.Ghost,
                    size = CfButtonSize.Lg,
                    minHeight = PlanUITokens.actionMinHeight.dp,
                    fillWidth = true,
                )
            }
        }
    }
}
