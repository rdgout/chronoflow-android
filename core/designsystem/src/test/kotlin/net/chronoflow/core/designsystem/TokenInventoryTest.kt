package net.chronoflow.core.designsystem

import androidx.compose.ui.graphics.toArgb
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfDarkColors
import net.chronoflow.core.designsystem.generated.CfLightColors
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.generated.PressTokens
import net.chronoflow.core.designsystem.generated.SettingsTokens
import net.chronoflow.core.designsystem.generated.SkyRamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenInventoryTest {
    @Test fun generatedColorsMatchThePinnedInventory() {
        val expected =
            listOf(
                listOf(
                    0xFFFAECE3,
                    0xFFFAEAEA,
                    0x59CC2827,
                    0xFFFBF2EE,
                    0x66E78A45,
                    0x40202955,
                    0xFFE78A45,
                    0xFFA35303,
                    0xFF2D1205,
                    0xFFFBFEFF,
                    0xFFFBFEFF,
                    0xFFF3F7FC,
                ),
                listOf(
                    0xFF302D38,
                    0xFF262031,
                    0x59EF6661,
                    0xFF242433,
                    0x66F2AD73,
                    0x99000000,
                    0xFFF2AD73,
                    0xFFF2AD73,
                    0xFF101428,
                    0xFF12172B,
                    0xFF161B32,
                    0xFF0A0E1F,
                ),
            )
        listOf(CfLightColors, CfDarkColors).forEachIndexed { index, c ->
            assertEquals(
                expected[index].map {
                    it.toInt()
                },
                listOf(
                    c.systemSelected,
                    c.authErrorBackground,
                    c.authErrorBorder,
                    c.settingsWarningBackground,
                    c.settingsWarningBorder,
                    c.authShadow,
                    c.dawn,
                    c.dawnText,
                    c.dawnForeground,
                    c.card,
                    c.popover,
                    c.background,
                ).map {
                    it.toArgb()
                },
            )
        }
    }

    @Test fun generatedGeometryAndMotionMatchThePinnedInventory() {
        assertEquals(
            listOf(10f, 15f, -12f, 12f, 20f, -14f, 26f),
            listOf(
                AuthTokens.lightShadowY,
                AuthTokens.lightShadowRadius,
                AuthTokens.lightShadowSpread,
                AuthTokens.darkShadowY,
                AuthTokens.darkShadowRadius,
                AuthTokens.darkShadowSpread,
                AuthTokens.cardRadius,
            ),
        )
        assertEquals(0.97f, PressTokens.scale)
        assertEquals(0.85f, PressTokens.opacity)
        assertEquals(120, PressTokens.durationMillis)
        assertEquals(listOf(0.25f, 0.1f, 0.25f, 1f), PressTokens.curve.toList())
        assertEquals(
            listOf(10f, 12f, 14f, 18f, 22f, 26f),
            listOf(
                PlanUITokens.radiusSm,
                PlanUITokens.radiusMd,
                PlanUITokens.radiusLg,
                PlanUITokens.radiusXl,
                PlanUITokens.radius2Xl,
                PlanUITokens.radius3Xl,
            ),
        )
        assertEquals(
            listOf(4f, 8f, 12f, 16f, 20f, 24f, 32f, 56f, 64f),
            listOf(
                PlanUITokens.space1,
                PlanUITokens.space2,
                PlanUITokens.space3,
                PlanUITokens.space4,
                PlanUITokens.space5,
                PlanUITokens.space6,
                PlanUITokens.space8,
                PlanUITokens.space14,
                PlanUITokens.space16,
            ),
        )
        assertEquals(
            listOf(64f, 10f, 36f, 44f, 6f, 2f, 38f),
            listOf(
                SettingsTokens.rowHeight,
                SettingsTokens.rowVertical,
                SettingsTokens.rowDisc,
                SettingsTokens.pillHeight,
                SettingsTokens.pillGap,
                SettingsTokens.selectionWidth,
                SettingsTokens.sheetRadius,
            ),
        )
        assertEquals(33, SkyRamp.light.size)
        assertEquals(33, SkyRamp.dark.size)
        assertTrue(SkyRamp.light.any { it.location == 0.55f })
        assertTrue(SkyRamp.dark.any { it.location == 0.6f })
        assertEquals(
            listOf(
                0xFF1F285A.toInt(),
                0xFFEB9666.toInt(),
                0xFF06091C.toInt(),
                0xFFAA5830.toInt(),
            ),
            listOf(SkyRamp.light.first(), SkyRamp.light.last(), SkyRamp.dark.first(), SkyRamp.dark.last()).map {
                it.color.toArgb()
            },
        )
    }
}
