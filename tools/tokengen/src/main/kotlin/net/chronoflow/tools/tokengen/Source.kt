package net.chronoflow.tools.tokengen

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

const val TOKENS_PATH = "_design/canvas/ds/chronoflow/tokens.json"
const val THEME_PATH = "_design/canvas/theme.css"

fun Path.readText(): String = Files.readString(this)

fun parseJson(text: String): JsonElement = Json.parseToJsonElement(text)

fun JsonElement.obj(): JsonObject = this as? JsonObject ?: throw GenerationError("Expected a JSON object")

fun JsonElement.arr(): JsonArray = this as? JsonArray ?: throw GenerationError("Expected a JSON array")

fun JsonElement.primitive(): JsonPrimitive =
    this as? JsonPrimitive ?: throw GenerationError("Expected a JSON primitive")

fun JsonElement.string(): String = primitive().content

fun JsonElement.double(): Double =
    primitive().content.toDoubleOrNull() ?: throw GenerationError("Expected a number: $this")

fun JsonObject.member(key: String): JsonElement = this[key] ?: throw GenerationError("Missing JSON key: $key")

/** Group 1 of every match, as `captures` in iOS `Source.swift`. */
fun captures(
    pattern: String,
    text: String,
): List<String> = Regex(pattern).findAll(text).mapNotNull { it.groups[1]?.value }.toList()

/** The first capture, or a `GenerationError` naming the pattern. */
fun firstCapture(
    pattern: String,
    text: String,
): String = captures(pattern, text).firstOrNull() ?: throw GenerationError("No match for $pattern")

fun number(value: Double): String = String.format(Locale.ROOT, "%.10f", value)

/** Foundation `capitalized`: lower-case the part, then upper-case its first letter, even after digits. */
fun camelCase(name: String): String {
    val parts = name.split("-").filter { it.isNotEmpty() }
    return parts[0] +
        parts.drop(1).joinToString("") { part ->
            val lower = part.lowercase()
            val index = lower.indexOfFirst { it.isLetter() }
            if (index <
                0
            ) {
                lower
            } else {
                lower.substring(0, index) + lower[index].uppercaseChar() + lower.substring(index + 1)
            }
        }
}

/** A Kotlin `Float` literal with at most ten decimals, locale independent. */
fun floatLiteral(value: Double): String = plain(value) + "f"

/** A Kotlin `Double` literal with at most ten decimals, locale independent. */
fun doubleLiteral(value: Double): String = plain(value).let { if ('.' in it) it else "$it.0" }

private fun plain(value: Double): String =
    BigDecimal(value).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

