package net.chronoflow.core.data.session

interface GuestSecrets {
    suspend fun deleteAll()
}

object NoGuestSecrets : GuestSecrets {
    override suspend fun deleteAll() = Unit
}
