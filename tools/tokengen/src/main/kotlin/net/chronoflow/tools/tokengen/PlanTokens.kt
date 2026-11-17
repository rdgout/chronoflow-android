package net.chronoflow.tools.tokengen

import java.nio.file.Files
import java.nio.file.Path

class PlanTokens(
    /** `PlanMetrics`, in dp (`Double`); `pointsPerMinute` is derived at emit time. */
    val metrics: List<Metric>,
    /** `PlanTypeMetrics`: size, line height, and the weight as the face. */
    val types: List<TypeEntry>,
    /** Android-only metrics from `tools/tokengen/android-overlay.json`, emitted after the iOS values. */
    val overlay: List<OverlayMetric> = emptyList(),
)

class OverlayMetric(
    val name: String,
    val value: Double,
    val replaces: String,
)

fun planTokens(root: Path): PlanTokens {
    val source = parseJson(root.ios("Plan.source.json")).obj()
    val design = parseJson(root.design("ds/chronoflow/tokens.json")).obj()
    val points =
        design
            .member("spacing")
            .obj()
            .member("tokens")
            .arr()
            .mapNotNull { token ->
                val value = token.obj().member("value").string()
                if (value.endsWith("px")) {
                    value.dropLast(2).toDoubleOrNull()?.let {
                        token.obj().member("name").string() to
                            it
                    }
                } else {
                    null
                }
            }.toMap()
    val base = points["space-1"] ?: throw GenerationError("Missing spacing token space-1")

    fun sortedEntries(key: String) = source[key]?.obj()?.entries?.sortedBy { it.key } ?: emptyList()
    val metrics = mutableListOf<Metric>()
    for ((name, multiplier) in sortedEntries("spacingMultipliers")) metrics += Metric(name, base * multiplier.double())
    for ((name, token) in sortedEntries("spacingTokens")) {
        metrics +=
            Metric(name, points[token.string()] ?: throw GenerationError("Missing spacing token ${token.string()}"))
    }
    for ((name, value) in sortedEntries("scalars")) metrics += Metric(name, value.double())
    val type = source.member("type").obj()
    val lane = type.member("planLane").obj()
    metrics += Metric("laneLabelSize", lane.member("size").double())
    metrics += Metric("laneLabelLineHeight", lane.member("lineHeight").double())
    val types = mutableListOf<TypeEntry>()
    for ((name, values) in type.entries.sortedBy { it.key }) {
        val entry = values.obj()
        metrics += Metric("${name}Size", entry.member("size").double())
        metrics += Metric("${name}LineHeight", entry.member("lineHeight").double())
        types +=
            TypeEntry(
                name,
                face("Figtree${entry.member("weight").string()}"),
                entry.member("size").double(),
                entry.member("lineHeight").double(),
                0.0,
            )
    }
    val overlayFile = root.resolve("tools/tokengen/android-overlay.json")
    val overlay =
        if (Files.exists(overlayFile)) {
            parseJson(overlayFile.readText())
                .obj()
                .member("planMetrics")
                .obj()
                .entries
                .map { (name, entry) ->
                    OverlayMetric(name, entry.obj().member("value").double(), entry.obj().member("replaces").string())
                }
        } else {
            emptyList()
        }
    return PlanTokens(metrics, types, overlay)
}
