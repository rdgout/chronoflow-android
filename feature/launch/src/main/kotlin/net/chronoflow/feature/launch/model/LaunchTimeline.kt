package net.chronoflow.feature.launch.model

import kotlin.time.Duration
import kotlin.time.DurationUnit

class LaunchTimeline private constructor(
    val size: LaunchSize,
    val reduceMotion: Boolean,
) {
    private val mutableTracks = mutableListOf<Track>()
    val tracks: List<Track> get() = mutableTracks.toList()

    class Track(
        val layer: LayerID,
        val keyPath: String,
        keyTimes: DoubleArray,
        xs: DoubleArray,
        ys: DoubleArray? = null,
        curves: Array<CubicBezier> = Array(keyTimes.size - 1) { CubicBezier.linear },
        val repeatInterval: Double? = null,
        val replicaCount: Int = 1,
        val replicaDelay: Double = 0.0,
    ) {
        val keyTimes = keyTimes.copyOf()
        private val xs = xs.copyOf()
        private val ys = ys?.copyOf()
        val curves = curves.copyOf()

        init {
            require(keyTimes.isNotEmpty() && keyTimes.size == xs.size && curves.size == xs.size - 1)
            require(ys == null || ys.size == xs.size)
            require((1 until keyTimes.size).all { keyTimes[it - 1] < keyTimes[it] })
        }

        val values: List<TrackValue> get() =
            xs.indices.map { index ->
                if (ys == null) TrackValue.Scalar(xs[index]) else TrackValue.Point(LaunchPoint(xs[index], ys[index]))
            }

        fun scalarAt(seconds: Double): Double = coordinateAt(seconds, xs)

        fun xAt(seconds: Double): Double = coordinateAt(seconds, xs)

        fun yAt(seconds: Double): Double = coordinateAt(seconds, requireNotNull(ys))

        fun sample(at: Duration): TrackValue {
            val seconds = at.toDouble(DurationUnit.SECONDS)
            return if (ys ==
                null
            ) {
                TrackValue.Scalar(scalarAt(seconds))
            } else {
                TrackValue.Point(LaunchPoint(xAt(seconds), yAt(seconds)))
            }
        }

        private fun coordinateAt(
            seconds: Double,
            coordinates: DoubleArray,
        ): Double {
            var time = seconds
            if (repeatInterval != null && time > keyTimes[0]) time = keyTimes[0] + (time - keyTimes[0]) % repeatInterval
            if (time <= keyTimes[0]) return coordinates[0]
            var upper = 1
            while (upper < keyTimes.size && keyTimes[upper] <= time) upper++
            if (upper == keyTimes.size) return coordinates.last()
            val lower = upper - 1
            val p = curves[lower].value((time - keyTimes[lower]) / (keyTimes[upper] - keyTimes[lower]))
            return coordinates[lower] + (coordinates[upper] - coordinates[lower]) * p
        }

        override fun equals(other: Any?): Boolean =
            other is Track &&
                layer == other.layer &&
                keyPath == other.keyPath &&
                keyTimes.contentEquals(other.keyTimes) &&
                xs.contentEquals(other.xs) &&
                ys.contentEquals(other.ys) &&
                curves.contentEquals(other.curves) &&
                repeatInterval == other.repeatInterval &&
                replicaCount == other.replicaCount &&
                replicaDelay == other.replicaDelay

        override fun hashCode(): Int {
            var result = layer.hashCode()
            result = 31 * result + keyPath.hashCode()
            result = 31 * result + keyTimes.contentHashCode()
            result = 31 * result + xs.contentHashCode()
            result = 31 * result + ys.contentHashCode()
            result = 31 * result + curves.contentHashCode()
            result = 31 * result + (repeatInterval?.hashCode() ?: 0)
            result = 31 * result + replicaCount
            return 31 * result + replicaDelay.hashCode()
        }
    }

    fun sample(at: Duration): Map<LayerID, LayerValues> {
        val result = mutableMapOf<LayerID, LayerValues>()
        for (track in mutableTracks) {
            val old = result[track.layer] ?: LayerValues()
            val replicas =
                if (track.replicaCount > 1) {
                    List(track.replicaCount) { i ->
                        track.scalarAt(at.toDouble(DurationUnit.SECONDS) - i * track.replicaDelay)
                    }
                } else {
                    old.replicaOpacities
                }
            result[track.layer] = LayerValues(old.properties + (track.keyPath to track.sample(at)), replicas)
        }
        return result
    }

    internal fun set(
        layer: LayerID,
        keyPath: String,
        times: DoubleArray,
        scalars: DoubleArray,
        curves: Array<CubicBezier> =
            Array(times.size - 1) { CubicBezier.linear },
    ) = replace(Track(layer, keyPath, times, scalars, curves = curves))

    internal fun setPoints(
        layer: LayerID,
        keyPath: String,
        times: DoubleArray,
        points: List<LaunchPoint>,
        curves: Array<CubicBezier> =
            Array(times.size - 1) { CubicBezier.linear },
    ) = replace(
        Track(
            layer,
            keyPath,
            times,
            points.map { it.x }.toDoubleArray(),
            points.map { it.y }.toDoubleArray(),
            curves,
        ),
    )

    internal fun replace(track: Track) {
        val index = mutableTracks.indexOfFirst { it.layer == track.layer && it.keyPath == track.keyPath }
        if (index >= 0) mutableTracks[index] = track else mutableTracks.add(track)
    }

    internal fun withoutExit(): LaunchTimeline =
        opening(size, reduceMotion).also { timeline ->
            mutableTracks.filter { it.layer == LayerID.Status || it.layer == LayerID.StatusDot }.forEach(
                timeline::replace,
            )
        }

    internal fun append(
        layer: LayerID,
        keyPath: String,
        start: Double,
        end: Double,
        value: TrackValue,
        curve: CubicBezier,
    ) {
        val original = mutableTracks.first { it.layer == layer && it.keyPath == keyPath }
        require(start >= original.keyTimes.last() && end > start)
        val times = original.keyTimes.toMutableList()
        val values = original.values.toMutableList()
        val curves = original.curves.toMutableList()
        require(value::class == values.last()::class)
        if (start > times.last()) {
            times.add(start)
            values.add(values.last())
            curves.add(CubicBezier.linear)
        }
        times.add(end)
        values.add(value)
        curves.add(curve)
        val xs =
            values
                .map { if (it is TrackValue.Scalar) it.value else (it as TrackValue.Point).point.x }
                .toDoubleArray()
        val ys =
            if (value is TrackValue.Point) {
                values
                    .map {
                        (it as TrackValue.Point).point.y
                    }.toDoubleArray()
            } else {
                null
            }
        replace(Track(layer, keyPath, times.toDoubleArray(), xs, ys, curves.toTypedArray()))
    }

    companion object {
        fun opening(
            size: LaunchSize,
            reduceMotion: Boolean,
        ): LaunchTimeline {
            val layout = LaunchLayout(size)
            val timeline = LaunchTimeline(size, reduceMotion)
            with(timeline) {
                for (layer in LayerID.entries) {
                    set(
                        layer,
                        KeyPath.OPACITY,
                        doubleArrayOf(0.0),
                        doubleArrayOf(
                            if (layer ==
                                LayerID.Container ||
                                layer == LayerID.Moon
                            ) {
                                1.0
                            } else {
                                0.0
                            },
                        ),
                    )
                }
                set(LayerID.Container, KeyPath.SCALE, doubleArrayOf(0.0), doubleArrayOf(1.0))
                setPoints(LayerID.Moon, KeyPath.POSITION, doubleArrayOf(0.0), listOf(layout.moonCentre))
                set(LayerID.Moon, KeyPath.SCALE, doubleArrayOf(0.0), doubleArrayOf(1.0))
                set(LayerID.Wordmark, KeyPath.TRANSLATION_Y, doubleArrayOf(0.0), doubleArrayOf(8.0))
                set(LayerID.Path, KeyPath.STROKE_END, doubleArrayOf(0.0), doubleArrayOf(0.0))
                for (layer in listOf(
                    LayerID.Sun,
                    LayerID.Halo,
                )) {
                    set(
                        layer,
                        KeyPath.TRANSLATION_Y,
                        doubleArrayOf(0.0),
                        doubleArrayOf(if (reduceMotion) 0.0 else 110.0),
                    )
                }
                if (reduceMotion) return timeline
                val ease = arrayOf(CubicBezier.linear, CubicBezier.easeOut)
                for (layer in listOf(
                    LayerID.Sky,
                    LayerID.Band,
                )) {
                    set(layer, KeyPath.OPACITY, doubleArrayOf(0.0, 0.3, 1.1), doubleArrayOf(0.0, 0.0, 1.0), ease)
                }
                for (layer in listOf(LayerID.Sun, LayerID.Halo)) {
                    set(layer, KeyPath.OPACITY, doubleArrayOf(0.0, 0.4, 1.3), doubleArrayOf(0.0, 0.0, 1.0), ease)
                    set(
                        layer,
                        KeyPath.TRANSLATION_Y,
                        doubleArrayOf(0.0, 0.4, 1.3),
                        doubleArrayOf(110.0, 110.0, 0.0),
                        ease,
                    )
                }
                set(LayerID.Path, KeyPath.OPACITY, doubleArrayOf(0.0), doubleArrayOf(1.0))
                set(
                    LayerID.Path,
                    KeyPath.STROKE_END,
                    doubleArrayOf(0.0, 0.5, 0.56, 1.4),
                    doubleArrayOf(0.0, 0.0, 0.03, 1.0),
                )
                val flight = FlightPath(size)
                setPoints(
                    LayerID.Plane,
                    KeyPath.POSITION,
                    doubleArrayOf(0.0) + flight.positionKeyTimes.toDoubleArray(),
                    listOf(flight.start) + flight.positionValues,
                )
                set(
                    LayerID.Plane,
                    KeyPath.OPACITY,
                    doubleArrayOf(0.0, 0.5, 0.56, 1.4, 1.5),
                    doubleArrayOf(0.0, 0.0, 1.0, 1.0, 0.0),
                )
                setPoints(
                    LayerID.Moon,
                    KeyPath.POSITION,
                    doubleArrayOf(0.0, 0.3, 1.1),
                    listOf(
                        layout.moonCentre,
                        layout.moonCentre,
                        layout.moonCentre.copy(
                            y =
                                layout.moonCentre.y - 28,
                        ),
                    ),
                    ease,
                )
                set(LayerID.Moon, KeyPath.SCALE, doubleArrayOf(0.0, 0.3, 1.1), doubleArrayOf(1.0, 1.0, 0.86), ease)
                set(LayerID.Wordmark, KeyPath.OPACITY, doubleArrayOf(0.0, 1.0, 1.4), doubleArrayOf(0.0, 0.0, 1.0), ease)
                set(
                    LayerID.Wordmark,
                    KeyPath.TRANSLATION_Y,
                    doubleArrayOf(0.0, 1.0, 1.4),
                    doubleArrayOf(8.0, 8.0, 0.0),
                    ease,
                )
            }
            return timeline
        }
    }
}
