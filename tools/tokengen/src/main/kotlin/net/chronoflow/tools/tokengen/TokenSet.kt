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
    val allowFontSynthesis: Boolean = true,
) {
    override fun equals(other: Any?): Boolean =
        other is TypeEntry &&
            other.name == name &&
            other.face == face &&
            other.size == size &&
            other.lineHeight == lineHeight &&
            other.trackingPt == trackingPt &&
            other.allowFontSynthesis == allowFontSynthesis

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

/** Faces shipped in `core/designsystem/src/main/res/font`. */
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

fun face(name: String): String {
    val fraunces = Regex("""Fraunces(\d+)Opsz(20|32|48)""").matchEntire(name)
    val resolved =
        if (fraunces != null && fraunces.groupValues[1].toInt() > 500) {
            "Fraunces500Opsz${fraunces.groupValues[2]}"
        } else {
            name
        }
    return if (resolved in knownFaces) resolved else throw GenerationError("Unsupported typeface: $name")
}
