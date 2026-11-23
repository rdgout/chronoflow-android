package net.chronoflow.feature.auth.reset

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.feature.auth.model.AuthBannerKind
import net.chronoflow.feature.auth.model.AuthFormState
import net.chronoflow.feature.auth.model.ResetPasswordModel

@Composable internal fun ResetSample(state: String) {
    ResetPasswordScreen(
        ResetPasswordUiState(
            form =
                AuthFormState(
                    banner = if (state == "fieldErrors") AuthBannerKind.Server else null,
                    fields =
                        if (state == "fieldErrors") {
                            mapOf(
                                "password" to
                                    FieldMessage(
                                        "Use at least :min characters.",
                                        mapOf("min" to JsonPrimitive(12)),
                                    ),
                                "password_confirmation" to FieldMessage("The passwords don't match yet."),
                            )
                        } else {
                            emptyMap()
                        },
                ),
            state = if (state == "done") ResetPasswordModel.State.Done else ResetPasswordModel.State.Form,
        ),
        remember { TextFieldState() },
        remember { TextFieldState() },
        {},
    )
}
