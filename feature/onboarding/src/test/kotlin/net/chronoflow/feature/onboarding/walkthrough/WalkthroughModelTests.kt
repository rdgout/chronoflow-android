@file:Suppress("ktlint:standard:max-line-length")

@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.onboarding.walkthrough

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.session.AccountReadinessRepository
import net.chronoflow.core.data.session.SessionAccountDatabase
import net.chronoflow.core.data.sync.SqlSyncDatabase
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.model.session.SyncEpoch
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.sync.FullPull
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionTeardown
import net.chronoflow.feature.onboarding.support.onboardingNow
import net.chronoflow.feature.onboarding.support.withOnboardingFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class WalkthroughModelTests {
    @Test fun nextNextDoneCommitsOnceAndGateMovesWithoutAnyNetworkRequest() =
        runTest(timeout = 60.seconds) {
            withOnboardingFixture { f ->
                assertEquals(Gate.Walkthrough, f.gate())
                f.source.setOffline(true)
                val model = f.walkthroughModel()
                assertEquals(WalkthroughModel.Page.Timeline, model.state.value.page)
                model.advance()
                assertEquals(WalkthroughModel.Page.Categories, model.state.value.page)
                model.advance()
                assertEquals(WalkthroughModel.Page.Progress, model.state.value.page)
                assertTrue(f.outbox.pending().isEmpty())
                model.advance()
                assertTrue(model.state.value.completed)
                assertFalse(model.state.value.busy)
                assertNull(model.state.value.failure)
                assertEquals(Gate.App, f.gate())
                val row = f.outbox.pending().single()
                assertEquals("walkthrough", row.kind)
                assertEquals(OutboxChange.Walkthrough, OutboxPayload.change(row))
                assertTrue(f.account.account()!!.walkthroughSeen)
                model.skip()
                model.advance()
                f.outbox.enqueue(OutboxChange.Walkthrough)
                assertEquals(listOf(row.id), f.outbox.pending().map { it.id })
            }
        }

    @Test fun relaunchPendingOverlaySurvivesSessionUpsertAndStaleFullPull() =
        runTest(timeout = 60.seconds) {
            withOnboardingFixture { f ->
                f.walkthroughModel().skip()
                val rows = f.outbox.pending().map { it.id }
                f.accounts.close()
                val accounts =
                    SessionAccountDatabase({ f.root }, StorageNamespace.Mock, JdbcAccountDriverFactory(), f.io)
                val restored = SessionController(f.credentials, accounts, SessionTeardown(sleep = {}), f.io)
                try {
                    restored.restore()
                    val database = checkNotNull(accounts.activeAccountDatabase())
                    accounts.upsert(f.result.account)
                    val snapshot = checkNotNull(restored.activeSession())
                    FullPull(SqlSyncDatabase(database), f.source, now = {
                        onboardingNow
                    }).run(SyncEpoch(AccountID.of(f.result.account.id), snapshot.epoch))
                    assertEquals(rows, OutboxRepository(database).pending().map { it.id })
                    assertTrue(AccountRepository(database).account()!!.walkthroughSeen)
                    val ready = AccountReadinessRepository(database).values(f.result.account.id).first()!!
                    assertEquals(Gate.App, ready.gate)
                    assertEquals(Gate.App, ready.firstLoadGate)
                } finally {
                    restored.close()
                    runCurrent()
                    accounts.close()
                }
            }
        }

    @Test fun replayReturnsWithoutWritingOrRoutingToToday() =
        runTest(timeout = 60.seconds) {
            var writes = 0
            var returns = 0
            val model = WalkthroughModel(WalkthroughModel.Mode.Replay, { writes++ }, { returns++ })
            model.advance()
            model.advance()
            model.advance()
            model.skip()
            assertTrue(model.state.value.completed)
            assertEquals(0, writes)
            assertEquals(1, returns)
        }

    @Test fun invalidatedSessionDoesNotCompleteAndStorageFailureCanRetry() =
        runTest(timeout = 60.seconds) {
            withOnboardingFixture { f ->
                val model = f.walkthroughModel()
                f.controller.accept(f.result)
                model.skip()
                assertFalse(model.state.value.completed)
                assertFalse(model.state.value.busy)
                assertNotNull(model.state.value.failure)
                assertEquals(Gate.Walkthrough, f.gate())
                assertTrue(f.outbox.pending().isEmpty())
                var attempts = 0
                val retry =
                    WalkthroughModel(complete = {
                        if (++attempts == 1) throw StoreFailure.Unavailable()
                        f.outbox.enqueue(OutboxChange.Walkthrough)
                    })
                retry.skip()
                assertFalse(retry.state.value.completed)
                retry.skip()
                assertTrue(retry.state.value.completed)
                assertNull(retry.state.value.failure)
            }
        }

    @Test fun pendingLegacyRowOverridesAStaleFlagButKeepsVerificationAndTermsFirst() =
        runTest(timeout = 60.seconds) {
            withOnboardingFixture { f ->
                f.walkthroughModel().skip()
                val account = f.account.account()!!.copy(walkthroughSeen = false)
                f.account.upsert(account)
                assertEquals(Gate.App, f.gate())
                f.account.upsert(account.copy(hasVerifiedEmail = false))
                assertEquals(Gate.Verify, f.gate())
                f.account.upsert(account)
                f.settings.upsert(
                    SettingsRecord(
                        "policies",
                        ApiJson.response.encodeToString(AccountPolicies.serializer(), AccountPolicies("new", "1")),
                    ),
                )
                assertEquals(Gate.Terms, f.gate())
            }
        }

    @Test fun cancellationIsRethrownAndBusyResets() =
        runTest(timeout = 60.seconds) {
            val entered = CompletableDeferred<Unit>()
            val model =
                WalkthroughModel(complete = {
                    entered.complete(Unit)
                    awaitCancellation()
                })
            val job =
                launch {
                    model.skip()
                    fail("caller cancellation returned normally")
                }
            entered.await()
            assertTrue(model.state.value.busy)
            job.cancelAndJoin()
            assertFalse(model.state.value.busy)
            assertFalse(model.state.value.completed)
            assertNull(model.state.value.failure)
        }

    @Test fun stalePermitIsASessionChangeNotACancellation() =
        runTest(timeout = 60.seconds) {
            val model = WalkthroughModel(complete = { throw StaleSessionException() })
            model.skip()
            assertTrue(model.state.value.failure is StaleSessionException)
            assertFalse(model.state.value.completed)
        }
}
