package net.chronoflow.core.data.guest

import kotlinx.coroutines.flow.Flow
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.store.deleteSetting
import net.chronoflow.core.database.store.fetchSetting
import net.chronoflow.core.database.store.saveSetting
import net.chronoflow.core.model.auth.GuestSessionContinuation
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.session.SessionMutationPermit
import net.chronoflow.core.model.store.SettingsRecord

class GuestContinuationRepository(
    private val database: AccountDatabase,
    private val permit: SessionMutationPermit? = null,
) {
    suspend fun store(destination: GuestSessionContinuation) =
        database.write(permit) { it.saveSetting(SettingsRecord(SECTION, destination.encode())) }

    fun values(): Flow<GuestSessionContinuation?> =
        database.observe("settings") { it.fetchSetting(SECTION)?.value?.let(GuestSessionContinuation::decode) }

    suspend fun consume(gate: Gate): GuestSessionContinuation? {
        if (gate != Gate.App) return null
        return database.write(permit) {
            val destination = it.fetchSetting(SECTION)?.value?.let(GuestSessionContinuation::decode)
            it.deleteSetting(SECTION)
            destination
        }
    }

    private companion object {
        const val SECTION = "guestContinuation"
    }
}
