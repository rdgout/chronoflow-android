package net.chronoflow.feature.auth.links

import net.chronoflow.core.model.api.SignedVerificationInput

fun interface VerifyLinkReceiver {
    suspend fun receiveVerificationLink(input: SignedVerificationInput?)
}
