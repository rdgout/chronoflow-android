package net.chronoflow.feature.auth.guest

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import net.chronoflow.core.ui.R
import net.chronoflow.feature.auth.model.AuthFormState
import net.chronoflow.feature.auth.model.GuestSignupState

data class GuestSignupUiState(
    val name: String = "",
    val email: String = "",
    val form: AuthFormState = AuthFormState(),
    val guestState: GuestSignupState = GuestSignupState.Ready,
)

@Stable
class GuestSignupFields(
    name: String = "",
    email: String = "",
    password: String = "",
) {
    val name = TextFieldState(name)
    val email = TextFieldState(email)
    val password = TextFieldState(password)
    val confirmation = TextFieldState(password)

    fun clearSecrets() {
        password.edit { replace(0, length, "") }
        confirmation.edit { replace(0, length, "") }
    }
}

sealed interface GuestSignupEvent {
    data class EditName(
        val value: String,
    ) : GuestSignupEvent

    data class EditEmail(
        val value: String,
    ) : GuestSignupEvent

    data object EditPassword : GuestSignupEvent

    data object EditConfirmation : GuestSignupEvent

    data object Submit : GuestSignupEvent

    data object SignInInstead : GuestSignupEvent

    data object Review : GuestSignupEvent

    data object Dismiss : GuestSignupEvent
}

object GuestSignupCopy {
    val consentResource: Int = R.string.auth_by_creating_an_account_you_accept_th_b18f33
}
