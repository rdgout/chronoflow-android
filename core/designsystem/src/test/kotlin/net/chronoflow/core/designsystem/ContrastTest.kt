package net.chronoflow.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import net.chronoflow.core.designsystem.generated.CfColors
import net.chronoflow.core.designsystem.generated.CfDarkColors
import net.chronoflow.core.designsystem.generated.CfLightColors
import net.chronoflow.core.designsystem.theme.toM3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class ContrastTest {
    @Test fun textAndControlTokenPairsPassAAInBothAppearances() {
        themes.forEach { c ->
            with(c) {
                listOf(background, card, popover).forEach { surface ->
                    passes(foreground, surface)
                    passes(mutedForeground, surface)
                    passes(dawnText, surface)
                }
                passes(cardForeground, card)
                passes(cardForeground, authErrorBackground)
                passes(mutedForeground, muted)
                passes(dawnText, systemSelected)
                passes(dawnForeground, dawn)
                passes(destructive, card)
                passes(destructive, popover)
                passes(destructiveForeground, destructive)
                passes(secondaryForeground, secondary)
                passes(ring, background, 3.0)
                val m = toM3()
                listOf(
                    m.onPrimary to m.primary,
                    m.onSecondary to m.secondary,
                    m.onTertiary to m.tertiary,
                    m.onPrimaryContainer to m.primaryContainer,
                    m.onSecondaryContainer to m.secondaryContainer,
                    m.onTertiaryContainer to m.tertiaryContainer,
                    m.onSurface to m.surface,
                    m.onSurfaceVariant to m.surfaceVariant,
                    m.onBackground to m.background,
                    m.onError to m.error,
                    m.onErrorContainer to m.errorContainer,
                    m.inverseOnSurface to m.inverseSurface,
                    m.onPrimaryFixed to m.primaryFixed,
                    m.onPrimaryFixedVariant to m.primaryFixedDim,
                    m.onSecondaryFixed to m.secondaryFixed,
                    m.onSecondaryFixedVariant to m.secondaryFixedDim,
                    m.onTertiaryFixed to m.tertiaryFixed,
                    m.onTertiaryFixedVariant to m.tertiaryFixedDim,
                ).forEach { (fg, bg) -> passes(fg, bg) }
            }
        }
    }

    @Test fun subtitleAndBannerTokenPairsPassAAInBothAppearances() {
        themes.forEach { c ->
            passes(c.cardForeground, c.card)
            passes(c.cardForeground, c.authErrorBackground)
            for (i in 0..1000) {
                val t = i / 1000f
                val bg =
                    Color(
                        c.skyTop.red + (c.skyMid.red - c.skyTop.red) * t,
                        c.skyTop.green + (c.skyMid.green - c.skyTop.green) * t,
                        c.skyTop.blue + (c.skyMid.blue - c.skyTop.blue) * t,
                    )
                passes(c.skyTitle, bg)
            }
        }
    }

    @Test fun createAccountTextContrastAdaptationKeepsDawnTextOnBothCards() {
        assertEquals(2.5523, contrast(CfLightColors.dawn, CfLightColors.card), 0.001)
        assertEquals(5.4592, contrast(CfLightColors.dawnText, CfLightColors.card), 0.001)
        assertEquals(9.2975, contrast(CfDarkColors.dawn, CfDarkColors.card), 0.001)
        assertEquals(9.2975, contrast(CfDarkColors.dawnText, CfDarkColors.card), 0.001)
        themes.forEach { passes(it.dawnText, it.card) }
        assertTrue(contrast(CfLightColors.dawn, CfLightColors.card) < 4.5)
    }

    @Test fun knownBoardGapsStillNeedTheirDesignDecisions() {
        themes.forEach { c ->
            listOf(
                c.input to c.background,
                c.border to c.background,
                c.border to c.card,
                c.input to c.card,
            ).forEach { (fg, bg) ->
                assertTrue("Accepted decorative border gap changed", contrast(fg, bg) < 3.0)
            }
        }
        with(CfLightColors) {
            listOf(card, background, muted, popover).forEach { assertTrue("OQ-2-7 changed", contrast(dawn, it) < 3.0) }
        }
        with(CfDarkColors) { assertTrue("OQ-2-5 changed", contrast(dawn, foreground) < 4.5) }
    }

    private fun passes(
        fg: Color,
        bg: Color,
        minimum: Double = 4.5,
    ) {
        assertTrue("contrast ${contrast(fg, bg)} < $minimum for $fg on $bg", contrast(fg, bg) >= minimum)
    }

    private fun contrast(
        fg: Color,
        bg: Color,
    ): Double {
        val a = luminance(if (fg.alpha < 1f) fg.compositeOver(bg) else fg)
        val b = luminance(bg)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    private fun luminance(c: Color): Double {
        fun linear(v: Float): Double =
            v.toDouble().let {
                if (it <=
                    0.04045
                ) {
                    it / 12.92
                } else {
                    ((it + 0.055) / 1.055).pow(2.4)
                }
            }
        return 0.2126 * linear(c.red) + 0.7152 * linear(c.green) + 0.0722 * linear(c.blue)
    }

    private val themes: List<CfColors> = listOf(CfLightColors, CfDarkColors)
}
