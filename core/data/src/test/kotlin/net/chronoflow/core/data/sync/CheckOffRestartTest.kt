package net.chronoflow.core.data.sync

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.sync.support.SyncStore
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.TripDetailResponse
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.testing.sync.check
import net.chronoflow.core.testing.sync.detail
import net.chronoflow.core.testing.sync.syncAccount
import net.chronoflow.core.testing.sync.syncDate
import net.chronoflow.core.testing.sync.syncTrip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class CheckOffRestartTest {
    @Test fun checkAndUndoSurviveDatabaseReopen() = reopen(CheckStatus.Done)

    @Test fun skipAndUndoSurviveDatabaseReopen() = reopen(CheckStatus.Skipped)

    private fun reopen(status: CheckStatus) =
        runTest(timeout = 60.seconds) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val store = SyncStore(dispatcher)
            val reopened = AccountDatabase(store.root, driverFactory = JdbcAccountDriverFactory(), io = dispatcher)

            suspend fun marks(): List<net.chronoflow.core.model.contracts.CheckMark> {
                val snapshot = TripRepository(reopened).detailValues(syncTrip).first()
                return checkNotNull(PlanDecoder(dispatcher).detail(snapshot)).checkState.marks
            }
            try {
                TripRepository(store.database).store(
                    PlanVersionRecord(
                        syncTrip,
                        "hash",
                        TripDetailResponse.encoding(detail()).data,
                        "2027-01-15T08:00:00Z",
                        "v1",
                    ),
                    emptyList(),
                )
                store.enqueue(OutboxChange.CheckOff(check(status = status)))
                store.database.close()
                reopened.open(syncAccount)
                assertEquals(1, OutboxRepository(reopened).pending().size)
                assertEquals(status, marks().single().status)
                OutboxRepository(reopened).enqueue(
                    OutboxChange.CheckOff(check(status = null)),
                    now = syncDate.plusSeconds(1),
                    randomBytes = ByteArray(10) { 1 },
                )
                reopened.close()
                reopened.open(syncAccount)
                assertEquals(2, OutboxRepository(reopened).pending().size)
                assertTrue(marks().isEmpty())
            } finally {
                withContext(NonCancellable) {
                    reopened.close()
                    store.close()
                }
            }
        }
}
