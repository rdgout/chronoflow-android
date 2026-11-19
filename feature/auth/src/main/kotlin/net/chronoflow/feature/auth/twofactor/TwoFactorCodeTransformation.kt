package net.chronoflow.feature.auth.twofactor

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import net.chronoflow.feature.auth.model.TwoFactorModel

class TwoFactorCodeTransformation : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        val filtered = TwoFactorModel.filterCode(asCharSequence())
        if (asCharSequence().toString() != filtered) replace(0, length, filtered)
    }
}
