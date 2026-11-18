package net.chronoflow.feature.auth.twofactor

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import net.chronoflow.core.ui.R
import net.chronoflow.feature.auth.model.TwoFactorModel

data class FieldSpec(
    val label: Int,
    val hint: Int?,
    val placeholder: Int?,
    val keyboardOptions: KeyboardOptions,
    val contentType: ContentType?,
    val tag: String,
)

object TwoFactorFieldSpec {
    val CodeContentType: ContentType = ContentType.SmsOtpCode + ContentType("2faAppOTPCode")

    fun forMode(mode: TwoFactorModel.Mode): FieldSpec {
        val recovery = mode == TwoFactorModel.Mode.Recovery
        return FieldSpec(
            if (recovery) R.string.auth_recovery_code else R.string.auth_6_digit_code,
            if (recovery) R.string.auth_each_recovery_code_works_once else null,
            if (recovery) R.string.auth_xxxx_xxxx else null,
            KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = if (recovery) KeyboardType.Ascii else KeyboardType.Number,
                imeAction = ImeAction.Go,
            ),
            if (recovery) null else CodeContentType,
            if (recovery) "two-factor.recovery_code" else "two-factor.code",
        )
    }
}
