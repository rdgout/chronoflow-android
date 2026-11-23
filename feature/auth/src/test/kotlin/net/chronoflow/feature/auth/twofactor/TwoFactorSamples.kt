package net.chronoflow.feature.auth.twofactor

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.feature.auth.model.AuthBannerKind
import net.chronoflow.feature.auth.model.AuthFormState
import net.chronoflow.feature.auth.model.TwoFactorModel

internal val twoFactorStates =
    listOf(
        "code-none",
        "code-fieldError",
        "code-attempts",
        "code-server",
        "recovery-none",
        "recovery-fieldError",
        "recovery-attempts",
        "recovery-server",
    )

@Composable
internal fun TwoFactorSample(name: String) {
    val recovery = name.startsWith("recovery-")
    val mode = if (recovery) TwoFactorModel.Mode.Recovery else TwoFactorModel.Mode.Code
    val field = if (recovery) "recovery_code" else "code"
    val banner =
        when (name.substringAfter('-')) {
            "attempts" -> AuthBannerKind.Locked
            "server" -> AuthBannerKind.Server
            else -> null
        }
    val fields =
        if (name.endsWith("fieldError")) {
            mapOf(
                field to
                    FieldMessage(
                        if (recovery) {
                            "This recovery code was not accepted. Try another saved code."
                        } else {
                            "That code didn't work. Codes change every 30 seconds, so try the newest one."
                        },
                    ),
            )
        } else {
            emptyMap()
        }
    TwoFactorContent(
        TwoFactorUiState(mode, "", AuthFormState(banner = banner, fields = fields), field),
        remember { TextFieldState() },
        {},
        {},
        {},
    )
}
