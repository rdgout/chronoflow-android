package net.chronoflow.feature.trip.timeline

import android.annotation.SuppressLint
import android.os.Trace
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Constraints
import net.chronoflow.core.plan.drawing.DayDrawing
import net.chronoflow.core.plan.drawing.TimelineTarget
import net.chronoflow.core.plan.drawing.TimelineTraceNames
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
) {
    val probe = LocalTimelineProbe.current
    SideEffect { probe?.composed(TimelineProbe.Kind.TARGETS, dayId) }
    Trace.beginSection(TimelineTraceNames.TARGETS)
    // No try/finally: the Compose compiler rejects them around composable calls, and an exception aborts composition anyway.
    TargetsLayout(drawing.value, onBlock, modifier)
    Trace.endSection()
}

@Composable
private fun TargetsLayout(
    value: DayDrawing?,
    onBlock: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val targets = value?.targets.orEmpty()
    Layout(
        content = {
            for (target in targets) Box(Modifier.targetSemantics(target, onBlock))
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
): Modifier =
    semantics {
        contentDescription = target.label
        traversalIndex = target.traversalIndex
        target.testTag?.let { testTag = it }
        if (target.kind == TimelineTarget.Kind.BLOCK) {
            role = Role.Button
            val blockId = target.blockId
            if (blockId != null) {
                onClick {
                    onBlock(blockId)
                    true
                }
            }
        }
    }
