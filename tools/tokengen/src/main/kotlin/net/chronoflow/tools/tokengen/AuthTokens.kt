package net.chronoflow.tools.tokengen

import kotlinx.serialization.json.JsonObject
import java.nio.file.Path

fun authTokens(root: Path): TokenSet {
    val base =
        authTokens(
            root.design("AuthSignIn.dc.html"),
            root.design("ds/chronoflow/components/bundle.js"),
            root.design("ds/chronoflow/components/bundle.css"),
            parseJson(root.design("ds/chronoflow/tokens.json")).obj(),
        )
    val done =
        root
            .design(
                "AuthTwoFactor.dc.html",
            ).substringAfter("value=\"{{c.isDoneIcon}}\"")
            .substringBefore("</sc-if>")
    val strokes = captures("""stroke-width="([0-9.]+)"""", done)
    val stroke = strokes.singleOrNull()?.toDoubleOrNull() ?: throw GenerationError("Missing auth heading check stroke")
    return TokenSet(base.metrics + Metric("headingCheckStroke", stroke), base.styles, base.lists)
}

internal fun authTokens(
    board: String,
    bundle: String,
    css: String,
    json: JsonObject,
): TokenSet {
    fun one(
        pattern: String,
        source: String,
    ): String {
        val values = captures(pattern, source)
        if (values.size != 1) throw GenerationError("Auth source must uniquely match: $pattern")
        return values[0]
    }

    fun style(pattern: String): Map<String, String> =
        one(pattern, board)
            .split(";")
            .mapNotNull { declaration ->
                val parts = declaration.split(":", limit = 2).filter { it.isNotEmpty() }.map { it.trim() }
                if (parts.size == 2) parts[0] to parts[1] else null
            }.toMap()

    fun value(
        text: String?,
        suffix: String = "px",
    ): Double {
        if (text == null || !text.endsWith(suffix)) throw GenerationError("Invalid auth metric: ${text ?: "missing"}")
        return text.dropLast(suffix.length).toDoubleOrNull() ?: throw GenerationError("Invalid auth metric: $text")
    }

    fun pixels(
        style: Map<String, String>,
        key: String,
    ): Double = value(style[key])

    fun padding(style: Map<String, String>): List<Double> {
        val text = style["padding"] ?: throw GenerationError("Missing auth padding")
        return text.split(" ").filter { it.isNotEmpty() }.map { if (it == "0") 0.0 else value(it) }
    }

    fun token(
        group: String,
        name: String,
    ): Double {
        val rows =
            json
                .member(group)
                .obj()
                .member("tokens")
                .arr()
        return value(
            rows
                .firstOrNull { it.obj()["name"]?.string() == name }
                ?.obj()
                ?.get("value")
                ?.string(),
        )
    }

    fun same(
        values: List<Double>,
        context: String,
    ): Double {
        val first = values.firstOrNull()
        if (first == null ||
            !values.all { it == first }
        ) {
            throw GenerationError("Auth sources disagree: $context $values")
        }
        return first
    }

    fun component(name: String): String = one("function $name\\((.*?)\\}(?=function |var )", bundle)

    fun utility(
        prefix: String,
        classes: String,
    ): Double =
        one("(?:^| )$prefix([0-9.]+)(?= |\$)", classes).toDoubleOrNull()
            ?: throw GenerationError("Invalid auth utility: $prefix")

    fun require(
        classes: String,
        required: List<String>,
    ) {
        if (!classes.split(" ").toSet().containsAll(required)) throw GenerationError("Auth component classes changed")
    }

    val shell = style("""<div class="cf-sky"[^>]*style="([^"]+)"""")
    val header = style("""<div style="([^"]*justify-content: space-between; height: [^"]+)"""")
    val back = style("""<a href="Main.dc.html"[^>]*style="([^"]+)"""")
    val brand = style("""<span style="(display: inline-flex;[^"]+)"""")
    val headingGroup = style("""<div style="([^"]*flex-direction: column;[^"]*margin-top:[^"]+)"""")
    val heading = style("""<h1 style="([^"]+)"""")
    val body = style("""<p style="([^"]+)">Sign in to see your trips and plans\.</p>""")
    val card = style("""<section aria-label="Sign in" style="([^"]+)"""")
    val banner = style("""<div role="alert" style="([^"]+)"""")
    val bannerIcon = style("""<svg[^>]*style="(flex-shrink: 0;[^"]+)"""")
    val footer = style("""<a href="GuestFlight.dc.html" style="([^"]+)"""")
    val links = style("""<div style="(display: flex; justify-content: space-between;[^"]+)"""")
    val forgot = style("""<a href="AuthForgot.dc.html"[^>]*style="([^"]+)"""")
    val signup = style("""<a href="AuthSignUp.dc.html"[^>]*style="([^"]+)"""")
    val divider = style("""<div aria-hidden="true" style="([^"]+)"""")
    val button = one("""className:c\("([^"]+)"""", component("ue"))
    val field = one("""className:"([^"]*focus-visible:border-ring[^"]+)"""", component("Da"))
    val fieldGroup = one("""className:c\("([^"]+)"""", component("Da"))
    val notice =
        one(
            """function \w+\(\{[^}]*\}\)\{[^}]*className:c\("(flex items-start gap-3 rounded-2xl border p-3 text-sm)"""",
            bundle,
        )
    val noticeIcon = one("""className:c\("(mt-[0-9.]+ size-[0-9.]+ shrink-0)"""", bundle)
    val buttonLarge = one("""lg:"([^"]+)""", one("""Ba=\{([^}]+)\}""", bundle))
    require(field, listOf("rounded-md", "border", "bg-transparent", "text-base", "focus-visible:ring-ring"))
    require(button, listOf("text-sm", "font-medium"))
    require(
        one("""className:"(flex items-center gap-1.5 text-sm text-destructive)"""", component("Da")),
        listOf("items-center"),
    )
    val spacing = token("spacing", "space-1")
    val shellPadding = padding(shell)
    val bannerPadding = padding(banner)
    val forgotPadding = padding(forgot)
    val signupPadding = padding(signup)
    if (shellPadding.size != 3 || bannerPadding.size != 2 || forgotPadding.size != 2 || signupPadding.size != 2) {
        throw GenerationError("Auth padding arity")
    }
    val border = value(one("""^([0-9.]+px) solid """, banner["border"] ?: ""))
    val cssBorder = value(one("""\.border\{[^}]*border-width:([0-9.]+px)""", css))
    val metrics =
        listOf(
            "headingBadge" to spacing * 12,
            "headingGlyph" to spacing * 6,
            "gutter" to same(listOf(shellPadding[0], shellPadding[1]), "shell gutters"),
            "bottom" to shellPadding[2],
            "touch" to
                same(
                    listOf(
                        pixels(header, "height"),
                        pixels(back, "height"),
                        pixels(back, "width"),
                        pixels(forgot, "min-height"),
                        pixels(signup, "min-height"),
                    ),
                    "touch",
                ),
            "backGlyph" to value(one("""<a href="Main.dc.html"[^>]*>\s*<svg[^>]*\swidth="([0-9.]+)"""", board), ""),
            "brandGap" to pixels(brand, "gap"),
            "moon" to value(one("""<span style="display: inline-flex;[^>]*><svg[^>]*\swidth="([0-9.]+)"""", board), ""),
            "headingMargin" to pixels(headingGroup, "margin-top"),
            "fieldGap" to
                same(
                    listOf(
                        pixels(headingGroup, "gap"),
                        pixels(links, "gap"),
                        utility("gap-", fieldGroup) * spacing,
                        utility("gap-", button) * spacing,
                        forgotPadding[1],
                        signupPadding[1],
                    ),
                    "field gap",
                ),
            "cardPadding" to pixels(card, "padding"),
            "cardRadius" to pixels(card, "border-radius"),
            "cardGap" to pixels(card, "gap"),
            "footerGap" to pixels(footer, "margin-top"),
            "footerHeight" to pixels(footer, "min-height"),
            "bannerGap" to pixels(banner, "gap"),
            "bannerVertical" to bannerPadding[0],
            "bannerHorizontal" to bannerPadding[1],
            "bannerRadius" to pixels(banner, "border-radius"),
            "bannerGlyph" to value(one("""<div role="alert"[^>]*><svg[^>]*\swidth="([0-9.]+)"""", board), ""),
            "bannerIconTop" to pixels(bannerIcon, "margin-top"),
            "icon" to
                same(
                    listOf(
                        value(
                            one(
                                """<x-import component-from-global-scope="ChronoFlow.Button"[^>]*>\s*<svg[^>]*\swidth="([0-9.]+)"""",
                                board,
                            ),
                            "",
                        ),
                        utility("size-", noticeIcon) * spacing,
                    ),
                    "icon",
                ),
            "spacer" to utility("px-", buttonLarge) * spacing,
            "inset" to
                same(
                    listOf(utility("px-", field) * spacing, utility("p-", notice) * spacing, pixels(divider, "gap")),
                    "inset",
                ),
            "fieldRadius" to token("radius", "radius-md"),
            "noticeRadius" to token("radius", "radius-2xl"),
            "noticeIconTop" to utility("mt-", noticeIcon) * spacing,
            "borderWidth" to same(listOf(border, cssBorder), "border"),
            "focusWidth" to utility("focus-visible:ring-", field),
        ).map { (name, number) -> Metric(name, number) }.toMutableList()
    metrics +=
        Metric(
            "disabledOpacity",
            same(
                listOf(utility("disabled:opacity-", button), utility("disabled:opacity-", field)),
                "disabled opacity",
            ) /
                100,
        )
    val shadows =
        json
            .member("shadow")
            .obj()
            .member("tokens")
            .arr()
            .first { it.obj()["name"]?.string() == "shadow-soft" }
            .obj()
            .member("value")
            .obj()
    for (appearance in listOf("light", "dark")) {
        val values = shadows.member(appearance).string().split(" ")
        metrics += Metric("${appearance}ShadowY", value(values[1]))
        metrics += Metric("${appearance}ShadowRadius", value(values[2]) / 2)
        metrics += Metric("${appearance}ShadowSpread", value(values[3]))
    }

    val type = json.member("type").obj()
    val families = type.member("families").obj()
    val styles =
        type.member("groups").arr().flatMap {
            it
                .obj()
                .member("styles")
                .arr()
                .map { style -> style.obj() }
        }
    val hintToken = styles.first { it["name"]?.string() == "body" }
    val hint =
        mapOf(
            "font-size" to hintToken.member("fontSize").string(),
            "line-height" to hintToken.member("lineHeight").string(),
            "font-weight" to hintToken.member("fontWeight").string(),
        )
    if (shell["font-family"] != "var(--font-sans)") throw GenerationError("Auth root font changed")
    val entries =
        listOf(
            AuthStyle("authBrand", brand, "display", 20),
            AuthStyle("authHeading", heading, "display", 32),
            AuthStyle("authBody", body, "sans", 0),
            AuthStyle("authLabel", forgot, "sans", 0),
            AuthStyle("authStrong", signup, "sans", 0),
            AuthStyle("authHint", hint, "sans", 0),
            AuthStyle("authMeta", divider, "sans", 0),
        ).map { (name, declaration, defaultFamily, optical) ->
            val family = declaration["font-family"] ?: "var(--font-$defaultFamily)"
            val weight = declaration["font-weight"] ?: one("""--font-weight-normal:([0-9]+)(?:;|\})""", css)
            val face =
                when {
                    family == "var(--font-display)" &&
                        families["display"]?.string()?.startsWith("'Fraunces',") == true &&
                        weight in listOf("400", "500") -> "Fraunces${weight}Opsz$optical"

                    family == "var(--font-sans)" &&
                        families["sans"]?.string()?.startsWith("'Figtree',") == true &&
                        weight in listOf("400", "500", "600", "700") -> "Figtree$weight"

                    else -> throw GenerationError("Unsupported auth typeface: $name")
                }
            val size = pixels(declaration, "font-size")
            val line = pixels(declaration, "line-height")
            val spacingValue = declaration["letter-spacing"]
            val tracking =
                if (spacingValue == null) {
                    0.0
                } else if (spacingValue.endsWith("em")) {
                    value(spacingValue, "em") * size
                } else {
                    value(spacingValue)
                }
            TypeEntry(name, face(face), size, line, tracking)
        }
    return TokenSet(metrics, entries)
}

private data class AuthStyle(
    val name: String,
    val declaration: Map<String, String>,
    val defaultFamily: String,
    val optical: Int,
)
