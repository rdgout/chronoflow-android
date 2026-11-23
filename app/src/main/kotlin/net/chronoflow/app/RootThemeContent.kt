package net.chronoflow.app

import androidx.compose.runtime.Composable
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme

@Composable
internal fun RootThemeContent(
    darkTheme: Boolean,
    inputs: RootThemeInputs,
    content: @Composable () -> Unit,
) {
    RootFontWarmup(inputs.firstCommit, inputs.preloader, inputs.dispatcher, inputs.onFontsReady)
    ChronoFlowTheme(darkTheme = darkTheme, reduceMotion = inputs.reduceMotion, content = content)
}
