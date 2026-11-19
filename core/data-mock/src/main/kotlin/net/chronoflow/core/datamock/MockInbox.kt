@file:Suppress("ktlint:standard:filename")

package net.chronoflow.core.datamock

import kotlinx.serialization.Serializable
import net.chronoflow.core.model.api.ResetPasswordInput
import net.chronoflow.core.model.api.SignedVerificationInput

@Serializable
sealed interface MockMail {
    @Serializable
    data class PasswordReset(
        val id: String,
        val to: String,
        val sentAt: Double,
        val input: ResetPasswordInput,
    ) : MockMail {
        override fun toString(): String = "PasswordReset(id=$id, to=$to, sentAt=$sentAt, input=***)"
    }

    @Serializable
    data class Verification(
        val id: String,
        val to: String,
        val sentAt: Double,
        val input: SignedVerificationInput,
    ) : MockMail {
        override fun toString(): String = "Verification(id=$id, to=$to, sentAt=$sentAt, input=***)"
    }
}