class DesignSource(
    val root: Path,
) {
    val tokens: Map<String, JsonElement>
    val theme: String

    init {
        val json = parseJson(root.resolve(TOKENS_PATH).readText()).obj()
        tokens =
            json
                .member("color")
                .obj()
                .member("tokens")
                .arr()
                .associate { it.obj().member("name").string() to it.obj().member("value") }
        theme = root.resolve(THEME_PATH).readText()
        for (appearance in listOf("light", "dark")) {
            val blockPattern =
                if (appearance == "light") {
                    """:root, \[data-theme="light"\] \{([^}]+)\}"""
                } else {
                    """\[data-theme="dark"\] \{([^}]+)\}"""
                }
            val block =
                captures(blockPattern, theme).firstOrNull()
                    ?: throw GenerationError("Missing theme.css block: $appearance")
            val values =
                Regex("""--([a-z0-9-]+):\s*([^;]+);""").findAll(block).associate {
                    it.groupValues[1] to
                        it.groupValues[2]
                }

            fun resolveCss(
                name: String,
                seen: Set<String> = emptySet(),
            ): String {
                val value = values[name]
                if (name in seen || value == null) throw GenerationError("Missing or cyclic CSS token: $name")
                return if (value.startsWith("var(--")) resolveCss(value.drop(6).dropLast(1), seen + name) else value
            }
            for (name in names) {
                if (resolveCss(name) != resolved(name, appearance, emptySet())) {
                    throw GenerationError("tokens.json / theme.css disagree: $name $appearance")
                }
            }
        }
    }

    private fun board(file: String): String = root.resolve("_design/canvas/$file").readText()

    fun color(
        name: String,
        appearance: String,
    ): OKLab {
        when (name) {
            "settings-warning-background" -> {
                return color("card", appearance).mixed(color("dawn", appearance), 0.1)
            }

            "settings-warning-border" -> {
                return color("dawn", appearance).withAlpha(0.4)
            }

            "settings-qr-background" -> {
                return OKLab.fromCss(firstCapture("""background: (oklch\(1 0 0\))""", board("Set2FASetup.dc.html")))
            }

            "system-selected" -> {
                return color("card", appearance).mixed(color("dawn", appearance), 0.16)
            }

            "auth-error-background" -> {
                return color("card", appearance).mixed(color("destructive", appearance), 0.1)
            }

            "auth-error-border" -> {
                return color("destructive", appearance).withAlpha(0.35)
            }

            "auth-shadow" -> {
                val shadow =
                    parseJson(root.resolve(TOKENS_PATH).readText())
                        .obj()
                        .member("shadow")
                        .obj()
                        .member("tokens")
                        .arr()[0]
                        .obj()
                        .member("value")
                        .obj()
                        .member(appearance)
                        .string()
                return OKLab.fromCss(firstCapture("""(oklch\([^)]+\))""", shadow))
            }
        }
        if (name.startsWith("wizard-notice-")) {
            val pattern =
                if (name == "wizard-notice-background") {
                    """background: color-mix\(in oklab, var\(--dawn\) ([\d.]+)%, var\(--card\)\)"""
                } else {
                    """border: 1px solid color-mix\(in oklab, var\(--dawn\) ([\d.]+)%, transparent\)"""
                }
            val fraction = firstCapture(pattern, board("Wizard.dc.html")).toDouble() / 100
            val dawn = color("dawn", appearance)
            return if (name ==
                "wizard-notice-background"
            ) {
                color("card", appearance).mixed(dawn, fraction)
            } else {
                dawn.withAlpha(fraction)
            }
        }
        return OKLab.fromCss(resolved(name, appearance, emptySet()))
    }

    /** `rgba(r, g, b, a)` of the wizard picker pill; the same in both appearances. */
    fun wizardPickerFill(): List<Double> {
        val text = firstCapture("""const PILL = '[^']*background: rgba\(([\d., ]+)\)""", board("Wizard.dc.html"))
        return text.split(",").map { it.trim().toDouble() }
    }

    private fun resolved(
        name: String,
        appearance: String,
        visited: Set<String>,
    ): String {
        val token = tokens[name]
        val value =
            when {
                name in visited || token == null -> null
                token is JsonObject -> token[appearance]?.string()
                else -> token.string()
            } ?: throw GenerationError("Missing or cyclic token: $name, $appearance")
        return if (value.startsWith("{")) resolved(value.drop(1).dropLast(1), appearance, visited + name) else value
    }

    fun midStop(appearance: String): Double {
        val values = captures("""--sky-mid-stop:\s*([\d.]+)%;""", theme)
        val expected = if (appearance == "light") 55.0 else 60.0
        if (values.size != 2 || values[if (appearance == "light") 0 else 1].toDoubleOrNull() != expected) {
            throw GenerationError("theme.css sky-mid-stop differs from plan: expected 55% light / 60% dark")
        }
        return expected / 100
    }

    fun ramp(appearance: String): List<RampStop> =
        skyRamp(
            color("sky-top", appearance),
            color("sky-mid", appearance),
            color("sky-horizon", appearance),
            midStop(appearance),
        )

    companion object {
        val names =
            listOf(
                "sky-top",
                "sky-mid",
                "sky-horizon",
                "sky-title",
                "sky-copy",
                "sky-copy-background",
                "dawn",
                "dawn-foreground",
                "background",
                "foreground",
                "card",
                "card-foreground",
                "popover",
                "popover-foreground",
                "destructive-foreground",
                "input",
                "border",
                "ring",
                "surface-2",
                "muted",
                "secondary",
                "secondary-foreground",
                "now",
                "cat-sleep",
                "cat-sleep-fg",
                "cat-light",
                "cat-light-fg",
                "cat-moderate",
                "cat-moderate-fg",
                "cat-dark",
                "cat-dark-fg",
                "cat-caffeine",
                "cat-caffeine-fg",
                "cat-travel",
                "cat-travel-fg",
                "muted-foreground",
                "primary",
                "primary-foreground",
                "accent",
                "accent-foreground",
                "destructive",
                "dawn-text",
            )
        val authDerivedNames =
            listOf(
                "auth-error-background",
                "auth-error-border",
                "auth-shadow",
                "settings-warning-background",
                "settings-warning-border",
                "settings-qr-background",
            )
        val wizardDerivedNames = listOf("wizard-notice-background", "wizard-notice-border")
        val androidDerivedNames = listOf("system-selected", "wizard-picker-fill")
    }
}
