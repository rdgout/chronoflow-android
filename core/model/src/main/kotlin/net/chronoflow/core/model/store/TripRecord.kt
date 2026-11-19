package net.chronoflow.core.model.store

data class TripRecord(
    val id: String,
    val card: String,
    val adaptation: String? = null,
    val form: String? = null,
    val revision: String? = null,
    val currentInputHash: String? = null,
    val keepOffline: Boolean = false,
    val departureUtc: String,
    val planStartUtc: String? = null,
    val planStartZone: String? = null,
    val preparationDays: Int? = null,
    val planEndUtc: String? = null,
    val planEndDate: String? = null,
    val deletedAt: String? = null,
    val deletedLocally: Boolean = false,
)
