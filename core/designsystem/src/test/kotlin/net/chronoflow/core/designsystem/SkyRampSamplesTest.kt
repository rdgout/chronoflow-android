package net.chronoflow.core.designsystem

import androidx.compose.ui.graphics.toArgb
import net.chronoflow.core.designsystem.effect.CfSky
import org.junit.Assert.assertTrue
import org.junit.Test

class SkyRampSamplesTest {
    @Test fun rampMatchesTheIosEncodedSrgbSamples() {
        val samples =
            listOf(
                "1F285A 272D61 303167 38366E 403B75 493F7C 5D4C7F 7E607E A1737A C58572 EB9666",
                "06091C 0A0C22 0F0F27 13112D 181434 1D173A 221A40 402B42 623B41 854A3C AA5830",
            )
        samples.forEachIndexed { theme, colors ->
            colors.split(' ').forEachIndexed { i, hex ->
                val actual = CfSky.colorAt(theme == 1, i / 10f).toArgb()
                val expected = hex.toInt(16)
                listOf(16, 8, 0).forEach { shift ->
                    assertTrue(
                        "$hex at $i",
                        kotlin.math.abs((actual shr shift and 255) - (expected shr shift and 255)) <= 1,
                    )
                }
            }
        }
    }
}
