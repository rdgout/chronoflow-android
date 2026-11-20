package net.chronoflow.core.data.wizard.support

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.AirportRef
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripProgress
import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.model.store.TripRecord
import java.io.File

internal const val WIZARD_ACCOUNT = "019a562b-8000-7000-8000-000000000001"

internal suspend fun wizardDatabase(
    io: CoroutineDispatcher,
    root: File,
    id: String = WIZARD_ACCOUNT,
): AccountDatabase = AccountDatabase(root, driverFactory = JdbcAccountDriverFactory(), io = io).also { it.open(id) }

internal class WizardStore private constructor(
    val database: AccountDatabase,
    val root: File,
    val io: CoroutineDispatcher,
) {
    suspend fun close() =
        withContext(NonCancellable + io) {
            database.close()
            check(root.deleteRecursively())
        }

    companion object {
        suspend operator fun invoke(
            io: CoroutineDispatcher,
            id: String = WIZARD_ACCOUNT,
        ): WizardStore {
            val root = withContext(io) { TestRoots.temporaryStoreRoot() }
            return WizardStore(wizardDatabase(io, root, id), root, io)
        }
    }
}

internal fun wizardTrip(
    id: String,
    origin: String,
    departure: String,
): TripRecord {
    val card =
        TripCardView(
            id = id,
            from = AirportRef(origin, "Amsterdam", "Europe/Amsterdam"),
            to = AirportRef("HND", "Tokyo", "Asia/Tokyo"),
            departLocal = "2026-11-01T12:00",
            arriveLocal = "2026-11-02T09:00",
            departureUtc = departure,
            arrivalUtc = "2026-11-02T00:00:00Z",
            flightNumber = null,
            connections = emptyList(),
            offsetDifferenceMinutes = 480,
            progress = TripProgress(TripStatus.Upcoming, null),
            canComplete = false,
            canReopen = false,
            canEdit = true,
            canDuplicate = true,
            canDelete = true,
        )
    return TripRecord(id, ApiJson.response.encodeToString(card), departureUtc = departure)
}
