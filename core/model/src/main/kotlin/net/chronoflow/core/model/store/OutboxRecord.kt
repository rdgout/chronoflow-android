package net.chronoflow.core.model.store

data class OutboxRecord(
    val id: String,
    val kind: String,
    val payload: String,
    val createdAt: String,
    val attempts: Int = 0,
    val lastError: String? = null,
)
