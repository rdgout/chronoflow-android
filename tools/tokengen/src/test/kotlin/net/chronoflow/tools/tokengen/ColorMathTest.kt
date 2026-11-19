package net.chronoflow.tools.tokengen

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SpecColorsTest(
    private val name: String,
    private val appearance: String,
) {
    @Test
    fun specColorsAndAliases() {
        val row = reportRows.getValue(name.let(::camelCase))
        val lab = sources.color(name, appearance)
        val (hex, p3) = if (appearance == "light") row.lightHex to row.lightP3 else row.darkHex to row.darkP3
        val alpha = if (lab.alpha < 1) " α${lab.alpha}" else ""
        assertEquals("$name $appearance", hex, lab.rgb(p3 = false).hex + alpha)
        val expected = p3Channels(p3)
        lab.rgb(p3 = true).values.zip(expected).forEach { (actual, golden) ->
            assertEquals("$name $appearance P3", golden, actual, 1e-9)
        }
    }

    companion object {
        private val reportRows by lazy { parseReport(iosGolden("TokenGenReport.md")).associateBy { it.name } }

        @JvmStatic
        @Parameterized.Parameters(name = "{0} {1}")
        fun cases(): List<Array<String>> =
            (DesignSource.names + DesignSource.authDerivedNames + DesignSource.wizardDerivedNames)
                .flatMap { name -> listOf(arrayOf(name, "light"), arrayOf(name, "dark")) }
    }
}

class ColorMathTest {
    @Test
    fun derivedColorsMatchTheIOSReport() {
        val expected =
            mapOf(
                "auth-error-border" to ("light" to 0.35),
                "settings-warning-border" to ("light" to 0.4),
                "wizard-notice-border" to ("light" to 0.4),
            )
        for ((name, pair) in expected) {
            assertEquals(name, pair.second, sources.color(name, pair.first).alpha, 0.0)
        }
        val shadowLight = sources.color("auth-shadow", "light")
        val shadowDark = sources.color("auth-shadow", "dark")
        assertEquals("#202955", shadowLight.rgb(p3 = false).hex)
        assertEquals(0.25, shadowLight.alpha, 0.0)
        assertEquals("#000000", shadowDark.rgb(p3 = false).hex)
        assertEquals(0.6, shadowDark.alpha, 0.0)
        assertEquals(0.4, sources.color("settings-warning-border", "dark").alpha, 0.0)
        assertEquals(0.4, sources.color("wizard-notice-border", "dark").alpha, 0.0)
        assertEquals(0.35, sources.color("auth-error-border", "dark").alpha, 0.0)
    }

    @Test
    fun systemSelectedIsTheOKLabMix() {
        assertEquals("#FAECE3", sources.color("system-selected", "light").rgb(p3 = false).hex)
        assertEquals("#302D38", sources.color("system-selected", "dark").rgb(p3 = false).hex)
    }

    @Test
    fun roundingIsHalfUp() {
        assertEquals(3L, roundHalfUp(2.5))
        assertEquals(1L, roundHalfUp(0.5))
        assertEquals(127L, roundHalfUp(126.5))
        assertEquals("#80", RGB(listOf(0.5)).hex)
    }

    @Test
    fun negativeLinearChannelsAreClippedBeforeTheTransferCurve() {
        val travel = sources.color("cat-travel", "light").rgb(p3 = false)
        assertEquals(0.0, travel.values[0], 0.0)
    }
}
