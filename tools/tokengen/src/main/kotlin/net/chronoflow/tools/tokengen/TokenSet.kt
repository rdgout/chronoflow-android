package net.chronoflow.tools.tokengen

class Metric(
    val name: String,
    val value: Double,
) {
    override fun equals(other: Any?): Boolean = other is Metric && other.name == name && other.value == value

    override fun hashCode(): Int = name.hashCode() * 31 + value.hashCode()

    override fun toString(): String = "$name=$value"
}

class TypeEntry(
    val name: String,
    val face: String,
    val size: Double,
    val lineHeight: Double?,
    val trackingPt: Double,
) {
    override fun equals(other: Any?): Boolean =
        other is TypeEntry &&
            other.name == name &&
            other.face == face &&
            other.size == size &&
            other.lineHeight == lineHeight &&
            other.trackingPt == trackingPt

    override fun hashCode(): Int = name.hashCode() * 31 + size.hashCode()

    override fun toString(): String = "$name($face $size/$lineHeight $trackingPt)"
}

class TokenSet(
    val metrics: List<Metric>,
    val styles: List<TypeEntry> = emptyList(),
    val lists: List<Pair<String, List<Double>>> = emptyList(),
) {
    fun metric(name: String): Double =
        metrics.firstOrNull { it.name == name }?.value ?: throw GenerationError("Missing metric: $name")

    fun style(name: String): TypeEntry =
        styles.firstOrNull { it.name == name } ?: throw GenerationError("Missing style: $name")

    override fun equals(other: Any?): Boolean =
        other is TokenSet && other.metrics == metrics && other.styles == styles && other.lists == lists

    override fun hashCode(): Int = metrics.hashCode()
}

/** Faces that exist as font files (`core/designsystem/src/main/res/font`). */
val knownFaces =
    setOf(
        "Figtree400",
        "Figtree500",
        "Figtree600",
        "Figtree700",
        "Fraunces400Opsz20",
        "Fraunces400Opsz32",
        "Fraunces400Opsz48",
        "Fraunces500Opsz20",
        "Fraunces500Opsz32",
        "Fraunces500Opsz48",
    )

fun face(name: String): String = if (name in knownFaces) name else throw GenerationError("Unsupported typeface: $name")
