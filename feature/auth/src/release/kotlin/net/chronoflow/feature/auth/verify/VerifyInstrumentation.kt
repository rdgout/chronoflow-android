package net.chronoflow.feature.auth.verify

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun Modifier.verifyCountdown(remaining: Int): Modifier = this

@Composable
internal fun Modifier.verifyButtons(state: VerifyEmailUiState): Modifier = this
