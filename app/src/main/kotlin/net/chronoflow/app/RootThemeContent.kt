package net.chronoflow.app

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme

@Composable
internal fun RootThemeContent(
    darkTheme: Boolean,
    reduceMotion: Boolean,
    firstCommit: Deferred<Unit>,
    preloader: FontPreloader,
    dispatcher: CoroutineDispatcher,
    onFontsReady: () -> Unit,
    content: @Composable () -> Unit,
) {
    RootFontWarmup(firstCommit, preloader, dispatcher, onFontsReady)
    ChronoFlowTheme(darkTheme = darkTheme, reduceMotion = reduceMotion, content = content)
}
