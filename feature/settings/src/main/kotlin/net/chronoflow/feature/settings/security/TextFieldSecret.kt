package net.chronoflow.feature.settings.security

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText

class TextFieldSecret(
    private val state: TextFieldState,
) : SecretField {
    override fun read(): String = state.text.toString()

    override fun clear() {
        state.clearText()
    }
}
