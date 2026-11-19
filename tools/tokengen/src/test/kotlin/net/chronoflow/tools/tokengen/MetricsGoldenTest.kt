package net.chronoflow.tools.tokengen

import org.junit.Assert.assertEquals
import org.junit.Test

class MetricsGoldenTest {
    private val ui by lazy { planUITokens(repoRoot) }

    private fun iosMetrics(file: String): Map<String, Double> =
        Regex("""static var (\w+): (?:CGFloat|Double) \{ (-?[\d.]+) \}""")
            .findAll(joined(iosGolden(file)))
            .associate { it.groupValues[1] to it.groupValues[2].toDouble() }

    private fun compare(
        file: String,
        expectedCount: Int,
        model: List<Metric>,
        extras: Set<String> = emptySet(),
    ) {
        val ios = iosMetrics(file)
        assertEquals(file, expectedCount, ios.size)
        val ours = model.associate { it.name to it.value }
        assertEquals("duplicate names in $file", model.size, ours.size)
        for ((name, value) in ios) {
            assertEquals("$file $name", value, ours[name] ?: error("missing $name in $file"), 1e-9)
        }
        assertEquals("$file extras", extras, ours.keys - ios.keys)
    }

    @Test
    fun planUITokens() = compare("PlanUITokens.generated.swift", 115, ui.metrics)

    @Test
    fun authTokens() = compare("AuthTokens.generated.swift", 36, authTokens(repoRoot).metrics)

    @Test
    fun settingsTokens() {
        val settings = settingsTokens(repoRoot, ui)
        compare("SettingsTokens.generated.swift", 21, settings.metrics)
        assertEquals(listOf(150.0, 190.0, 130.0), settings.lists.single { it.first == "skeletonRowWidths" }.second)
    }

    @Test
    fun termsTokens() = compare("TermsTokens.generated.swift", 8, termsTokens(repoRoot, ui).metrics)

    @Test
    fun welcomeTokens() = compare("WelcomeTokens.generated.swift", 15, welcomeTokens(repoRoot).metrics)

    @Test
    fun planMetrics() {
        val plan = planTokens(repoRoot)
        compare("PlanMetrics.generated.swift", 99, plan.metrics)
        val emitted = String(tokenOutputs(repoRoot).getValue(PLAN_METRICS_PATH))
        assertEquals(true, emitted.contains("const val pointsPerMinute: Double = pointsPerHour / 60"))
        assertEquals(40.0 / 60, plan.metrics.first { it.name == "pointsPerHour" }.value / 60, 0.0)
    }

    @Test
    fun welcomeMetricsComeFromTheBoard() {
        val welcome = welcomeTokens(repoRoot)
        for ((name, value) in mapOf(
            "topPadding" to 64.0,
            "moonSize" to 28.0,
            "cardRadius" to 22.0,
            "buttonHeight" to 52.0,
        )) {
            assertEquals(name, value, welcome.metric(name), 0.0)
        }
        val emitted = String(tokenOutputs(repoRoot).getValue(DIMENS_PATH))
        for (line in listOf(
            "const val topPadding: Float = 64f",
            "const val moonSize: Float = 28f",
            "const val cardRadius: Float = 22f",
            "const val buttonHeight: Float = 52f",
        )) {
            assertEquals(line, true, emitted.contains(line))
        }
    }
}
