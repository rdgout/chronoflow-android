package net.chronoflow.app

import androidx.compose.ui.text.font.FontFamily

fun interface FontPreloader {
    suspend fun preload(
        resolver: FontFamily.Resolver,
        family: FontFamily,
    )
}

internal object ResourceFontPreloader : FontPreloader {
    override suspend fun preload(
        resolver: FontFamily.Resolver,
        family: FontFamily,
    ) {
        resolver.preload(family)
    }
}
