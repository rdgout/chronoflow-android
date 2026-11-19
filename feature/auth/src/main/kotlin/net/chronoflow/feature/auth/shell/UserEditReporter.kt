package net.chronoflow.feature.auth.shell

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer

class UserEditReporter(
    private val onEdit: (String) -> Unit,
) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        val value = asCharSequence().toString()
        if (originalText.toString() != value) onEdit(value)
    }
}
