package net.chronoflow.feature.auth.signup

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.feature.auth.model.AuthFormContext
import net.chronoflow.feature.auth.model.AuthFormModel

internal fun signUpSnapshotState(state: String): SignUpUiState {
    val form = AuthFormModel(setOf("name", "email", "password", "password_confirmation"), AuthFormContext.SignUp)
    val required = FieldMessage("Please fill this in.")
    val invalidEmail = FieldMessage("Enter a valid email address, like name@example.com.")
    val taken = FieldMessage("This email address is already in use.")
    val minimum = FieldMessage("Use at least :min characters.", mapOf("min" to JsonPrimitive(12)))
    val mismatch = FieldMessage("The passwords don't match yet.")
    val failure =
        when (state) {
            "empty" -> {
                DataFailure.Validation(
                    mapOf(
                        "name" to required,
                        "email" to required,
                        "password" to required,
                        "password_confirmation" to required,
                    ),
                    422,
                )
            }

            "email" -> {
                DataFailure.Validation(mapOf("email" to invalidEmail), 422)
            }

            "taken" -> {
                DataFailure.Validation(mapOf("email" to taken), 422)
            }

            "password" -> {
                DataFailure.Validation(mapOf("password" to minimum), 422)
            }

            "confirmation" -> {
                DataFailure.Validation(mapOf("password_confirmation" to mismatch), 422)
            }

            "server" -> {
                DataFailure.Server(503)
            }

            "throttle" -> {
                DataFailure.Throttled(60.0, 429)
            }

            "offline" -> {
                DataFailure.Offline
            }

            else -> {
                null
            }
        }
    failure?.let(form::receive)
    val name = if (state == "empty") "" else "Rick"
    val email = if (state == "empty") "" else "rick@example.com"
    return SignUpUiState(name, email, state == "visible", form.state.value.copy(failure = null))
}

@Composable
internal fun SignUpSample(
    state: String,
    board: Boolean = false,
) {
    val value = remember(state) { signUpSnapshotState(state) }
    val password =
        if (board) {
            "sunrise-in-tokyo"
        } else if (state == "visible") {
            "NewPassword123!"
        } else {
            ""
        }
    SignUpContent(
        value,
        remember { TextFieldState(value.name) },
        remember { TextFieldState(value.email) },
        remember { TextFieldState(password) },
        remember { TextFieldState(password) },
        PolicyOpener {},
        {},
    )
}
