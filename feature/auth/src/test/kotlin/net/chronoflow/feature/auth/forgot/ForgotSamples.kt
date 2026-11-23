package net.chronoflow.feature.auth.forgot

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.feature.auth.model.AuthBannerKind
import net.chronoflow.feature.auth.model.AuthFormModel
import net.chronoflow.feature.auth.model.AuthFormState

@Composable internal fun ForgotSample(
    state: String,
    expired: Boolean = false,
) {
    ForgotPasswordScreen(
        ForgotPasswordUiState(
            form =
                AuthFormState(
                    pending = if (state == "busy") AuthFormModel.Action.SendResetLink else null,
                    banner = if (state == "server") AuthBannerKind.Server else null,
                    fields =
                        if (state ==
                            "fieldError"
                        ) {
                            mapOf("email" to FieldMessage("Enter a valid email address, like name@example.com."))
                        } else {
                            emptyMap()
                        },
                ),
            sentEmail = if (state == "sent") "demo@example.com" else null,
            expired = expired,
        ),
        remember { TextFieldState(if (expired) "demo@example.com" else "") },
        {},
    )
}
