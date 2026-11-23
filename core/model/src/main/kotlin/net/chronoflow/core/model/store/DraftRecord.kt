package net.chronoflow.core.model.store

data class DraftRecord(
    val id: String,
    val tripId: String? = null,
    val form: String,
    val baseForm: String? = null,
    val dirty: Boolean = true,
    val review: String? = null,
    val token: String? = null,
    val inputRevision: Int = 0,
    val reviewRevision: Int? = null,
    val wizardSourceRevision: String? = null,
    val wizardReview: String? = null,
    val furthestStep: String? = null,
    val wizardState: String? = null,
    val sourceRevision: String? = null,
    val saveFailure: String? = null,
    val fieldErrors: String? = null,
    val updatedAt: String,
) {
    override fun toString(): String = "DraftRecord(id=$id, tripId=$tripId)"
}
