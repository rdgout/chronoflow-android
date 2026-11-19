package net.chronoflow.app.perf

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.feature.trip.timeline.host.TimelineSource
import net.chronoflow.feature.trip.timeline.host.TimelineSourceInput

/** One emission of a fixture's full `TripDetail`: no marks, 24 hour time, nothing hidden. The caller generates the bytes first. */
class FixtureTimelineSource(
    private val detailBytes: ByteArray,
) : TimelineSource {
    override fun inputs(): Flow<TimelineSourceInput> =
        flow {
            emit(
                TimelineSourceInput(
                    detailBytes,
                    emptyList(),
                    TimeFormat.Value24,
                    emptyList(),
                ),
            )
        }
}
