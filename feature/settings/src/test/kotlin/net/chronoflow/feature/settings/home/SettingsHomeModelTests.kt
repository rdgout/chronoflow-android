@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.home

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.feature.settings.support.SettingsFixture
import net.chronoflow.feature.settings.support.SettingsFixtureTest
import net.chronoflow.feature.settings.support.withSettingsFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

class SettingsHomeModelTests : SettingsFixtureTest() {
    @Test fun stayPreservesQueueDiscardWipesAndNextAccountHasNoOldRows() =
        runTest(timeout = 60.seconds) {
            withSettingsFixture { f ->
                val model = f.model()
                val row = f.pending()
                runCurrent()
                model.signOut()
                runCurrent()
                assertTrue(model.ui.value.showsPending)
                assertEquals(1, model.ui.value.confirmationCount)
                assertNotNull(f.credentials.data)
                model.stay()
                assertFalse(model.ui.value.showsPending)
                assertEquals(listOf(row.id), f.outbox.pending().map { it.id })
                assertEquals(f.snapshot, f.controller.activeSession())
                model.signOut()
                runCurrent()
                model.signOut(true)
                runCurrent()
                assertEquals(SessionPresence.SignedOut, f.controller.presence)
                assertNull(f.credentials.data)
                assertNull(model.ui.value.state.account)
                f.controller.accept(f.result.copy(account = f.result.account.copy(id = SECOND_ID)))
                val next = checkNotNull(f.accounts.activeAccountDatabase())
                assertTrue(OutboxRepository(next).pending().isEmpty())
                assertTrue(TripRepository(next).trips().isEmpty())
                assertTrue(SettingsRepository(next).values().first().all { it.section == "policies" })
            }
        }

    @Test fun observationUpdatesBadgesAndEmptiedQueueDismissesSheet() =
        runTest(timeout = 60.seconds) {
            withSettingsFixture { f ->
                val model = f.model()
                val row = f.pending()
                model.signOut()
                runCurrent()
                assertTrue(model.ui.value.showsPending)
                f.settings.upsert(SettingsRecord("planCredits", """{"credits":13}"""))
                runCurrent()
                assertEquals(13, model.ui.value.state.credits)
                f.outbox.remove(row.id)
                runCurrent()
                assertFalse(model.ui.value.showsPending)
                assertTrue(
                    model.ui.value.state.pending
                        .isEmpty(),
                )
            }
        }

    @Test fun emptyQueueAndLogoutTimeoutStillFinishSignOut() =
        runTest(timeout = 60.seconds) {
            withSettingsFixture { f ->
                f.logout = { awaitCancellation() }
                val model = f.model()
                model.signOut()
                runCurrent()
                assertEquals(0, f.signOutCalls)
                assertNotNull(f.credentials.data)
                advanceTimeBy(3000)
                runCurrent()
                assertEquals(SessionPresence.SignedOut, f.controller.presence)
                assertFalse(model.ui.value.busy)
                assertFalse(model.ui.value.showsPending)
                assertNull(model.failure)
                assertNull(f.credentials.data)
            }
        }

    @Test fun forced401DuringFlushNeverPresentsStaleConfirmation() =
        runTest(timeout = 60.seconds) {
            withSettingsFixture { f ->
                val gate = CompletableDeferred<Unit>()
                f.flush = { gate.await() }
                val model = f.model()
                f.pending()
                model.signOut()
                runCurrent()
                assertTrue(model.ui.value.busy)
                val forced = backgroundScope.launch { f.controller.unauthorized(f.snapshot.epoch) }
                runCurrent()
                assertEquals(f.snapshot.epoch, f.controller.pendingUnauthorizedEpoch)
                gate.complete(Unit)
                forced.join()
                runCurrent()
                assertFalse(model.ui.value.showsPending)
                assertTrue(
                    model.ui.value.state.pending
                        .isEmpty(),
                )
                assertNull(f.credentials.data)
            }
        }

    @Test fun syncEventsDriveOfflineCopyAndReplayToALateSettingsSubscriber() =
        runTest(timeout = 60.seconds) {
            withSettingsFixture { f ->
                f.syncSource = MockDataSource(offline = true, sleep = {}, io = f.io)
                val model = f.model()
                f.sync.start(f.snapshot.session, f.snapshot.epoch)
                f.sync.refreshNow()
                runCurrent()
                assertTrue(model.ui.value.offline)
                assertFalse(model.ui.value.syncing)
                val late = f.model()
                runCurrent()
                assertTrue(late.ui.value.offline)
                assertEquals(
                    SyncLine.Offline,
                    late.ui.value.state.syncLine(
                        f.clock.now,
                        late.ui.value.syncing,
                        late.ui.value.offline,
                        f.deps.zone,
                    ),
                )
            }
        }

    @Test fun replacementSessionRevokesOldDiscardAndClearsValues() =
        runTest(timeout = 60.seconds) {
            withSettingsFixture { f ->
                val model = f.model()
                f.pending()
                model.signOut()
                runCurrent()
                f.controller.accept(f.result.copy(account = f.result.account.copy(id = SECOND_ID)))
                val replacement = f.controller.activeSession()
                model.signOut(true)
                runCurrent()
                assertEquals(replacement, f.controller.activeSession())
                assertNull(model.ui.value.state.account)
                assertFalse(model.ui.value.showsPending)
            }
        }

    @Test fun discardWithoutSheetIsIgnored() =
        runTest(timeout = 60.seconds) {
            withSettingsFixture { f ->
                val model = f.model()
                runCurrent()
                model.signOut(true)
                runCurrent()
                assertEquals(f.snapshot, f.controller.activeSession())
                assertEquals(0, f.signOutCalls)
            }
        }

    @Test fun doubleTapSignOutRunsOnce() =
        runTest(timeout = 60.seconds) {
            withSettingsFixture { f ->
                val gate = CompletableDeferred<Unit>()
                f.flush = {
                    gate.await()
                    throw DataFailure.Offline
                }
                val model = f.model()
                f.pending()
                model.signOut()
                model.signOut()
                runCurrent()
                assertTrue(model.ui.value.busy)
                gate.complete(Unit)
                runCurrent()
                assertEquals(1, f.signOutCalls)
                assertTrue(model.ui.value.showsPending)
            }
        }

    companion object {
        private const val SECOND_ID = "019a562b-8000-7000-8000-000000000072"
    }
}
