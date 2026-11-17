package net.chronoflow.feature.launch.model

import kotlin.time.Duration
import kotlin.time.DurationUnit

fun LaunchTimeline.withReadyExit(start: Duration): LaunchTimeline {
    val timeline = withoutExit()
    val exit = maxOf(if (reduceMotion) 0.0 else 1.5, start.toDouble(DurationUnit.SECONDS))
    timeline.append(
        LayerID.Container,
        KeyPath.OPACITY,
        exit,
        exit + if (reduceMotion) 0.3 else 0.4,
        TrackValue.Scalar(0.0),
        CubicBezier.easeOut,
    )
    if (!reduceMotion) {
        timeline.append(
            LayerID.Container,
            KeyPath.SCALE,
            exit,
            exit + 0.4,
            TrackValue.Scalar(1.08),
            CubicBezier.easeOut,
        )
    }
    return timeline
}

fun LaunchTimeline.withStatus(start: Duration): LaunchTimeline {
    val timeline = copyTracks()
    val time = maxOf(0.0, start.toDouble(DurationUnit.SECONDS))
    timeline.set(
        LayerID.Status,
        KeyPath.OPACITY,
        if (time == 0.0) doubleArrayOf(0.0, 0.3) else doubleArrayOf(0.0, time, time + 0.3),
        if (time == 0.0) doubleArrayOf(0.0, 1.0) else doubleArrayOf(0.0, 0.0, 1.0),
        if (time == 0.0) arrayOf(CubicBezier.easeOut) else arrayOf(CubicBezier.linear, CubicBezier.easeOut),
    )
    timeline.replace(
        LaunchTimeline.Track(
            LayerID.StatusDot,
            KeyPath.OPACITY,
            if (reduceMotion) doubleArrayOf(0.0) else doubleArrayOf(0.0, 0.48, 0.96, 1.2),
            if (reduceMotion) doubleArrayOf(1.0) else doubleArrayOf(0.25, 1.0, 0.25, 0.25),
            curves =
                if (reduceMotion) {
                    emptyArray()
                } else {
                    arrayOf(
                        CubicBezier.easeInOut,
                        CubicBezier.easeInOut,
                        CubicBezier.linear,
                    )
                },
            repeatInterval = if (reduceMotion) null else 1.2,
            replicaCount = 3,
            replicaDelay = if (reduceMotion) 0.0 else 0.2,
        ),
    )
    return timeline
}
