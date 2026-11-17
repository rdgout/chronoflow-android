package net.chronoflow.feature.launch.model

import kotlin.time.Duration
import kotlin.time.DurationUnit

private const val EARLIEST_EXIT = 1.6
private const val FLIGHT = 0.7
private const val SUN_EXIT = 0.6
private const val PATH_EXIT = 0.4
private const val WORDMARK_EXIT = 0.3
private const val DISSOLVE_END = 0.95
private const val SUN_TRANSLATION = 120.0
private const val WORDMARK_TRANSLATION = -6.0
private const val MOON_SIZE = 104.0

fun LaunchTimeline.withWelcomeExit(
    start: Duration,
    size: LaunchSize,
    moonTarget: LaunchRect,
): LaunchTimeline {
    require(size == this.size)
    require(
        moonTarget.width > 0 && moonTarget.width.isFinite() && moonTarget.midX.isFinite() && moonTarget.midY.isFinite(),
    )
    if (reduceMotion) return withReadyExit(start)
    val timeline = withoutExit()
    val exit = maxOf(EARLIEST_EXIT, start.toDouble(DurationUnit.SECONDS))
    timeline.append(LayerID.Band, KeyPath.OPACITY, exit, exit + FLIGHT, TrackValue.Scalar(0.0), CubicBezier.easeOut)
    for (layer in listOf(LayerID.Sun, LayerID.Halo)) {
        timeline.append(layer, KeyPath.OPACITY, exit, exit + SUN_EXIT, TrackValue.Scalar(0.0), CubicBezier.easeOut)
        timeline.append(
            layer,
            KeyPath.TRANSLATION_Y,
            exit,
            exit + SUN_EXIT,
            TrackValue.Scalar(SUN_TRANSLATION),
            CubicBezier.easeOut,
        )
    }
    timeline.append(LayerID.Path, KeyPath.OPACITY, exit, exit + PATH_EXIT, TrackValue.Scalar(0.0), CubicBezier.linear)
    timeline.append(
        LayerID.Wordmark,
        KeyPath.OPACITY,
        exit,
        exit + WORDMARK_EXIT,
        TrackValue.Scalar(0.0),
        CubicBezier.easeOut,
    )
    timeline.append(
        LayerID.Wordmark,
        KeyPath.TRANSLATION_Y,
        exit,
        exit + WORDMARK_EXIT,
        TrackValue.Scalar(WORDMARK_TRANSLATION),
        CubicBezier.easeOut,
    )
    timeline.append(
        LayerID.Moon,
        KeyPath.POSITION,
        exit,
        exit + FLIGHT,
        TrackValue.Point(LaunchPoint(moonTarget.midX, moonTarget.midY)),
        CubicBezier.easeInOut,
    )
    timeline.append(
        LayerID.Moon,
        KeyPath.SCALE,
        exit,
        exit + FLIGHT,
        TrackValue.Scalar(moonTarget.width / MOON_SIZE),
        CubicBezier.easeInOut,
    )
    timeline.append(
        LayerID.Container,
        KeyPath.OPACITY,
        exit + FLIGHT,
        exit + DISSOLVE_END,
        TrackValue.Scalar(0.0),
        CubicBezier.easeOut,
    )
    return timeline
}
