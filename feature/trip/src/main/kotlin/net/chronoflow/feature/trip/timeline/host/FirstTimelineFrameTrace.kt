package net.chronoflow.feature.trip.timeline.host

import android.view.Choreographer
import net.chronoflow.feature.trip.timeline.TimelineTraceNames
import net.chronoflow.feature.trip.timeline.TimelineTracer

/**
 * Ends the `cf.timeline.firstFrame` async slice two frames after the first non-placeholder replay. Main thread only;
 * later calls are one boolean check. The ViewModel begins the slice in `init`.
 */
class FirstTimelineFrameTrace(
    private val tracer: TimelineTracer = TimelineTracer.Android,
    private val choreographer: () -> Choreographer = Choreographer::getInstance,
) {
    private var done = false

    fun onReplay() {
        if (done) return
        done = true
        val frames = choreographer()
        frames.postFrameCallback {
            frames.postFrameCallback { tracer.endAsync(TimelineTraceNames.FIRST_FRAME, COOKIE) }
        }
    }

    companion object {
        const val COOKIE = 0
    }
}
