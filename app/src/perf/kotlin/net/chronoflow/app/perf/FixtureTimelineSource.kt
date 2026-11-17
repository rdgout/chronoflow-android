package net.chronoflow.app.perf

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.testing.LargeTimelineFixtures
import net.chronoflow.feature.trip.timeline.host.TimelineSource
import net.chronoflow.feature.trip.timeline.host.TimelineSourceInput

/** One emission of a fixture's full `TripDetail`: no marks, 24 hour time, nothing hidden. The read and decode run on [worker]. */
class FixtureTimelineSource(
    private val name: String,
    private val worker: CoroutineDispatcher,
) : TimelineSource {
    override fun inputs(): Flow<TimelineSourceInput> =
        flow {
            emit(
                TimelineSourceInput(
                    LargeTimelineFixtures.detailBytes(name),
                    emptyList(),
                    TimeFormat.Value24,
                    emptyList(),
                ),
            )
        }.flowOn(worker)
}
