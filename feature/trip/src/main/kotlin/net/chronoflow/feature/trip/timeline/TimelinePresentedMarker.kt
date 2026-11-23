package net.chronoflow.feature.trip.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag

/**
 * Test hook of the perf benchmarks, not UI: iOS `trip.timeline.opening` until the first timeline frame is presented,
 * then `trip.timeline.presented`. It is 1 px because UiAutomator skips nodes with empty bounds. It takes the `State` so
 * that only this composable recomposes when the flag flips.
 */
@Suppress("ktlint:compose:state-param-check")
@Composable
internal fun TimelinePresentedMarker(
    presented: State<Boolean>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .layout { measurable, constraints ->
                val exact = constraints.copy(minWidth = 1, minHeight = 1, maxWidth = 1, maxHeight = 1)
                val placeable = measurable.measure(exact)
                layout(1, 1) { placeable.place(0, 0) }
            }.testTag(if (presented.value) PRESENTED_TAG else OPENING_TAG),
    )
}

internal const val OPENING_TAG = "trip.timeline.opening"
internal const val PRESENTED_TAG = "trip.timeline.presented"
