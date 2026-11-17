package net.chronoflow.feature.trip.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Test hook of the perf benchmarks, not UI: iOS `trip.timeline.opening` until the first timeline frame is presented,
 * then `trip.timeline.presented`. It is 1 dp because UiAutomator skips nodes with empty bounds. It takes the `State` so
 * that only this composable recomposes when the flag flips.
 */
@Suppress("ktlint:compose:state-param-check")
@Composable
internal fun TimelinePresentedMarker(
    presented: State<Boolean>,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(PRESENTED_MARKER_SIZE_DP.dp).testTag(if (presented.value) PRESENTED_TAG else OPENING_TAG))
}

internal const val OPENING_TAG = "trip.timeline.opening"
internal const val PRESENTED_TAG = "trip.timeline.presented"

private const val PRESENTED_MARKER_SIZE_DP = 1f
