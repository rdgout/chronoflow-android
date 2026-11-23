package net.chronoflow.feature.trip.timeline

import android.os.Trace
import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.plan.DayLayout
import net.chronoflow.core.plan.PreparedDay
import net.chronoflow.core.plan.TimelineGeometry
import net.chronoflow.core.plan.drawing.DayDrawingKey
import net.chronoflow.core.plan.drawing.TimelineTraceNames
import net.chronoflow.core.plan.generated.TimelinePalette
import net.chronoflow.core.ui.R

/**
 * Port of `DayTimelineView`. The drawing is built off the main thread behind a placeholder, then replayed from
 * `drawWithCache` in its own graphics layer. This scope reads no clock and no `drawing` value in composition: the clock
 * belongs to [NowLine], the drawing to [TimelineTargets] and to the draw and pointer lambdas.
 */
@Composable
fun DayTimeline(
    day: PreparedDay,
    layout: DayLayout,
    key: DayDrawingKey,
    dayIndex: Int,
    availableWidthDp: Double,
    repository: TimelineDrawingRepository,
    resources: TimelineResources,
    clock: DayClock,
    onBlock: (String) -> Unit,
    onFirstReplay: () -> Unit,
    prefetch: (dayIndex: Int, key: DayDrawingKey) -> Unit,
    modifier: Modifier = Modifier,
    actions: net.chronoflow.core.plan.step.DayCheckActions = day.checkActions,
    onCheck: (String, net.chronoflow.core.model.contracts.CheckStatus?) -> Unit = { _, _ -> },
    headings: @Composable () -> Unit = {},
) {
    val probe = LocalTimelineProbe.current
    SideEffect { probe?.composed(TimelineProbe.Kind.STATIC, day.id) }

    // Kept across key changes: the old drawing stays on screen until the new one replaces it (no placeholder flash).
    val drawing = remember(day.id) { mutableStateOf(repository.peek(key, day)) }
    val currentPrefetch by rememberUpdatedState(prefetch)
    LaunchedEffect(key, day) {
        recoverDrawingFailure({ Log.w(LOG_TAG, "drawing failed for ${day.id}", it) }) {
            val built = repository.drawing(key, day, layout)
            drawing.value = built
            currentPrefetch(dayIndex, key)
        }
    }

    val geometry = remember(layout, availableWidthDp) { TimelineGeometry(layout, availableWidthDp) }
    val overflows = geometry.width > availableWidthDp
    val scroll = if (overflows) rememberScrollState() else null
    val palette = if (key.dark) TimelinePalette.Dark else TimelinePalette.Light
    val placeholder =
        remember(layout, geometry, key.density, palette) { DayPlaceholder(layout, geometry, key.density, palette) }
    val currentOnBlock by rememberUpdatedState(onBlock)
    val currentOnFirstReplay by rememberUpdatedState(onFirstReplay)
    val dayId = day.id
    val placeholderTrace = remember(dayId) { PlaceholderReplayTrace() }
    val staticLayer =
        remember(drawing, resources, placeholder, probe, dayId) {
            Modifier.graphicsLayer().drawWithCache {
                onDrawBehind {
                    probe?.drew(TimelineProbe.Kind.STATIC, dayId)
                    val d = drawing.value
                    if (d == null) {
                        placeholderTrace.onPlaceholder()
                        Trace.beginSection(TimelineTraceNames.PLACEHOLDER)
                        drawIntoCanvas { placeholder.draw(it.nativeCanvas) }
                        Trace.endSection()
                    } else {
                        Trace.beginSection(TimelineTraceNames.REPLAY)
                        replay(d, resources)
                        Trace.endSection()
                        placeholderTrace.onReplay()
                        currentOnFirstReplay()
                    }
                }
            }
        }
    val taps =
        Modifier.pointerInput(dayId, drawing) {
            detectTapGestures(onTap = { p ->
                drawing.value
                    ?.hits
                    ?.resolve(p.x, p.y)
                    ?.let { currentOnBlock(it) }
            })
        }
    val description = stringResource(R.string.plan_plan_timeline)
    Box(
        modifier
            .let { if (scroll != null) it.horizontalScroll(scroll) else it }
            .semantics {
                contentDescription = description
                isTraversalGroup = true
            }.testTag("trip.day.$dayId"),
    ) {
        Column {
            headings()
            Box(Modifier.size(geometry.width.dp, layout.height.dp)) {
                Spacer(Modifier.matchParentSize().then(staticLayer).then(taps))
                NowLine(clock, dayId, geometry.width)
                TimelineTargets(drawing, dayId, onBlock, Modifier.matchParentSize(), actions, onCheck)
            }
        }
    }
}

private const val LOG_TAG = "CfTimeline"
