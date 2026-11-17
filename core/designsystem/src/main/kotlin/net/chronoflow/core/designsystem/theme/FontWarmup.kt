package net.chronoflow.core.designsystem.theme

import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

suspend fun warmFonts(
    preload: suspend (FontFamily) -> Unit,
    dispatcher: CoroutineDispatcher,
) {
    withContext(dispatcher) {
        listOf(Figtree, Fraunces20, Fraunces32, Fraunces48).forEach { preload(it) }
    }
}
