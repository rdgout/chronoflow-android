package net.chronoflow.tools.tokengen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TypeScaleGoldenTest {
    private val ui by lazy { planUITokens(repoRoot) }
    private val auth by lazy { authTokens(repoRoot) }
    private val settings by lazy { settingsTokens(repoRoot, ui) }
    private val terms by lazy { termsTokens(repoRoot, ui) }
    private val all by lazy { (ui.styles + auth.styles + settings.styles + terms.styles).associateBy { it.name } }

    @Test
    fun everyLiteralStyleMatchesIOS() {
        val pattern =
            Regex(
                """static var (\w+): Style \{ Style\(\s*face: \.(\w+), size: (-?[\d.]+), lineHeight: (-?[\d.]+), tracking: (-?[\d.]+)""",
            )
        var matched = 0
        for (file in listOf("PlanUITokens", "AuthTokens", "SettingsTokens", "TermsTokens")) {
            for (match in pattern.findAll(joined(iosGolden("$file.generated.swift")))) {
                val (name, face, size, line, tracking) = match.destructured
                val mine = all[name] ?: error("missing style $name")
                assertEquals(name, face.replaceFirstChar { it.uppercase() }, mine.face)
                assertEquals(name, size.toDouble(), mine.size, 1e-9)
                assertEquals(name, line.toDouble(), mine.lineHeight!!, 1e-9)
                assertEquals(name, tracking.toDouble(), mine.trackingPt, 1e-9)
                matched++
            }
        }
        assertEquals(24, matched)
    }

    @Test
    fun compositeStylesMatchTheResolvedTable() {
        val table =
            listOf(
                listOf("walkthroughBody", "Figtree500", 16.0, 24.0, 0.0),
                listOf("wizardReadySummary", "Fraunces400Opsz20", 20.0, 27.0, 0.0),
                listOf("wizardFailureTitle", "Fraunces400Opsz20", 22.0, 28.0, 0.0),
                listOf("wizardFailureBody", "Figtree400", 15.0, 22.0, 0.0),
                listOf("wizardSecondary", "Figtree500", 16.0, 22.0, 0.0),
                listOf("wizardArc", "Fraunces400Opsz32", 26.0, 32.0, 0.0),
                listOf("wizardNavTitle", "Figtree500", 15.0, 20.0, 0.0),
                listOf("wizardSearchCode", "Figtree700", 15.0, 20.0, 0.6),
                listOf("wizardSearchName", "Figtree400", 16.0, 21.0, 0.0),
                listOf("wizardLongTitle", "Fraunces400Opsz20", 24.0, 30.0, -0.24),
                listOf("wizardAirport", "Figtree600", 20.0, 26.0, 0.0),
                listOf("wizardAirportPlaceholder", "Figtree400", 20.0, 26.0, 0.0),
                listOf("wizardRow", "Figtree400", 17.0, 22.0, 0.0),
                listOf("wizardButton", "Figtree600", 17.0, 22.0, 0.0),
                listOf("wizardSubtitle", "Figtree400", 16.0, 22.0, 0.0),
                listOf("wizardChronotype", "Figtree600", 16.0, 22.0, 0.0),
                listOf("wizardFieldLabel", "Figtree400", 15.0, 20.0, 0.0),
                listOf("wizardHint", "Figtree400", 13.0, 18.0, 0.0),
                listOf("wizardChoice", "Figtree600", 18.0, 24.0, 0.0),
                listOf("wizardRoute", "Figtree600", 15.0, 20.0, 0.0),
                listOf("settingsRow", "Figtree500", 16.0, 24.0, 0.0),
                listOf("settingsStrong", "Figtree600", 16.0, 24.0, 0.0),
                listOf("settingsPill", "Figtree500", 14.0, 20.0, 0.0),
                listOf("settingsBadge", "Figtree500", 12.0, 16.0, 0.0),
                listOf("termsTitle", "Fraunces400Opsz32", 32.0, 38.0, -0.3),
                listOf("termsBody", "Figtree400", 15.0, 22.0, 0.0),
                listOf("termsStrong", "Figtree600", 15.0, 22.0, 0.0),
            )
        for (row in table) {
            val style = all[row[0] as String] ?: error("missing ${row[0]}")
            assertEquals(row[0] as String, row[1], style.face)
            assertEquals(row[0] as String, row[2] as Double, style.size, 1e-9)
            assertEquals(row[0] as String, row[3] as Double, style.lineHeight!!, 1e-9)
            assertEquals(row[0] as String, row[4] as Double, style.trackingPt, 1e-9)
        }
        assertEquals(28.0, ui.style("screenDayTitle").lineHeight!!, 0.0)
        assertEquals(13.75, ui.style("screenBlockLabel").lineHeight!!, 1e-9)
    }

    @Test
    fun planTypeMetricsMatchIOS() {
        val golden = joined(iosGolden("PlanTypeMetrics.generated.swift"))
        val metrics =
            Regex(
                """static var (\w+): TypeMetrics \{ TypeMetrics\(size: ([\d.]+), lineHeight: ([\d.]+), tracking: 0\) \}""",
            ).findAll(golden)
                .associate {
                    it.groupValues[1] to
                        listOf(it.groupValues[2].toDouble(), it.groupValues[3].toDouble())
                }
        val faces =
            Regex("""static var (\w+)Face: FontFace \{ \.(\w+) \}""").findAll(golden).associate {
                it.groupValues[1] to
                    it.groupValues[2]
            }
        val plan = planTokens(repoRoot).types
        assertEquals(metrics.keys, plan.map { it.name }.toSet())
        for (entry in plan) {
            assertEquals(entry.name, metrics.getValue(entry.name)[0], entry.size, 1e-9)
            assertEquals(entry.name, metrics.getValue(entry.name)[1], entry.lineHeight!!, 1e-9)
            assertEquals(entry.name, faces.getValue(entry.name).replaceFirstChar { it.uppercase() }, entry.face)
        }
        assertEquals("Figtree400", plan.first { it.name == "planCardTitle" }.face)
    }

    @Test
    fun typeMetricsMatchIOS() {
        val golden = joined(iosGolden("TypeMetrics.generated.swift"))
        val pattern =
            Regex(
                """static var (\w+): TypeMetrics \{ TypeMetrics\(size: (-?[\d.]+), lineHeight: (nil|-?[\d.]+), tracking: (-?[\d.]+)\) \}""",
            )
        val ios = pattern.findAll(golden).toList()
        val mine = fontMetrics(repoRoot)
        assertEquals(7, ios.size)
        assertEquals(ios.map { it.groupValues[1] }, mine.map { it.name })
        for ((index, match) in ios.withIndex()) {
            assertEquals(match.groupValues[2].toDouble(), mine[index].size, 1e-9)
            assertEquals(match.groupValues[4].toDouble(), mine[index].trackingPt, 1e-9)
            val line = match.groupValues[3]
            if (line ==
                "nil"
            ) {
                assertNull(mine[index].lineHeight)
            } else {
                assertEquals(line.toDouble(), mine[index].lineHeight!!, 1e-9)
            }
        }
        assertNull(mine.single { it.name == "button" }.lineHeight)
    }
}
