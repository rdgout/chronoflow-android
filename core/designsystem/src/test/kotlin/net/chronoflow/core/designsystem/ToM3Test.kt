package net.chronoflow.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import net.chronoflow.core.designsystem.generated.CfColors
import net.chronoflow.core.designsystem.generated.CfDarkColors
import net.chronoflow.core.designsystem.generated.CfLightColors
import net.chronoflow.core.designsystem.theme.toM3
import org.junit.Assert.assertEquals
import org.junit.Test

class ToM3Test {
    @Test fun everyMaterialRoleIsMappedInBothAppearances() {
        listOf(CfLightColors, CfDarkColors).forEach { c ->
            val scheme = c.toM3()
            val getters =
                ColorScheme::class.java.methods.filter {
                    it.parameterCount == 0 &&
                        it.returnType == Long::class.javaPrimitiveType &&
                        it.name.startsWith("get")
                }
            val actual =
                getters.associate { getter ->
                    getter.name
                        .removePrefix("get")
                        .substringBefore('-')
                        .replaceFirstChar { it.lowercase() } to
                        getter.invoke(scheme)
                }
            assertEquals(expected(c).mapValues { it.value.value.toLong() }, actual)
        }
    }

    private fun expected(c: CfColors): Map<String, Color> =
        with(c) {
            mapOf(
                "primary" to dawnText,
                "onPrimary" to if (isDark) dawnForeground else primaryForeground,
                "primaryContainer" to systemSelected,
                "onPrimaryContainer" to dawnText,
                "secondary" to dawnText,
                "onSecondary" to if (isDark) dawnForeground else primaryForeground,
                "secondaryContainer" to systemSelected,
                "onSecondaryContainer" to dawnText,
                "tertiary" to dawn,
                "onTertiary" to dawnForeground,
                "tertiaryContainer" to systemSelected,
                "onTertiaryContainer" to dawnText,
                "background" to background,
                "onBackground" to foreground,
                "surface" to background,
                "onSurface" to foreground,
                "surfaceVariant" to muted,
                "onSurfaceVariant" to mutedForeground,
                "surfaceTint" to Color.Transparent,
                "inverseSurface" to foreground,
                "inverseOnSurface" to background,
                "inversePrimary" to dawn,
                "error" to destructive,
                "onError" to destructiveForeground,
                "errorContainer" to authErrorBackground,
                "onErrorContainer" to cardForeground,
                "outline" to mutedForeground,
                "outlineVariant" to border,
                "scrim" to Color.Black,
                "surfaceBright" to popover,
                "surfaceDim" to surface2,
                "surfaceContainer" to popover,
                "surfaceContainerHigh" to popover,
                "surfaceContainerHighest" to muted,
                "surfaceContainerLow" to card,
                "surfaceContainerLowest" to card,
                "primaryFixed" to systemSelected,
                "primaryFixedDim" to systemSelected,
                "onPrimaryFixed" to dawnText,
                "onPrimaryFixedVariant" to dawnText,
                "secondaryFixed" to systemSelected,
                "secondaryFixedDim" to systemSelected,
                "onSecondaryFixed" to dawnText,
                "onSecondaryFixedVariant" to dawnText,
                "tertiaryFixed" to systemSelected,
                "tertiaryFixedDim" to systemSelected,
                "onTertiaryFixed" to dawnText,
                "onTertiaryFixedVariant" to dawnText,
            )
        }
}
