package net.chronoflow.core.datamock

import kotlinx.serialization.Serializable
import java.security.SecureRandom

@Serializable
data class MockGuestDraft(
    val deviceID: String,
    val secretHash: String,
    val draft: MockWizardDraft,
) {
    override fun toString(): String = "MockGuestDraft(***)"
}

object MockGuestReview {
    fun hash(secret: String): String = sha256Hex(secret)

    fun secret(): String = ByteArray(32).also { SecureRandom().nextBytes(it) }.hex()
}
