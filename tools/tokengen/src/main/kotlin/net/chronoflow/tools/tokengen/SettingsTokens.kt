package net.chronoflow.tools.tokengen

import java.nio.file.Path

fun settingsTokens(
    root: Path,
    ui: TokenSet,
): TokenSet {
    val source = parseJson(root.ios("Settings.source.json")).obj()
    val metrics =
        source.keys.sorted().map { name ->
            val metric = source.member(name).obj()
            val board = root.resolve(metric.member("source").string()).readText()
            val values = captures(metric.member("pattern").string(), board)
            val raw = values.firstOrNull()
            if (raw == null || raw.toDoubleOrNull() == null || values.toSet().size != 1) {
                throw GenerationError("Settings metric changed: $name")
            }
            Metric(name, if (name.endsWith("Ratio")) raw.toDouble() / 100 else raw.toDouble())
        }
    val planBoard = root.design("SetPlanCredits.dc.html")
    val rowWidths =
        captures(
            """width: ([0-9]+)px; height: 16px; border-radius: 9999px; overflow: hidden;"><x-import""",
            planBoard,
        ).takeLast(3)
    if (rowWidths.size != 3) throw GenerationError("Settings skeleton rows changed")

    val parsed =
        listOf(
            Triple("settingsHint", "SetProfile", """<p style="([^"]*font-size: 13px; line-height: 18px[^"]*)""""),
            Triple(
                "settingsPromptBody",
                "PasswordPrompt",
                """<p style="([^"]*font-size: 15px; line-height: 22px[^"]*)"""",
            ),
            Triple("settingsCodes", "Set2FAOn", """<ul aria-label="[^"]+" style="([^"]+)""""),
            Triple("settingsTime", "SetBodyClock", """<span style="(font-size: 24px;[^"]+)""""),
            Triple("settingsEyebrow", "SetPreferences", """<h2 style="([^"]+)""""),
        ).map { (name, file, pattern) ->
            val declaration = captures(pattern, root.design("$file.dc.html")).firstOrNull()
            val size = declaration?.let { captures("""font-size: ([0-9.]+)px""", it).firstOrNull() }
            val line = declaration?.let { captures("""line-height: ([0-9.]+)px""", it).firstOrNull() }
            if (declaration == null || size == null ||
                line == null
            ) {
                throw GenerationError("Settings typography changed: $name")
            }
            val weight = captures("""font-weight: ([0-9]+)""", declaration).firstOrNull() ?: "400"
            val tracking =
                captures("""letter-spacing: ([0-9.]+)em""", declaration).firstOrNull()?.let {
                    it.toDouble() *
                        size.toDouble()
                }
                    ?: 0.0
            TypeEntry(name, face("Figtree$weight"), size.toDouble(), line.toDouble(), tracking)
        }
    val composites =
        listOf(
            Triple("settingsRow", "screenBodyLg", "500"),
            Triple("settingsStrong", "screenBodyLg", "600"),
            Triple("settingsPill", "screenBody", "500"),
            Triple("settingsBadge", "screenMeta", "500"),
        ).map { (name, base, weight) ->
            val style = ui.style(base)
            TypeEntry(name, face("Figtree$weight"), style.size, style.lineHeight, style.trackingPt)
        }
    return TokenSet(metrics, parsed + composites, listOf("skeletonRowWidths" to rowWidths.map { it.toDouble() }))
}
