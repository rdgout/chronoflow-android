package net.chronoflow.feature.launch.model

import kotlin.time.Duration
import kotlin.time.DurationUnit

internal fun LaunchTimeline.withWelcomeHandOff(start: Duration): LaunchTimeline {
    if (reduceMotion) return withReadyExit(start)
    val timeline = withoutExit()
    val exit = maxOf(1.6, start.toDouble(DurationUnit.SECONDS))
    timeline.append(
        LayerID.Container,
        KeyPath.OPACITY,
        exit + 0.7,
        exit + 0.95,
        TrackValue.Scalar(0.0),
        CubicBezier.easeOut,
    )
    return timeline
}
