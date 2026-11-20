package net.chronoflow.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalFontFamilyResolver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import net.chronoflow.core.designsystem.theme.warmFontsAfterFirstFrame

@Composable
internal fun RootFontWarmup(
    firstCommit: Deferred<Unit>,
    preloader: FontPreloader,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    onReady: () -> Unit,
) {
    val ready = rememberUpdatedState(onReady)
    val resolver = LocalFontFamilyResolver.current
    LaunchedEffect(resolver) {
        withFrameNanos { }
        try {
            warmFontsAfterFirstFrame(firstCommit, { preloader.preload(resolver, it) }, dispatcher)
        } finally {
            ready.value()
        }
    }
}
