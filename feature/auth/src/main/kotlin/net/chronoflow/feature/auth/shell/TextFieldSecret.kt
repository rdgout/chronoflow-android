package net.chronoflow.feature.auth.shell

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import net.chronoflow.feature.auth.model.SecretField

class TextFieldSecret(
    private val state: TextFieldState,
) : SecretField {
    override fun read(): String = state.text.toString()

    override fun clear() {
        state.clearText()
    }
}
