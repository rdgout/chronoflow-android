package net.chronoflow.feature.launch.model

class LaunchFrame {
    var containerOpacity: Double = 1.0
    var containerScale: Double = 1.0
    var skyOpacity: Double = 0.0
    var bandOpacity: Double = 0.0
    var haloOpacity: Double = 0.0
    var haloTranslationY: Double = 0.0
    var sunOpacity: Double = 0.0
    var sunTranslationY: Double = 0.0
    var pathOpacity: Double = 0.0
    var strokeEnd: Double = 0.0
    var planeOpacity: Double = 0.0
    var planeX: Double = 0.0
    var planeY: Double = 0.0
    var moonOpacity: Double = 1.0
    var moonX: Double = 0.0
    var moonY: Double = 0.0
    var moonScale: Double = 1.0
    var wordmarkOpacity: Double = 0.0
    var wordmarkTranslationY: Double = 8.0
    var statusOpacity: Double = 0.0
    val statusDotOpacity = DoubleArray(3)
}

class LaunchFrameSampler(
    timeline: LaunchTimeline,
) {
    private val containerOpacityTrack =
        timeline.tracks.firstOrNull {
            it.layer == LayerID.Container &&
                it.keyPath == KeyPath.OPACITY
        }
    private val containerScaleTrack =
        timeline.tracks.firstOrNull {
            it.layer == LayerID.Container &&
                it.keyPath == KeyPath.SCALE
        }
    private val skyOpacityTrack =
        timeline.tracks.firstOrNull {
            it.layer == LayerID.Sky && it.keyPath == KeyPath.OPACITY
        }
    private val bandOpacityTrack =
        timeline.tracks.firstOrNull {
            it.layer == LayerID.Band &&
                it.keyPath == KeyPath.OPACITY
        }
    private val haloOpacityTrack =
        timeline.tracks.firstOrNull {
            it.layer == LayerID.Halo &&
                it.keyPath == KeyPath.OPACITY
        }
    private val haloTranslationYTrack =
        timeline.tracks.firstOrNull {
            it.layer == LayerID.Halo &&
                it.keyPath == KeyPath.TRANSLATION_Y
        }
    private val sunOpacityTrack =
        timeline.tracks.firstOrNull {
            it.layer == LayerID.Sun && it.keyPath == KeyPath.OPACITY
        }
    private val sunTranslationYTrack =
        timeline.tracks.firstOrNull {
            it.layer == LayerID.Sun &&
                it.keyPath == KeyPath.TRANSLATION_Y
        }
    private val pathOpacityTrack =
        timeline.tracks.firstOrNull {
            it.layer == LayerID.Path &&
                it.keyPath == KeyPath.OPACITY
        }
    private val strokeEndTrack =
        timeline.tracks.firstOrNull {
            it.layer == LayerID.Path &&
                it.keyPath == KeyPath.STROKE_END
        }
    private val planeOpacityTrack =
        timeline.tracks.firstOrNull {
            it.layer == LayerID.Plane &&
                it.keyPath == KeyPath.OPACITY
        }
    private val planeXTrack =
        timeline.tracks.firstOrNull {
            it.layer == LayerID.Plane && it.keyPath == KeyPath.POSITION
        }
    private val planeYTrack =
        timeline.tracks.firstOrNull {
            it.layer == LayerID.Plane && it.keyPath == KeyPath.POSITION
        }
    private val moonOpacityTrack =
        timeline.tracks.firstOrNull {
            it.layer == LayerID.Moon &&
                it.keyPath == KeyPath.OPACITY
        }
    private val moonXTrack = timeline.tracks.firstOrNull { it.layer == LayerID.Moon && it.keyPath == KeyPath.POSITION }
    private val moonYTrack = timeline.tracks.firstOrNull { it.layer == LayerID.Moon && it.keyPath == KeyPath.POSITION }
    private val moonScaleTrack = timeline.tracks.firstOrNull { it.layer == LayerID.Moon && it.keyPath == KeyPath.SCALE }
    private val wordmarkOpacityTrack =
        timeline.tracks.firstOrNull {
            it.layer == LayerID.Wordmark &&
                it.keyPath == KeyPath.OPACITY
        }
    private val wordmarkTranslationYTrack =
        timeline.tracks.firstOrNull {
            it.layer == LayerID.Wordmark &&
                it.keyPath == KeyPath.TRANSLATION_Y
        }
    private val statusOpacityTrack =
        timeline.tracks.firstOrNull {
            it.layer == LayerID.Status &&
                it.keyPath == KeyPath.OPACITY
        }
    private val dotTrack =
        timeline.tracks.firstOrNull {
            it.layer == LayerID.StatusDot && it.keyPath == KeyPath.OPACITY
        }
    private val moonXDefault = timeline.size.width / 2
    private val moonYDefault = timeline.size.height / 2
    val settledAfterSeconds: Double =
        if (timeline.tracks.any {
                it.repeatInterval != null
            }
        ) {
            Double.POSITIVE_INFINITY
        } else {
            timeline.tracks.maxOf { it.keyTimes.last() }
        }

    fun containerOpacity(seconds: Double): Double = containerOpacityTrack?.scalarAt(seconds) ?: 1.0

    fun containerScale(seconds: Double): Double = containerScaleTrack?.scalarAt(seconds) ?: 1.0

    fun sampleInto(
        seconds: Double,
        out: LaunchFrame,
    ) {
        out.containerOpacity = containerOpacityTrack?.scalarAt(seconds) ?: 1.0
        out.containerScale = containerScaleTrack?.scalarAt(seconds) ?: 1.0
        out.skyOpacity = skyOpacityTrack?.scalarAt(seconds) ?: 0.0
        out.bandOpacity = bandOpacityTrack?.scalarAt(seconds) ?: 0.0
        out.haloOpacity = haloOpacityTrack?.scalarAt(seconds) ?: 0.0
        out.haloTranslationY = haloTranslationYTrack?.scalarAt(seconds) ?: 0.0
        out.sunOpacity = sunOpacityTrack?.scalarAt(seconds) ?: 0.0
        out.sunTranslationY = sunTranslationYTrack?.scalarAt(seconds) ?: 0.0
        out.pathOpacity = pathOpacityTrack?.scalarAt(seconds) ?: 0.0
        out.strokeEnd = strokeEndTrack?.scalarAt(seconds) ?: 0.0
        out.planeOpacity = planeOpacityTrack?.scalarAt(seconds) ?: 0.0
        out.planeX = planeXTrack?.xAt(seconds) ?: 0.0
        out.planeY = planeYTrack?.yAt(seconds) ?: 0.0
        out.moonOpacity = moonOpacityTrack?.scalarAt(seconds) ?: 1.0
        out.moonX = moonXTrack?.xAt(seconds) ?: moonXDefault
        out.moonY = moonYTrack?.yAt(seconds) ?: moonYDefault
        out.moonScale = moonScaleTrack?.scalarAt(seconds) ?: 1.0
        out.wordmarkOpacity = wordmarkOpacityTrack?.scalarAt(seconds) ?: 0.0
        out.wordmarkTranslationY = wordmarkTranslationYTrack?.scalarAt(seconds) ?: 8.0
        out.statusOpacity = statusOpacityTrack?.scalarAt(seconds) ?: 0.0
        var index = 0
        while (index < out.statusDotOpacity.size) {
            out.statusDotOpacity[index] = dotTrack?.scalarAt(seconds - index * dotTrack.replicaDelay) ?: 0.0
            index++
        }
    }
}
