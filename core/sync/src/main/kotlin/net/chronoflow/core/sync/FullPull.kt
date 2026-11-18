package net.chronoflow.core.sync

import kotlinx.coroutines.CancellationException
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.session.SyncEpoch
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.model.store.StoreTime
import java.time.Instant

data class FullPullResult(
    val changes: SyncChanges = SyncChanges(),
    val verificationRequired: Boolean = false,
    val retryAfter: Double? = null,
)

class FullPull(
    val database: SyncDatabase,
    val source: DataSource,
    val retention: PlanRetention = PlanRetention(),
    private val now: () -> Instant,
) {
    suspend fun run(epoch: SyncEpoch): FullPullResult {
        database.validate(epoch)
        epoch.check()
        val before = database.snapshot()
        epoch.check()
        val identity = source.fetchIdentity()
        epoch.check()
        database.applyIdentity(epoch, identity)
        epoch.check()
        if (!identity.hasVerifiedEmail) return FullPullResult(verificationRequired = true)
        val account = source.fetchAccount()
        epoch.check()
        if (account.id != epoch.accountID.rawValue) throw StoreFailure.InvalidAccountID
        if (!account.hasVerifiedEmail) throw DataFailure.EmailUnverified(403)
        val cards = source.fetchTrips()
        epoch.check()
        database.applyPull(epoch, account, cards, StoreTime.seconds(now()), retention)
        epoch.check()
        var retryAfter: Double? = null
        for (item in cards) {
            val needed = database.detailNeeded(item, retention)
            epoch.check()
            if (!needed) continue
            try {
                fetchDetail(item.card.id, epoch)
                epoch.check()
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: DataFailure) {
                epoch.check()
                when (failure) {
                    is DataFailure.Unauthorized, is DataFailure.EmailUnverified, DataFailure.Offline,
                    is DataFailure.Throttled,
                    -> throw failure

                    else -> if (failure != DataFailure.Server(404)) retryAfter = 1.0
                }
            } catch (_: Exception) {
                epoch.check()
                retryAfter = 1.0
            }
        }
        epoch.check()
        val after = database.snapshot()
        epoch.check()
        return FullPullResult(changes = before.changes(after), retryAfter = retryAfter)
    }

    suspend fun fetchDetail(
        tripId: String,
        epoch: SyncEpoch,
    ) {
        database.validate(epoch)
        epoch.check()
        val value = source.fetchTripDetail(tripId)
        epoch.check()
        database.applyDetail(epoch, value, tripId, StoreTime.seconds(now()))
        epoch.check()
    }
}
