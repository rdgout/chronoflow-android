package net.chronoflow.tools.tokengen

import java.nio.file.Path

/** `PlanUITokens` plus the `screen*` and wizard text styles of iOS `PlanUITokens.swift`. */
fun planUITokens(root: Path): TokenSet {
    val json = parseJson(root.design("ds/chronoflow/tokens.json")).obj()
    val metrics = mutableListOf<Metric>()
    for (group in listOf("spacing", "radius")) {
        for (token in json
            .member(group)
            .obj()
            .member("tokens")
            .arr()) {
            val value = token.obj().member("value").string()
            if (value.endsWith("px")) {
                metrics +=
                    Metric(camelCase(token.obj().member("name").string()), value.dropLast(2).toDouble())
            }
        }
    }
    metrics +=
        Metric(
            "markedOpacity",
            firstCapture("""opacity: ([\d.]+);""", root.design("TodayRowStates.dc.html")).toDouble(),
        )
    val patterns = parseJson(root.ios("PlanUI.source.json")).obj()
    for (key in patterns.keys.sorted().filter { it != "source" }) metrics += Metric(key, patterns.member(key).double())

    val styles = mutableListOf<TypeEntry>()
    for (group in json
        .member("type")
        .obj()
        .member("groups")
        .arr()) {
        val display = group.obj().member("family").string() == "display"
        for (style in group
            .obj()
            .member("styles")
            .arr()
            .map { it.obj() }) {
            val name = style.member("name").string()
            if (name == "page-title-lg") continue
            val size =
                style
                    .member("fontSize")
                    .string()
                    .dropLast(2)
                    .toDouble()
            val lineHeight = style.member("lineHeight").primitive()
            val line =
                if (lineHeight.isString) {
                    lineHeight.content.dropLast(2).toDouble()
                } else {
                    lineHeight.content.toDouble() *
                        size
                }
            val weight = style.member("fontWeight").string()
            val face =
                if (display) {
                    "Fraunces400Opsz${if (size >= 40) {
                        48
                    } else if (size >= 26) {
                        32
                    } else {
                        20
                    }}"
                } else {
                    "Figtree$weight"
                }
            val tracking =
                style["letterSpacing"]
                    ?.string()
                    ?.dropLast(2)
                    ?.toDoubleOrNull()
                    ?.let { it * size } ?: 0.0
            styles += TypeEntry(camelCase("screen-$name"), face(face), size, line, tracking)
        }
    }
    val base = TokenSet(metrics, styles)
    return TokenSet(metrics, styles + wizardStyles(base) + wizardPickerStyle(root))
}

private fun wizardStyles(ui: TokenSet): List<TypeEntry> {
    val body = ui.style("screenBody").trackingPt
    val bodyLg = ui.style("screenBodyLg")

    fun styleOf(
        name: String,
        face: String,
        sizeKey: String,
        lineKey: String,
        tracking: Double = body,
    ) = TypeEntry(name, face(face), ui.metric(sizeKey), ui.metric(lineKey), tracking)

    val longTitleSize = ui.metric("wizardLongTitleFontSize")
    return listOf(
        TypeEntry("walkthroughBody", "Figtree500", bodyLg.size, bodyLg.lineHeight, bodyLg.trackingPt),
        styleOf(
            "wizardReadySummary",
            "Fraunces400Opsz20",
            "wizardReadySummaryFontSize",
            "wizardReadySummaryLineHeight",
        ),
        styleOf(
            "wizardFailureTitle",
            "Fraunces400Opsz20",
            "wizardFailureTitleFontSize",
            "wizardFailureTitleLineHeight",
        ),
        styleOf("wizardFailureBody", "Figtree400", "wizardFailureBodyFontSize", "wizardFailureBodyLineHeight"),
        styleOf("wizardSecondary", "Figtree500", "wizardSecondaryFontSize", "wizardSecondaryLineHeight"),
        styleOf("wizardArc", "Fraunces400Opsz32", "wizardArcFontSize", "wizardArcLineHeight"),
        styleOf("wizardNavTitle", "Figtree500", "wizardRouteFontSize", "wizardRouteLineHeight"),
        styleOf(
            "wizardSearchCode",
            "Figtree700",
            "wizardSearchCodeFontSize",
            "wizardRouteLineHeight",
            ui.metric("wizardSearchCodeTracking"),
        ),
        TypeEntry("wizardSearchName", "Figtree400", bodyLg.size, ui.metric("wizardSearchNameLineHeight"), body),
        styleOf(
            "wizardLongTitle",
            "Fraunces400Opsz20",
            "wizardLongTitleFontSize",
            "wizardLongTitleLineHeight",
            -longTitleSize / 100,
        ),
        styleOf("wizardAirport", "Figtree600", "wizardAirportFontSize", "wizardAirportLineHeight"),
        styleOf("wizardAirportPlaceholder", "Figtree400", "wizardAirportFontSize", "wizardAirportLineHeight"),
        styleOf("wizardRow", "Figtree400", "wizardRowFontSize", "wizardRowLineHeight"),
        styleOf("wizardButton", "Figtree600", "wizardRowFontSize", "wizardRowLineHeight"),
        TypeEntry("wizardSubtitle", "Figtree400", bodyLg.size, ui.metric("wizardRowLineHeight"), body),
        styleOf("wizardChronotype", "Figtree600", "wizardChronotypeFontSize", "wizardChronotypeLineHeight"),
        styleOf("wizardFieldLabel", "Figtree400", "wizardRouteFontSize", "wizardRouteLineHeight"),
        styleOf("wizardHint", "Figtree400", "wizardHintFontSize", "wizardHintLineHeight"),
        styleOf("wizardChoice", "Figtree600", "wizardChoiceFontSize", "wizardChoiceLineHeight"),
        styleOf("wizardRoute", "Figtree600", "wizardRouteFontSize", "wizardRouteLineHeight"),
    )
}

private fun wizardPickerStyle(root: Path): TypeEntry {
    val ios = firstCapture("""\.ios \{([^}]+)\}""", root.design("theme.css"))
    if (!ios.contains("system-ui")) throw GenerationError("Wizard pill no longer uses the platform font")
    val tracking = firstCapture("""letter-spacing:\s*(-?[\d.]+)em""", ios).toDouble()
    val pill = firstCapture("""const PILL = '([^']+)'""", root.design("Wizard.dc.html"))
    val size = firstCapture("""font-size:\s*([\d.]+)px""", pill).toDouble()
    val line = firstCapture("""line-height:\s*([\d.]+)px""", pill).toDouble()
    return TypeEntry("wizardPicker", "System400", size, line, tracking * size)
}
