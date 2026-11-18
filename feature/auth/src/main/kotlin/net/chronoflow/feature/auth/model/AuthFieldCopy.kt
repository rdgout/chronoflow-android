package net.chronoflow.feature.auth.model

import net.chronoflow.core.model.FieldMessage

data class AuthText(
    val key: String,
    val count: Long? = null,
)

object AuthFieldCopy {
    fun field(message: FieldMessage): AuthText? {
        return when (message.key) {
            "Please fill this in.", "Enter a valid email address, like name@example.com.",
            "The passwords don't match yet.", "Use both uppercase and lowercase letters.",
            "Use at least one letter.", "Use at least one number.", "Use at least one symbol.",
            "This password has appeared in a data leak. Choose a different password.",
            "This email verification link is invalid or has expired.",
            "Enter the 6-digit code from your authenticator app.",
            "Enter one of the recovery codes you saved when you set up two-factor authentication.",
            "The authentication code was not accepted. Try again.",
            "That code didn't work. Codes change every 30 seconds, so try the newest one.",
            "This recovery code was not accepted. Try another saved code.",
            -> {
                AuthText(message.key)
            }

            "Use no more than :max characters.", "Use at least :min characters.",
            "Use no more than {{max}} characters.", "Use at least {{min}} characters.",
            -> {
                val name = if (message.key.contains("max")) "max" else "min"
                val value = message.arguments[name] ?: return null
                if (value.isString) return null
                val number = value.content.toBigDecimalOrNull() ?: return null
                if (number.signum() < 0 || number > Long.MAX_VALUE.toBigDecimal() ||
                    number.stripTrailingZeros().scale() > 0
                ) {
                    return null
                }
                AuthText(
                    if (name ==
                        "max"
                    ) {
                        "Use no more than %lld characters."
                    } else {
                        "Use at least %lld characters."
                    },
                    number.toLong(),
                )
            }

            else -> {
                null
            }
        }
    }

    fun supports(message: FieldMessage): Boolean = field(message) != null
}
