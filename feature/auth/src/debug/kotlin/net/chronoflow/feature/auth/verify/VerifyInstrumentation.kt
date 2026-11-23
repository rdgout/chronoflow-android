@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package net.chronoflow.feature.auth.verify

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

class VerifyCompositionProbe {
    var countdownCompositions = 0
    var buttonCompositions = 0
    var lastRemaining: Int? = null
    var lastButtons: VerifyEmailUiState? = null
}

val LocalVerifyCompositionProbe = staticCompositionLocalOf<VerifyCompositionProbe?> { null }

@Composable
internal fun Modifier.verifyCountdown(remaining: Int): Modifier {
    val probe = LocalVerifyCompositionProbe.current
    SideEffect {
        probe?.let {
            it.countdownCompositions++
            it.lastRemaining = remaining
        }
    }
    return this
}

@Composable
internal fun Modifier.verifyButtons(state: VerifyEmailUiState): Modifier {
    val probe = LocalVerifyCompositionProbe.current
    SideEffect {
        probe?.let {
            it.buttonCompositions++
            it.lastButtons = state
        }
    }
    return this
}
