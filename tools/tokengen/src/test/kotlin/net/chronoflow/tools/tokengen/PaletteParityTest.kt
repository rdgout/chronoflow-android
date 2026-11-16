package net.chronoflow.tools.tokengen

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

class PaletteParityTest {
    private val outputs by lazy { tokenOutputs(repoRoot).mapValues { String(it.value) } }

    private fun colors(
        text: String,
        pattern: Regex,
    ): Map<String, String> = pattern.findAll(text).associate { it.groupValues[1] to it.groupValues[2] }

    @Test
    fun cfColorsAndTimelinePaletteAgreeForAllNamesAndThemes() {
        val compose = outputs.getValue(COLORS_PATH)
        val plain = outputs.getValue(PALETTE_PATH)
        val composeRegex = Regex("""(\w+) = Color\(0x([0-9A-F]{8})\),""")
        val plainRegex = Regex("""(\w+) = 0x([0-9A-F]{8})\.toInt\(\),""")
        for ((theme, marker, plainMarker) in listOf(
            Triple("light", "val CfLightColors", "val Light"),
            Triple("dark", "val CfDarkColors", "val Dark"),
        )) {
            val composeBlock =
                if (theme ==
                    "light"
                ) {
                    compose.substringAfter(marker).substringBefore("val CfDarkColors")
                } else {
                    compose.substringAfter(marker)
                }
            val plainBlock =
                if (theme ==
                    "light"
                ) {
                    plain.substringAfter(plainMarker).substringBefore("val Dark")
                } else {
                    plain.substringAfter(plainMarker)
                }
            val fromCompose = colors(composeBlock, composeRegex)
            val fromPlain = colors(plainBlock, plainRegex)
            assertEquals(theme, 52, fromCompose.size)
            assertEquals(theme, fromCompose, fromPlain)
        }
    }

    @Test
    fun launchSkyIsTheSkyTop() {
        assertEquals(
            true,
            outputs.getValue(LAUNCH_LIGHT_PATH).contains("""<color name="launch_sky_top">#FF1F285A</color>"""),
        )
        assertEquals(
            true,
            outputs.getValue(LAUNCH_DARK_PATH).contains("""<color name="launch_sky_top">#FF06091C</color>"""),
        )
    }

    @Test
    fun alphaBytesFollowRoundHalfUp() {
        val rows = colorRows(sources).associateBy { it.name }
        assertEquals("0xE6F3F7FC", rows.getValue("sky-copy-background").light.argbLiteral)
        assertEquals("0x59", "0x" + String.format("%02X", roundHalfUp(0.35 * 255)))
        assertEquals(true, abs(rows.getValue("wizard-picker-fill").light.alpha - 0.12) < 1e-12)
    }
}
