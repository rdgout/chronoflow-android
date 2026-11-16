package net.chronoflow.tools.tokengen

import java.nio.file.Path

fun termsTokens(
    root: Path,
    ui: TokenSet,
): TokenSet {
    val board = root.design("TermsUpdate.dc.html")
    val patterns =
        mapOf(
            "brandSize" to """alt="" width="(\d+)"""",
            "summaryPadding" to """gap: 12px; padding: (\d+)px;""",
            "bulletSize" to """width: (\d+)px; height: 6px;""",
            "policyHeight" to """min-height: (\d+)px;""",
            "titleSize" to """font-size: (\d+)px; line-height: 38px;""",
            "titleLineHeight" to """font-size: 32px; line-height: (\d+)px;""",
            "bodySize" to """gap: 12px; font-size: (\d+)px;""",
            "bodyLineHeight" to """font-size: 15px; line-height: (\d+)px;""",
        )
    val metrics = patterns.keys.sorted().map { Metric(it, firstCapture(patterns.getValue(it), board).toDouble()) }
    val byName = metrics.associate { it.name to it.value }

    fun entry(
        name: String,
        face: String,
        size: String,
        line: String,
        tracking: TypeEntry,
    ) = TypeEntry(name, face(face), byName.getValue(size), byName.getValue(line), tracking.trackingPt)
    return TokenSet(
        metrics,
        listOf(
            entry("termsTitle", "Fraunces400Opsz32", "titleSize", "titleLineHeight", ui.style("screenPageTitle")),
            entry("termsBody", "Figtree400", "bodySize", "bodyLineHeight", ui.style("screenBody")),
            entry("termsStrong", "Figtree600", "bodySize", "bodyLineHeight", ui.style("screenLabel")),
        ),
    )
}
