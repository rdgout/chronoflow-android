@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package net.chronoflow.feature.launch.welcome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

class WelcomeProbe {
    var compositions = 0
        private set

    fun onComposition() {
        compositions++
    }
}

val LocalWelcomeProbe = staticCompositionLocalOf<WelcomeProbe?> { null }

@Composable
internal fun welcomeCompositionObserver(): () -> Unit = LocalWelcomeProbe.current?.let { it::onComposition } ?: {}
