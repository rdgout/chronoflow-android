package net.chronoflow.feature.auth.model

import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage

class AuthFailureMapping(
    failure: DataFailure,
    displayedFields: Set<String>,
    context: AuthFormContext = AuthFormContext.SignIn,
) {
    var banner: AuthBannerKind? = null
        private set
    var fields: Map<String, FieldMessage> = emptyMap()
        private set
    var retryAfterSeconds: Double? = null
        private set

    init {
        when (failure) {
            DataFailure.Offline -> {
                banner = AuthBannerKind.Offline
            }

            is DataFailure.Throttled -> {
                banner = AuthBannerKind.Locked
                retryAfterSeconds = failure.retryAfterSeconds
            }

            is DataFailure.Validation -> {
                mapValidation(failure.fields, displayedFields, context)
            }

            else -> {
                banner = AuthBannerKind.Server
            }
        }
        if (banner == null && fields.isEmpty()) banner = AuthBannerKind.Server
    }

    private fun mapValidation(
        messages: Map<String, FieldMessage>,
        displayedFields: Set<String>,
        context: AuthFormContext,
    ) {
        if (context == AuthFormContext.SignUp && messages["email"]?.key == "This email address is already in use.") {
            banner = AuthBannerKind.EmailTaken
        }
        if (context == AuthFormContext.SignIn &&
            messages.values.any { it.key == "These sign-in details don't match our records." }
        ) {
            banner = AuthBannerKind.Credentials
            return
        }
        val proofFields = setOf("code", "recovery_code")
        val activeProofHasError =
            context == AuthFormContext.TwoFactor && displayedFields.intersect(proofFields).any { it in messages }
        val mapped = mutableMapOf<String, FieldMessage>()
        for ((name, message) in messages) {
            if (banner == AuthBannerKind.EmailTaken && name == "email") continue
            if (activeProofHasError && name in proofFields && name !in displayedFields) continue
            if (name in displayedFields && AuthFieldCopy.supports(message)) {
                mapped[name] =
                    if (context == AuthFormContext.TwoFactor && name == "code" &&
                        message.key == "The authentication code was not accepted. Try again."
                    ) {
                        FieldMessage("That code didn't work. Codes change every 30 seconds, so try the newest one.")
                    } else {
                        message
                    }
            } else if (banner == null) {
                banner = AuthBannerKind.Server
            }
        }
        fields = mapped.toMap()
    }
}
