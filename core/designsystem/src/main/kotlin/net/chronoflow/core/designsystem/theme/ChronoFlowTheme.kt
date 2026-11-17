@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package net.chronoflow.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalHapticFeedback
import net.chronoflow.core.designsystem.generated.CfColors
import net.chronoflow.core.designsystem.generated.CfDarkColors
import net.chronoflow.core.designsystem.generated.CfLightColors
import net.chronoflow.core.designsystem.haptics.AndroidHaptics
import net.chronoflow.core.designsystem.haptics.LocalHaptics

val LocalCfColors = staticCompositionLocalOf<CfColors> { error("LocalCfColors is provided by ChronoFlowTheme") }
val LocalCfType = staticCompositionLocalOf<CfType> { error("LocalCfType is provided by ChronoFlowTheme") }
val LocalCfShapes = staticCompositionLocalOf<CfShapes> { error("LocalCfShapes is provided by ChronoFlowTheme") }
val LocalCfSpacing = staticCompositionLocalOf<CfSpacing> { error("LocalCfSpacing is provided by ChronoFlowTheme") }
val LocalReduceMotion = staticCompositionLocalOf { false }
private val type = CfType()
private val shapes = CfShapes()
private val spacing = CfSpacing()
private val lightScheme = CfLightColors.toM3()
private val darkScheme = CfDarkColors.toM3()

@Composable
fun ChronoFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reduceMotion: Boolean = LocalReduceMotion.current,
    content: @Composable () -> Unit,
) {
    val feedback = LocalHapticFeedback.current
    val haptics = remember(feedback) { AndroidHaptics(feedback) }
    val colors = if (darkTheme) CfDarkColors else CfLightColors
    CompositionLocalProvider(
        LocalHaptics provides haptics,
        LocalCfColors provides colors,
        LocalCfType provides type,
        LocalCfShapes provides shapes,
        LocalCfSpacing provides spacing,
        LocalReduceMotion provides reduceMotion,
        LocalRippleConfiguration provides RippleConfiguration(color = colors.foreground),
        LocalTextSelectionColors provides
            TextSelectionColors(handleColor = colors.dawnText, backgroundColor = colors.dawn.copy(alpha = 0.4f)),
    ) {
        MaterialTheme(colorScheme = if (darkTheme) darkScheme else lightScheme, content = content)
    }
}

object CfTheme {
    val colors: CfColors
        @Composable @ReadOnlyComposable
        get() = LocalCfColors.current
    val type: CfType
        @Composable @ReadOnlyComposable
        get() = LocalCfType.current
    val shapes: CfShapes
        @Composable @ReadOnlyComposable
        get() = LocalCfShapes.current
    val spacing: CfSpacing
        @Composable @ReadOnlyComposable
        get() = LocalCfSpacing.current
    val reduceMotion: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalReduceMotion.current
}
