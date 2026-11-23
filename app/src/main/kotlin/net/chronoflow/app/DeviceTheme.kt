package net.chronoflow.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.Flow
import net.chronoflow.core.model.config.ThemeChoice
import net.chronoflow.core.model.config.resolve

@Composable
internal fun rememberDeviceNight(
    choice: ThemeChoice,
    systemDark: Boolean,
    overlayShowing: Boolean,
    onSky: Boolean,
    onSystemBars: (Boolean, Boolean, Boolean) -> Unit,
): Boolean {
    val night = choice.resolve(systemDark)
    SideEffect { onSystemBars(night, overlayShowing, onSky) }
    return night
}

@Composable
internal fun rememberDeviceTheme(
    initialTheme: ThemeChoice,
    themes: () -> Flow<ThemeChoice>,
): ThemeChoice {
    var choice by remember { mutableStateOf(initialTheme) }
    val source by rememberUpdatedState(themes)
    LaunchedEffect(Unit) {
        withFrameNanos { }
        source().collect { choice = it }
    }
    return choice
}
