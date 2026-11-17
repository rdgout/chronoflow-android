@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.designsystem.system

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.theme.CfTheme

@Composable
fun CfSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = CfTheme.colors
    val haptics = LocalHaptics.current
    Switch(
        checked,
        onCheckedChange?.let { change ->
            { value ->
                haptics.perform(if (value) HapticEvent.ToggleOn else HapticEvent.ToggleOff)
                change(value)
            }
        },
        modifier,
        enabled = enabled,
        colors =
            SwitchDefaults.colors(
                checkedTrackColor = c.dawn,
                checkedBorderColor = c.dawn,
                checkedThumbColor = c.dawnForeground,
                checkedIconColor = c.dawn,
                uncheckedTrackColor = c.input,
                uncheckedBorderColor = c.mutedForeground,
                uncheckedThumbColor = c.mutedForeground,
            ),
    )
}

private class ToggleFeedback {
    var perform: ((Boolean) -> Unit)? = null
}

private class ToggleFeedbackNode(
    var feedback: ToggleFeedback,
) : Modifier.Node(),
    CompositionLocalConsumerModifierNode {
    private fun bind() {
        feedback.perform =
            { currentValueOf(LocalHaptics).perform(if (it) HapticEvent.ToggleOn else HapticEvent.ToggleOff) }
    }

    override fun onAttach() = bind()

    override fun onDetach() {
        feedback.perform = null
    }

    fun update(next: ToggleFeedback) {
        feedback.perform = null
        feedback = next
        if (isAttached) bind()
    }
}

private data class ToggleFeedbackElement(
    val feedback: ToggleFeedback,
) : ModifierNodeElement<ToggleFeedbackNode>() {
    override fun create() = ToggleFeedbackNode(feedback)

    override fun update(node: ToggleFeedbackNode) = node.update(feedback)

    override fun InspectorInfo.inspectableProperties() {
        name = "cfToggleable"
    }
}

fun Modifier.cfToggleable(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    enabled: Boolean = true,
): Modifier {
    val feedback = ToggleFeedback()
    return then(ToggleFeedbackElement(feedback)).toggleable(value, enabled = enabled, role = Role.Switch) {
        feedback.perform?.invoke(it)
        onValueChange(it)
    }
}

@Composable
fun CfSegmentedButtons(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val c = CfTheme.colors
    val haptics = LocalHaptics.current
    require(options.isNotEmpty() && selectedIndex in options.indices)
    SingleChoiceSegmentedButtonRow(modifier.then(if (testTag == null) Modifier else Modifier.testTag(testTag))) {
        options.forEachIndexed { i, option ->
            SegmentedButton(
                selected = i == selectedIndex,
                onClick = {
                    if (i != selectedIndex) {
                        haptics.perform(HapticEvent.Selection)
                        onSelect(i)
                    }
                },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                modifier = if (testTag == null) Modifier else Modifier.testTag("$testTag.$i"),
                colors =
                    SegmentedButtonDefaults.colors(
                        activeContainerColor = c.systemSelected,
                        activeContentColor = c.dawnText,
                        activeBorderColor = c.mutedForeground,
                        inactiveContainerColor = Color.Transparent,
                        inactiveContentColor = c.foreground,
                        inactiveBorderColor = c.mutedForeground,
                    ),
            ) { Text(option) }
        }
    }
}

@Composable
fun CfLinearProgress(
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress,
        modifier.fillMaxWidth().height(PlanUITokens.wizardProgressHeight.dp),
        color = CfTheme.colors.dawn,
        trackColor = CfTheme.colors.muted,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

@Composable
fun CfLinearProgress(modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        modifier.fillMaxWidth().height(PlanUITokens.wizardProgressHeight.dp),
        color = CfTheme.colors.dawn,
        trackColor = CfTheme.colors.muted,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
    )
}

@Composable
fun CfPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    content: @Composable BoxScope.() -> Unit,
) {
    PullToRefreshBox(isRefreshing, onRefresh, modifier, state = state, indicator = {
        PullToRefreshDefaults.Indicator(
            state = state,
            isRefreshing = isRefreshing,
            modifier = Modifier.align(Alignment.TopCenter),
            containerColor = CfTheme.colors.popover,
            color = CfTheme.colors.dawn,
        )
    }, content = content)
}
