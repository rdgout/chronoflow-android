package net.chronoflow.core.model.guest

data class GuestClaim(
    val draftToken: String,
    val inputRevision: Int,
    val reviewRevision: Int,
    val guestSecret: String,
) {
    override fun toString(): String =
        "GuestClaim(draftToken=$draftToken, inputRevision=$inputRevision, reviewRevision=$reviewRevision, guestSecret=***)"
}
