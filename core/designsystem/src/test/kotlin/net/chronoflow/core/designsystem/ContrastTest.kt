package net.chronoflow.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import net.chronoflow.core.designsystem.effect.CfSky
import net.chronoflow.core.designsystem.generated.CfColors
import net.chronoflow.core.designsystem.generated.CfDarkColors
import net.chronoflow.core.designsystem.generated.CfLightColors
import net.chronoflow.core.designsystem.theme.toM3
import net.chronoflow.core.plan.drawing.TimelineColors
import net.chronoflow.core.plan.generated.PlanMetrics
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

    @Test fun everyBuiltScreenRoleHasItsRecordedContrastContract() {
        themes.forEach { c ->
            val optional = PlanMetrics.optionalOpacity.toFloat()
            val marked = PlanMetrics.markedOpacity.toFloat()

            fun tile(
                color: Color,
                opacity: Float,
            ): Color = Color(TimelineColors.precomposite(color.toArgb(), opacity.toDouble(), c.surface2.toArgb()))

            fun marked(color: Color): Color =
                color.copy(alpha = PlanMetrics.markedRowOpacity.toFloat()).compositeOver(c.background)
            val warning = c.dawn.copy(alpha = PlanMetrics.warningOpacity.toFloat()).compositeOver(c.card)
            val pairs =
                listOf(
                    RolePair("foreground/background", c.foreground, c.background, 4.5),
                    RolePair("mutedForeground/background", c.mutedForeground, c.background, 4.5),
                    RolePair("foreground/card", c.foreground, c.card, 4.5),
                    RolePair("mutedForeground/card", c.mutedForeground, c.card, 4.5),
                    RolePair("foreground/popover", c.foreground, c.popover, 4.5),
                    RolePair("mutedForeground/popover", c.mutedForeground, c.popover, 4.5),
                    RolePair("foreground/surface2", c.foreground, c.surface2, 4.5),
                    RolePair("mutedForeground/surface2", c.mutedForeground, c.surface2, 4.5),
                    RolePair("foreground/muted", c.foreground, c.muted, 4.5),
                    RolePair("mutedForeground/muted", c.mutedForeground, c.muted, 4.5),
                    RolePair("foreground/systemSelected", c.foreground, c.systemSelected, 4.5),
                    RolePair(
                        "mutedForeground/systemSelected",
                        c.mutedForeground,
                        c.systemSelected,
                        4.5,
                    ),
                    RolePair(
                        "foreground/settingsWarningBackground",
                        c.foreground,
                        c.settingsWarningBackground,
                        4.5,
                    ),
                    RolePair(
                        "mutedForeground/settingsWarningBackground",
                        c.mutedForeground,
                        c.settingsWarningBackground,
                        4.5,
                    ),
                    RolePair(
                        "foreground/wizardNoticeBackground",
                        c.foreground,
                        c.wizardNoticeBackground,
                        4.5,
                    ),
                    RolePair(
                        "mutedForeground/wizardNoticeBackground",
                        c.mutedForeground,
                        c.wizardNoticeBackground,
                        4.5,
                    ),
                    RolePair("cardForeground/card", c.cardForeground, c.card, 4.5),
                    RolePair(
                        "cardForeground/authErrorBackground",
                        c.cardForeground,
                        c.authErrorBackground,
                        4.5,
                    ),
                    RolePair(
                        "foreground/authErrorBackground",
                        c.foreground,
                        c.authErrorBackground,
                        4.5,
                    ),
                    RolePair(
                        "cardForeground/settingsWarningBackground",
                        c.cardForeground,
                        c.settingsWarningBackground,
                        4.5,
                    ),
                    RolePair(
                        "cardForeground/wizardNoticeBackground",
                        c.cardForeground,
                        c.wizardNoticeBackground,
                        4.5,
                    ),
                    RolePair("popoverForeground/popover", c.popoverForeground, c.popover, 4.5),
                    RolePair("dawnForeground/dawn", c.dawnForeground, c.dawn, 4.5),
                    RolePair("primaryForeground/primary", c.primaryForeground, c.primary, 4.5),
                    RolePair("secondaryForeground/secondary", c.secondaryForeground, c.secondary, 4.5),
                    RolePair("accentForeground/accent", c.accentForeground, c.accent, 4.5),
                    RolePair(
                        "destructiveForeground/destructive",
                        c.destructiveForeground,
                        c.destructive,
                        4.5,
                    ),
                    RolePair("background/foreground", c.background, c.foreground, 4.5),
                    RolePair("primaryForeground/now", c.primaryForeground, c.now, 4.5),
                    RolePair("dawnText/background", c.dawnText, c.background, 4.5),
                    RolePair("dawnText/card", c.dawnText, c.card, 4.5),
                    RolePair("dawnText/popover", c.dawnText, c.popover, 4.5),
                    RolePair("dawnText/muted", c.dawnText, c.muted, 4.5),
                    RolePair("dawnText/systemSelected", c.dawnText, c.systemSelected, 4.5),
                    RolePair("destructive/background", c.destructive, c.background, 4.5),
                    RolePair("destructive/card", c.destructive, c.card, 4.5),
                    RolePair("destructive/popover", c.destructive, c.popover, 4.5),
                    RolePair(
                        "destructive/authErrorBackground",
                        c.destructive,
                        c.authErrorBackground,
                        4.5,
                    ),
                    RolePair("ring/background", c.ring, c.background, 3.0),
                    RolePair("ring/card", c.ring, c.card, 3.0),
                    RolePair("ring/popover", c.ring, c.popover, 3.0),
                    RolePair("input/background", c.input, c.background, 3.0),
                    RolePair("border/background", c.border, c.background, 3.0),
                    RolePair("input/card", c.input, c.card, 3.0),
                    RolePair("border/card", c.border, c.card, 3.0),
                    RolePair(
                        "dawn/settingsWarningBackground",
                        c.dawn,
                        c.settingsWarningBackground,
                        3.0,
                    ),
                    RolePair("now/surface2", c.now, c.surface2, 3.0),
                    RolePair("catSleepFg/catSleep", c.catSleepFg, c.catSleep, 4.5),
                    RolePair("catLightFg/catLight", c.catLightFg, c.catLight, 4.5),
                    RolePair("catModerateFg/catModerate", c.catModerateFg, c.catModerate, 4.5),
                    RolePair("catDarkFg/catDark", c.catDarkFg, c.catDark, 4.5),
                    RolePair("catCaffeineFg/catCaffeine", c.catCaffeineFg, c.catCaffeine, 4.5),
                    RolePair("catTravelFg/catTravel", c.catTravelFg, c.catTravel, 4.5),
                    RolePair("pickerText", c.foreground, c.wizardPickerFill.compositeOver(c.card), 4.5),
                    RolePair("warningText", c.foreground, warning, 4.5),
                    RolePair("warningIcon", c.dawnText, warning, 3.0),
                    RolePair("markedListTitle", marked(c.cardForeground), marked(c.card), 4.5),
                    RolePair("markedListInterval", marked(c.mutedForeground), marked(c.card), 4.5),
                    RolePair("markedListStatus", marked(c.dawnText), marked(c.card), 3.0),
                    RolePair(
                        "priorityText",
                        c.cardForeground,
                        c.background.copy(alpha = PlanMetrics.tagOpacity.toFloat()).compositeOver(c.card),
                        4.5,
                    ),
                    RolePair(
                        "optionalTimelineSleep",
                        tile(c.cardForeground, optional),
                        tile(c.catSleep, optional),
                        4.5,
                    ),
                    RolePair(
                        "markedTimelineSleep",
                        tile(c.cardForeground, marked),
                        tile(c.catSleep, marked),
                        4.5,
                    ),
                    RolePair(
                        "optionalTimelineLight",
                        tile(c.cardForeground, optional),
                        tile(c.catLight, optional),
                        4.5,
                    ),
                    RolePair(
                        "markedTimelineLight",
                        tile(c.cardForeground, marked),
                        tile(c.catLight, marked),
                        4.5,
                    ),
                    RolePair(
                        "optionalTimelineModerate",
                        tile(c.cardForeground, optional),
                        tile(c.catModerate, optional),
                        4.5,
                    ),
                    RolePair(
                        "markedTimelineModerate",
                        tile(c.cardForeground, marked),
                        tile(c.catModerate, marked),
                        4.5,
                    ),
                    RolePair(
                        "optionalTimelineDark",
                        tile(c.cardForeground, optional),
                        tile(c.catDark, optional),
                        4.5,
                    ),
                    RolePair(
                        "markedTimelineDark",
                        tile(c.cardForeground, marked),
                        tile(c.catDark, marked),
                        4.5,
                    ),
                    RolePair(
                        "optionalTimelineCaffeine",
                        tile(c.cardForeground, optional),
                        tile(c.catCaffeine, optional),
                        4.5,
                    ),
                    RolePair(
                        "markedTimelineCaffeine",
                        tile(c.cardForeground, marked),
                        tile(c.catCaffeine, marked),
                        4.5,
                    ),
                    RolePair(
                        "optionalTimelineTravel",
                        tile(c.cardForeground, optional),
                        tile(c.catTravel, optional),
                        4.5,
                    ),
                    RolePair(
                        "markedTimelineTravel",
                        tile(c.cardForeground, marked),
                        tile(c.catTravel, marked),
                        4.5,
                    ),
                )
            pairs.forEach { verifyRole(c, it) }
        }
    }

    @Test fun onSkyRolesAreMeasuredAtNineGeneratedRampSamples() {
        themes.forEach { c ->
            for (index in 0..8) {
                val sky = CfSky.colorAt(c.isDark, index / 8f)
                val copyCard = c.skyCopyBackground.compositeOver(sky)
                listOf(
                    RolePair("skyTitle@$index", c.skyTitle, sky, 3.0),
                    RolePair("eyebrow@$index", c.dawn, sky, 4.5),
                    RolePair("planPill@$index", c.dawn, sky, 3.0),
                    RolePair("skyCopyAndGuest@$index", c.skyCopy, copyCard, 4.5),
                ).forEach { verifyRole(c, it) }
            }
        }
    }

    private data class RolePair(
        val name: String,
        val foreground: Color,
        val background: Color,
        val minimum: Double,
    )

    private fun verifyRole(
        colors: CfColors,
        pair: RolePair,
    ) {
        val key = (if (colors.isDark) "dark:" else "light:") + pair.name
        val measured = contrast(pair.foreground, pair.background)
        val exception = recordedExceptions[key]
        if (exception == null) {
            assertTrue("$key contrast $measured < ${pair.minimum}", measured >= pair.minimum)
        } else {
            assertBelow(key, measured, pair.minimum, exception)
        }
    }

    private fun assertBelow(
        key: String,
        measured: Double,
        minimum: Double,
        reason: String,
    ) {
        assertTrue("$key ($reason) is no longer below $minimum: $measured; review its exception", measured < minimum)
    }

    // OQ-A619 retains the landed tokens/opacity defaults; OQ-A108 accepts Welcome board pairs.
    // These computed contracts do not add any ATF suppression.
    private val recordedExceptions =
        mapOf(
            "light:primaryForeground/now" to "OQ-A619",
            "light:input/background" to "Phase 2 decorative borders (docs/design-system.md)",
            "light:border/background" to "Phase 2 decorative borders (docs/design-system.md)",
            "light:input/card" to "Phase 2 decorative borders (docs/design-system.md)",
            "light:border/card" to "Phase 2 decorative borders (docs/design-system.md)",
            "light:dawn/settingsWarningBackground" to "OQ-A619",
            "light:optionalTimelineSleep" to "OQ-A619",
            "light:markedTimelineSleep" to "OQ-A619",
            "light:markedTimelineLight" to "OQ-A619",
            "light:markedTimelineModerate" to "OQ-A619",
            "light:optionalTimelineDark" to "OQ-A619",
            "light:markedTimelineDark" to "OQ-A619",
            "light:optionalTimelineCaffeine" to "OQ-A619",
            "light:markedTimelineCaffeine" to "OQ-A619",
            "light:optionalTimelineTravel" to "OQ-A619",
            "light:markedTimelineTravel" to "OQ-A619",
            "light:eyebrow@2" to "OQ-A108",
            "light:eyebrow@3" to "OQ-A108",
            "light:eyebrow@4" to "OQ-A108",
            "light:eyebrow@5" to "OQ-A108",
            "light:planPill@5" to "OQ-A108",
            "light:eyebrow@6" to "OQ-A108",
            "light:planPill@6" to "OQ-A108",
            "light:eyebrow@7" to "OQ-A108",
            "light:planPill@7" to "OQ-A108",
            "light:skyTitle@8" to "OQ-A108",
            "light:eyebrow@8" to "OQ-A108",
            "light:planPill@8" to "OQ-A108",
            "dark:input/background" to "Phase 2 decorative borders (docs/design-system.md)",
            "dark:border/background" to "Phase 2 decorative borders (docs/design-system.md)",
            "dark:input/card" to "Phase 2 decorative borders (docs/design-system.md)",
            "dark:border/card" to "Phase 2 decorative borders (docs/design-system.md)",
            "dark:markedListInterval" to "OQ-A619",
            "dark:optionalTimelineSleep" to "OQ-A619",
            "dark:markedTimelineSleep" to "OQ-A619",
            "dark:optionalTimelineLight" to "OQ-A619",
            "dark:markedTimelineLight" to "OQ-A619",
            "dark:optionalTimelineModerate" to "OQ-A619",
            "dark:markedTimelineModerate" to "OQ-A619",
            "dark:optionalTimelineCaffeine" to "OQ-A619",
            "dark:markedTimelineCaffeine" to "OQ-A619",
            "dark:optionalTimelineTravel" to "OQ-A619",
            "dark:markedTimelineTravel" to "OQ-A619",
            "dark:eyebrow@7" to "OQ-A108",
            "dark:eyebrow@8" to "OQ-A108",
            "dark:planPill@8" to "OQ-A108",
        )

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
