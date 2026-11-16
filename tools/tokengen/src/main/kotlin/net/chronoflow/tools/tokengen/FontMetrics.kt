package net.chronoflow.tools.tokengen

import java.nio.file.Path

/** The `TypeMetrics` part of iOS `Tools/generate_fonts.py` (`generate_metrics`). */
fun fontMetrics(root: Path): List<TypeEntry> {
    val groups =
        parseJson(root.design("ds/chronoflow/tokens.json"))
            .obj()
            .member("type")
            .obj()
            .member("groups")
            .arr()
    val styles =
        groups
            .flatMap {
                it.obj().member("styles").arr().map { style ->
                    style.obj()
                }
            }.associateBy { it.member("name").string() }
    val word = firstCapture("""word: '([^']+)'""", root.design("SplashScreen.dc.html"))

    fun entry(
        name: String,
        fontSize: String,
        lineHeight: Double?,
        letterSpacing: String?,
    ): TypeEntry {
        val size = fontSize.removeSuffix("px").toDouble()
        val tracking = (letterSpacing?.removeSuffix("em")?.toDouble() ?: 0.0) * size
        return TypeEntry(name, "", size, lineHeight, tracking)
    }

    fun fromToken(name: String): TypeEntry {
        val style = styles[name] ?: throw GenerationError("Missing type style: $name")
        val size =
            style
                .member("fontSize")
                .string()
                .removeSuffix("px")
                .toDouble()
        val line =
            style.member("lineHeight").primitive().let {
                if (it.isString) {
                    it.content.removeSuffix("px").toDouble()
                } else {
                    it.content.toDouble() *
                        size
                }
            }
        return entry(
            name,
            style.member("fontSize").string(),
            line.takeIf { it != 0.0 },
            style["letterSpacing"]?.string(),
        )
    }
    val splash =
        entry(
            "splash-wordmark",
            firstCapture("""font-size: ([0-9]+px)""", word),
            firstCapture("""line-height: ([0-9]+px)""", word).removeSuffix("px").toDouble(),
            firstCapture("""letter-spacing: (-?[0-9.]+em)""", word),
        )
    val bodyLg = styles["body-lg"] ?: throw GenerationError("Missing type style: body-lg")
    val button = entry("button", bodyLg.member("fontSize").string(), null, null)
    val order = listOf("splash-wordmark", "body", "section-title", "eyebrow", "hero", "body-lg", "button")
    val all =
        order.map { name ->
            when (name) {
                "splash-wordmark" -> splash
                "button" -> button
                else -> fromToken(name)
            }
        }
    return all.map { TypeEntry(camelCase(it.name), "", it.size, it.lineHeight, it.trackingPt) }
}
