@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.sync.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.SessionUpdate
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.testing.sync.TestGate
import net.chronoflow.core.testing.sync.eventually
import net.chronoflow.core.testing.sync.syncAccount
import net.chronoflow.core.testing.sync.syncOther
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SessionControllerTests {
    private class Harness(
        scope: TestScope,
        val log: SessionEventLog = SessionEventLog(),
    ) {
        val credentials = MemorySessionStore(log = log)
        val accounts = RecordingAccountStore(log)
        val controller =
            SessionController(
                credentials,
                accounts,
                SessionTeardown(sleep = {
                    awaitCancellation()
                }),
                StandardTestDispatcher(scope.testScheduler),
            )
    }

    @Test fun acceptOrdersDatabaseCredentialsPublicationAndSyncWithoutFakingMarker() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this)
            h.controller.attach(requestSync = { h.log.record("sync") })
            assertNull(h.controller.presence)
            assertNull(h.controller.updates.value)
            h.controller.accept(sessionResult())
            assertEquals(
                SessionUpdate(SessionPresence.SignedIn(syncAccount), 1),
                h.controller.updates
                    .filterNotNull()
                    .first(),
            )
            assertEquals(listOf("open:$syncAccount", "upsert:$syncAccount", "write", "sync"), h.log.events)
            assertEquals(StoredSession("test-token", syncAccount), StoredSession.parse(h.credentials.data!!))
            assertEquals(1L, h.controller.epoch)
            assertEquals(syncAccount, h.controller.storedSession?.accountId)
            h.controller.close()
        }

    private fun failures(stage: String) =
        runTest(timeout = 60.seconds) {
            val h = Harness(this)
            h.controller.attach(requestSync = { h.log.record("sync") })
            h.credentials.writeFails = stage == "keychain"
            h.accounts.openFails = stage == "open"
            h.accounts.upsertFails = stage == "upsert"
            failure(
                if (stage ==
                    "keychain"
                ) {
                    SessionFailure.CredentialWrite
                } else {
                    SessionFailure.DatabaseUnavailable
                },
            ) { h.controller.accept(sessionResult()) }
            assertNull(h.controller.presence)
            assertNull(h.controller.storedSession)
            assertNull(h.credentials.data)
            assertFalse(h.log.events.contains("sync"))
            if (stage != "keychain") assertFalse(h.log.events.contains("write"))
            h.controller.close()
        }

    @Test fun failuresCannotPublishASessionOrSyncOpen() = failures("open")

    @Test fun failuresCannotPublishASessionOrSyncUpsert() = failures("upsert")

    @Test fun failuresCannotPublishASessionOrSyncKeychain() = failures("keychain")

    @Test fun restoreRetriesUnavailableCredentialsWithoutDeletionAndOpensTheMatchingDatabase() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this)
            val data = StoredSession("legacy", syncAccount.uppercase()).encode()
            h.credentials.data = data
            h.credentials.readFails = true
            failure(SessionFailure.CredentialsUnavailable) { h.controller.restore() }
            assertEquals(0, h.credentials.deletes)
            assertArrayEquals(data, h.credentials.data)
            assertNull(h.controller.presence)
            h.credentials.readFails = false
            h.accounts.openFails = true
            failure(SessionFailure.DatabaseUnavailable) { h.controller.restore() }
            assertArrayEquals(data, h.credentials.data)
            h.accounts.openFails = false
            assertEquals(SessionPresence.SignedIn(syncAccount), h.controller.restore())
            assertEquals(syncAccount, h.accounts.accountID?.rawValue)
            assertEquals(syncAccount, h.controller.storedSession?.accountId)
            h.controller.close()
        }

    private fun malformed(data: ByteArray?) =
        runTest(timeout = 60.seconds) {
            val h = Harness(this)
            h.credentials.data = data
            assertEquals(SessionPresence.SignedOut, h.controller.restore())
            assertNull(h.credentials.data)
            assertEquals(if (data == null) 0 else 1, h.credentials.deletes)
            assertFalse(h.log.events.any { it.startsWith("open:") })
            h.controller.close()
        }

    @Test fun absentAndMalformedCredentialsSignOutAbsent() = malformed(null)

    @Test fun absentAndMalformedCredentialsSignOutInvalid() = malformed("invalid".encodeToByteArray())

    @Test fun absentAndMalformedCredentialsSignOutPath() =
        malformed("""{"token":"x","accountId":"../outside"}""".encodeToByteArray())

    @Test fun malformedCredentialsAreRetainedWhenDeletionIsTemporarilyUnavailable() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this)
            val invalid = "malformed".encodeToByteArray()
            h.credentials.data = invalid
            h.credentials.deleteFails = true
            failure(SessionFailure.CredentialsUnavailable) { h.controller.restore() }
            assertArrayEquals(invalid, h.credentials.data)
            assertNull(h.controller.presence)
            h.credentials.deleteFails = false
            assertEquals(SessionPresence.SignedOut, h.controller.restore())
            assertNull(h.credentials.data)
            h.controller.close()
        }

    @Test fun invalidSessionResultDoesNotTouchStorage() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this)
            failure(SessionFailure.InvalidResult) { h.controller.accept(sessionResult("../path")) }
            failure(SessionFailure.InvalidResult) { h.controller.accept(SessionResult("  ", sessionResult().account)) }
            assertTrue(h.log.events.isEmpty())
            assertNull(h.controller.presence)
            h.controller.close()
        }

    @Test fun newestConcurrentAcceptWinsAndStaleCompletionCannotWriteCredentials() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this)
            val gate = TestGate()
            h.accounts.gate = gate
            val first =
                backgroundScope.async {
                    failure(
                        SessionFailure.Superseded,
                    ) { h.controller.accept(sessionResult()) }
                }
            gate.waitUntilBlocked()
            val second = backgroundScope.async { h.controller.accept(sessionResult(syncOther)) }
            assertTrue(eventually { h.controller.generation == 2L })
            gate.release()
            first.await()
            second.await()
            assertEquals(SessionPresence.SignedIn(syncOther), h.controller.presence)
            assertEquals(syncOther, StoredSession.parse(h.credentials.data!!)?.accountId)
            assertEquals(1, h.log.events.count { it == "write" })
            h.controller.close()
        }

    @Test fun cancelingAnAcceptBeforeOpenCompletesPreventsPublication() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this)
            val gate = TestGate()
            h.accounts.gate = gate
            val task = backgroundScope.async { h.controller.accept(sessionResult()) }
            gate.waitUntilBlocked()
            task.cancel()
            runCurrent()
            gate.release()
            try {
                task.await()
                fail()
            } catch (_: CancellationException) {
            }
            runCurrent()
            assertNull(h.controller.presence)
            assertNull(h.credentials.data)
            h.controller.close()
        }

    @Test fun committedWritePublishesBeforeANewerInvalidAccept() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this)
            val gate = TestGate()
            h.credentials.writeGate = gate
            val first = backgroundScope.async { h.controller.accept(sessionResult()) }
            gate.waitUntilBlocked()
            val second =
                backgroundScope.async {
                    failure(SessionFailure.InvalidResult) { h.controller.accept(sessionResult("invalid")) }
                }
            assertTrue(eventually { h.controller.generation == 2L })
            gate.release()
            first.await()
            second.await()
            assertEquals(SessionPresence.SignedIn(syncAccount), h.controller.presence)
            assertEquals(StoredSession.parse(h.credentials.data!!), h.controller.storedSession)
            assertEquals(1L, h.controller.epoch)
            assertEquals(
                SessionUpdate(SessionPresence.SignedIn(syncAccount), 1),
                h.controller.updates
                    .filterNotNull()
                    .first(),
            )
            h.controller.close()
        }

    @Test fun cancellationDuringSuccessfulWriteStillCommitsAndPublishes() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this)
            val gate = TestGate()
            h.credentials.writeGate = gate
            val task = backgroundScope.async { h.controller.accept(sessionResult()) }
            gate.waitUntilBlocked()
            task.cancel()
            runCurrent()
            gate.release()
            runCurrent()
            assertEquals(SessionPresence.SignedIn(syncAccount), h.controller.presence)
            assertEquals(StoredSession.parse(h.credentials.data!!), h.controller.storedSession)
            assertEquals(
                SessionUpdate(SessionPresence.SignedIn(syncAccount), 1),
                h.controller.updates
                    .filterNotNull()
                    .first(),
            )
            h.controller.close()
        }

    @Test fun supersedingDuringSyncDoesNotRejectAnAlreadyCommittedAccept() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this)
            val gate = TestGate()
            h.controller.attach(requestSync = { gate.block() })
            val first = backgroundScope.async { h.controller.accept(sessionResult()) }
            gate.waitUntilBlocked()
            assertEquals(SessionPresence.SignedIn(syncAccount), h.controller.presence)
            val second =
                backgroundScope.async {
                    failure(SessionFailure.InvalidResult) { h.controller.accept(sessionResult("invalid")) }
                }
            assertTrue(eventually { h.controller.generation == 2L })
            gate.release()
            first.await()
            second.await()
            h.controller.close()
        }

    private fun failedRefresh(
        stage: String,
        switch: Boolean,
    ) = runTest(timeout = 60.seconds) {
        val h = Harness(this)
        h.controller.accept(sessionResult())
        val published = h.controller.updates.value!!
        val session = h.controller.storedSession
        val gate = TestGate()
        h.credentials.writeFails = stage == "keychain"
        h.credentials.writeGate = if (stage == "keychain" && !switch) gate else null
        h.accounts.openFails = stage == "open"
        h.accounts.upsertFails = stage == "upsert"
        val refresh =
            backgroundScope.async {
                failure(
                    if (stage ==
                        "keychain"
                    ) {
                        SessionFailure.CredentialWrite
                    } else {
                        SessionFailure.DatabaseUnavailable
                    },
                ) {
                    h.controller.accept(
                        SessionResult("refreshed-token", sessionResult(if (switch) syncOther else syncAccount).account),
                    )
                }
            }
        if (stage == "keychain" && !switch) {
            gate.waitUntilBlocked()
            assertEquals(published.presence, h.controller.presence)
            assertEquals(session, h.controller.storedSession)
            gate.release()
        }
        refresh.await()
        assertEquals(session, StoredSession.parse(h.credentials.data!!))
        if (switch) {
            assertNull(h.controller.storedSession)
            assertEquals(SessionPresence.SignedOut, h.controller.presence)
            assertEquals(published.epoch + 1, h.controller.epoch)
            assertEquals(SessionUpdate(SessionPresence.SignedOut, published.epoch + 1), h.controller.updates.value)
        } else {
            assertEquals(published.presence, h.controller.presence)
            assertEquals(published.epoch, h.controller.epoch)
            assertEquals(session, h.controller.storedSession)
        }
        h.controller.close()
    }

    @Test fun failedSameAccountRefreshRetainsThePublishedSessionOpen() = failedRefresh("open", false)

    @Test fun failedSameAccountRefreshRetainsThePublishedSessionUpsert() = failedRefresh("upsert", false)

    @Test fun failedSameAccountRefreshRetainsThePublishedSessionKeychain() = failedRefresh("keychain", false)

    @Test fun failedAccountSwitchPublishesSignedOutOpen() = failedRefresh("open", true)

    @Test fun failedAccountSwitchPublishesSignedOutUpsert() = failedRefresh("upsert", true)

    @Test fun failedAccountSwitchPublishesSignedOutKeychain() = failedRefresh("keychain", true)

    /** Android StateFlow starts unknown and replays only its newest publication. */
    @Test fun updatesReplayCurrentValueOnlyAfterPresenceKnown() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this)
            assertNull(h.controller.updates.value)
            h.controller.accept(sessionResult())
            h.controller.accept(sessionResult(syncOther))
            assertEquals(
                SessionUpdate(SessionPresence.SignedIn(syncOther), 3),
                h.controller.updates
                    .filterNotNull()
                    .first(),
            )
            h.controller.close()
        }
}

internal suspend fun failure(
    expected: SessionFailure,
    body: suspend () -> Unit,
) {
    try {
        body()
        fail("expected session failure")
    } catch (actual: SessionFailure) {
        assertEquals(expected, actual)
    }
}
