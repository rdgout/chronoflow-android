package net.chronoflow.feature.trip.timeline.host

import kotlinx.coroutines.flow.Flow
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.TimeFormat

/** Everything the host prepares a plan from. Phase 6b replaces this seam with `TripPageSource`. */
class TimelineSourceInput(
    val detailBytes: ByteArray,
    val marks: List<CheckMark>,
    val timeFormat: TimeFormat,
    val hidden: List<BlockType>,
)

interface TimelineSource {
    fun inputs(): Flow<TimelineSourceInput>
}
