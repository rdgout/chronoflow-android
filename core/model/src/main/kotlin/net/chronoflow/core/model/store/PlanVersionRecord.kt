package net.chronoflow.core.model.store

class PlanVersionRecord(
    val tripId: String,
    val inputHash: String,
    val detail: ByteArray,
    val receivedAt: String,
    val detailVersion: String? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is PlanVersionRecord &&
            tripId == other.tripId &&
            inputHash == other.inputHash &&
            detail.contentEquals(other.detail) &&
            receivedAt == other.receivedAt &&
            detailVersion == other.detailVersion

    override fun hashCode(): Int {
        var result = tripId.hashCode()
        result = 31 * result + inputHash.hashCode()
        result = 31 * result + detail.contentHashCode()
        result = 31 * result + receivedAt.hashCode()
        result = 31 * result + (detailVersion?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = "PlanVersionRecord(tripId=$tripId, inputHash=$inputHash, detail=${detail.size} B)"
}
