package net.chronoflow.feature.auth.signin

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.SafeInsetsDp
import net.chronoflow.feature.auth.model.AuthBannerKind
import net.chronoflow.feature.auth.model.AuthFormModel
import net.chronoflow.feature.auth.model.AuthFormState

internal val authBoard =
    DeviceProfile("authBoard", 375, 812, 3f, 1f, "gesture", SafeInsetsDp(0f, 0f, 0f, 0f), "AuthSignIn board")

internal fun signInSample(
    state: String,
    passkey: Boolean = true,
): SignInUiState {
    val banner =
        when (state) {
            "locked" -> AuthBannerKind.Locked
            "server" -> AuthBannerKind.Server
            "wrong" -> AuthBannerKind.Credentials
            "offline" -> AuthBannerKind.Offline
            else -> null
        }
    val fields =
        if (state ==
            "fieldError"
        ) {
            mapOf(
                "email" to FieldMessage("Please fill this in."),
                "password" to FieldMessage("Please fill this in."),
            )
        } else {
            emptyMap()
        }
    return SignInUiState(
        AuthFormState(
            pending =
                if (state ==
                    "busy"
                ) {
                    AuthFormModel.Action.Password
                } else {
                    null
                },
            banner = banner,
            fields = fields,
        ),
        passkeyVisible = passkey,
    )
}

@Composable
internal fun SignInSample(
    state: String,
    passkey: Boolean = true,
) {
    SignInContent(signInSample(state, passkey), remember { TextFieldState() }, remember { TextFieldState() }, {})
}
