package net.chronoflow.feature.launch.model

enum class LayerID(
    val rawValue: String,
) {
    Container("container"),
    Sky("sky"),
    Halo("halo"),
    Sun("sun"),
    Band("band"),
    Path("path"),
    Plane("plane"),
    Moon("moon"),
    Wordmark("wordmark"),
    Status("status"),
    StatusDot("statusDot"),
}

object KeyPath {
    const val OPACITY = "opacity"
    const val SCALE = "transform.scale"
    const val TRANSLATION_Y = "transform.translation.y"
    const val POSITION = "position"
    const val STROKE_END = "strokeEnd"
}

sealed interface TrackValue {
    data class Scalar(
        val value: Double,
    ) : TrackValue

    data class Point(
        val point: LaunchPoint,
    ) : TrackValue
}

data class LayerValues(
    val properties: Map<String, TrackValue> = emptyMap(),
    val replicaOpacities: List<Double> = emptyList(),
) {
    operator fun get(keyPath: String): TrackValue? = properties[keyPath]
}
