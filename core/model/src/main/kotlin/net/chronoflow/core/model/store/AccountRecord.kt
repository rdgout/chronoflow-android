package net.chronoflow.core.model.store

data class AccountRecord(
    val id: String,
    val name: String,
    val email: String,
    val hasVerifiedEmail: Boolean = true,
    val syncedAt: String? = null,
    val walkthroughSeen: Boolean = false,
)
