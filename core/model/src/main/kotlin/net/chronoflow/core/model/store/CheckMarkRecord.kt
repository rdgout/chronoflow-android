package net.chronoflow.core.model.store

data class CheckMarkRecord(
    val tripId: String,
    val inputHash: String,
    val type: String,
    val localDate: String,
    val ordinal: Int,
    val status: String,
    val markedAt: String,
)
