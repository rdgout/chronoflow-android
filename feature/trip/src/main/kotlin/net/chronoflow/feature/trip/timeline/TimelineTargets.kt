package net.chronoflow.feature.trip.timeline

import android.annotation.SuppressLint
import android.os.Trace
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.cfFocusRing
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.plan.drawing.DayDrawing
import net.chronoflow.core.plan.drawing.TimelineTarget
import net.chronoflow.core.plan.drawing.TimelineTraceNames
import net.chronoflow.core.plan.step.DayCheckActions
import net.chronoflow.feature.trip.step.checkAccessibilityActions
import net.chronoflow.feature.trip.step.rememberCheckActionLabels
import kotlin.math.roundToInt

/**
 * The invisible semantics overlay of one day: one node per target at its frame, with only semantics (no `clickable`, no
 * indication, no pointer input; sighted taps go through the day's single `pointerInput`). It reads the drawing in its
 * own scope, so it recomposes once when the drawing arrives or changes. That is why it takes the `State` itself.
 */
@Suppress("ktlint:compose:state-param-check")
@SuppressLint("UnclosedTrace")
@Composable
fun TimelineTargets(
    drawing: State<DayDrawing?>,
    dayId: String,
    onBlock: (String) -> Unit,
    modifier: Modifier = Modifier,
    actions: DayCheckActions = DayCheckActions(emptyMap()),
    onCheck: (String, CheckStatus?) -> Unit = { _, _ -> },
) {
    val labels = rememberCheckActionLabels()
    val custom =
        remember(actions, labels, onCheck) {
            actions.byBlock.mapValues { (id, value) -> checkAccessibilityActions(value, id, labels, onCheck) }
        }
    val probe = LocalTimelineProbe.current
    SideEffect { probe?.composed(TimelineProbe.Kind.TARGETS, dayId) }
    Trace.beginSection(TimelineTraceNames.TARGETS)
    // No try/finally: the Compose compiler rejects them around composable calls, and an exception aborts composition anyway.
    TargetsLayout(drawing.value, onBlock, custom, modifier)
    Trace.endSection()
}

@Composable
private fun TargetsLayout(
    value: DayDrawing?,
    onBlock: (String) -> Unit,
    custom: Map<String, List<CustomAccessibilityAction>>,
    modifier: Modifier = Modifier,
) {
    val targets = value?.targets.orEmpty()
    Layout(
        content = {
            for (target in targets) Box(Modifier.targetSemantics(target, onBlock, custom[target.blockId].orEmpty()))
        },
        modifier = modifier.testTag(if (value == null) PREPARING_TAG else READY_TAG),
    ) { measurables, constraints ->
        val placeables =
            measurables.mapIndexed { index, measurable ->
                val t = targets[index]
                val w = (t.right - t.left).roundToInt().coerceAtLeast(0)
                val h = (t.bottom - t.top).roundToInt().coerceAtLeast(0)
                measurable.measure(Constraints.fixed(w, h))
            }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { index, placeable ->
                placeable.place(targets[index].left.roundToInt(), targets[index].top.roundToInt())
            }
        }
    }
}

internal const val PREPARING_TAG = "trip.timeline.preparing"
internal const val READY_TAG = "trip.timeline.ready"

private fun Modifier.targetSemantics(
    target: TimelineTarget,
    onBlock: (String) -> Unit,
    actions: List<CustomAccessibilityAction>,
): Modifier {
    val id = target.blockId
    val focus =
        if (target.kind == TimelineTarget.Kind.BLOCK && id != null) {
            cfFocusRing(RoundedCornerShape(PlanUITokens.radiusLg.dp))
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp &&
                        (
                            event.key == Key.Enter || event.key == Key.NumPadEnter ||
                                event.key == Key.DirectionCenter || event.key == Key.Spacebar
                        )
                    ) {
                        onBlock(id)
                        true
                    } else {
                        false
                    }
                }.focusable()
        } else {
            this
        }
    return focus.semantics {
        contentDescription = target.label
        traversalIndex = target.traversalIndex
        target.testTag?.let { testTag = it }
        if (target.kind == TimelineTarget.Kind.BLOCK && id != null) {
            role = Role.Button
            onClick {
                onBlock(id)
                true
            }
            if (actions.isNotEmpty()) customActions = actions
        }
    }
}
