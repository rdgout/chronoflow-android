@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.onboarding.terms

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.session.AccountReadinessRepository
import net.chronoflow.core.data.session.SessionAccountDatabase
import net.chronoflow.core.data.sync.SqlSyncDatabase
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.model.session.SyncEpoch
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.sync.FullPull
import net.chronoflow.core.sync.OutboxFlusher
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionTeardown
import net.chronoflow.feature.onboarding.support.onboardingNow
import net.chronoflow.feature.onboarding.support.withOnboardingFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class TermsAcceptanceTests(
    private val seen: Boolean,
) {
    @Test fun offlineAcceptanceOpensNextGateAndCanSyncLater() =
        runTest(timeout = 60.seconds) {
            withOnboardingFixture(seen = seen, termsPending = true) { f ->
                assertEquals(Gate.Terms, f.gate())
                f.source.setOffline(true)
                val model = f.termsModel()
                model.accept()
                assertTrue(model.state.value.completed)
                assertFalse(model.state.value.failed)
                assertFalse(model.state.value.busy)
                val next = if (seen) Gate.App else Gate.Walkthrough
                assertEquals(next, f.gate())
                val row = f.outbox.pending().single()
                assertEquals("settings", row.kind)
                assertEquals(OutboxChange.Policies(f.result.account.policies.termsVersion), OutboxPayload.change(row))
                model.accept()
                f.outbox.enqueue(OutboxChange.Policies(f.result.account.policies.termsVersion))
                assertEquals(listOf(row.id), f.outbox.pending().map { it.id })
                val flush = OutboxFlusher(f.sync, f.source, now = { onboardingNow })
                assertTrue(flush.run(f.epoch).offline)
                assertEquals(next, f.gate())
                f.source.setOffline(false)
                flush.run(f.epoch)
                assertTrue(f.outbox.pending().isEmpty())
                assertEquals(
                    f.result.account.policies.termsVersion,
                    f.source
                        .fetchIdentity()
                        .policies.acceptedTermsVersion,
                )
                if (!seen) {
                    f.outbox.enqueue(OutboxChange.Walkthrough, f.permit)
                    assertEquals(Gate.App, f.gate())
                }
            }
        }

    @Test fun relaunchSessionUpsertAndStaleFullPullKeepPendingAcceptance() =
        runTest(timeout = 60.seconds) {
            withOnboardingFixture(termsPending = true) { f ->
                f.termsModel().accept()
                val ids = f.outbox.pending().map { it.id }
                f.accounts.close()
                val accounts =
                    SessionAccountDatabase({ f.root }, StorageNamespace.Mock, JdbcAccountDriverFactory(), f.io)
                val restored = SessionController(f.credentials, accounts, SessionTeardown(sleep = {}), f.io)
                try {
                    assertEquals(SessionPresence.SignedIn(f.result.account.id), restored.restore())
                    val database = checkNotNull(accounts.activeAccountDatabase())
                    val readiness = AccountReadinessRepository(database)
                    assertEquals(Gate.Walkthrough, readiness.values(f.result.account.id).first()?.gate)
                    accounts.upsert(f.result.account)
                    val snapshot = checkNotNull(restored.activeSession())
                    FullPull(SqlSyncDatabase(database), f.source, now = { onboardingNow })
                        .run(SyncEpoch(AccountID.of(f.result.account.id), snapshot.epoch))
                    assertEquals(Gate.Walkthrough, readiness.values(f.result.account.id).first()?.gate)
                    assertEquals(ids, OutboxRepository(database).pending().map { it.id })
                } finally {
                    withContext(NonCancellable) {
                        restored.close()
                        val field =
                            SessionController::class.java
                                .getDeclaredField(
                                    "scope",
                                ).apply { isAccessible = true }
                        (field.get(restored) as CoroutineScope).coroutineContext.job.cancelAndJoin()
                        accounts.close()
                    }
                }
            }
        }

    @Test fun failedOutboxInsertRollsBackAcceptanceAndShowsRetry() =
        runTest(timeout = 60.seconds) {
            withOnboardingFixture(termsPending = true) { f ->
                f.database.write {
                    it.driver.execute(
                        null,
                        "CREATE TRIGGER fail_terms BEFORE INSERT ON outbox " +
                            "BEGIN SELECT RAISE(ABORT, 'fixture'); END",
                        0,
                    )
                }
                val model = f.termsModel()
                model.accept()
                assertTrue(model.state.value.failed)
                assertFalse(model.state.value.completed)
                assertTrue(model.state.value.canAccept)
                assertEquals(Gate.Terms, f.gate())
                assertTrue(f.outbox.pending().isEmpty())
                f.database.write { it.driver.execute(null, "DROP TRIGGER fail_terms", 0) }
                model.accept()
                assertTrue(model.state.value.completed)
                assertFalse(model.state.value.failed)
                assertEquals(Gate.Walkthrough, f.gate())
            }
        }

    @Test fun newerServerVersionStillRequiresAcceptanceAndRejectsStaleScreen() =
        runTest(timeout = 60.seconds) {
            withOnboardingFixture(termsPending = true) { f ->
                val stale = f.termsModel()
                f.termsModel().accept()
                val policies = AccountPolicies("new", "new")
                f.accounts.upsert(f.result.account.copy(policies = policies))
                assertEquals(Gate.Terms, f.gate())
                stale.accept()
                assertTrue(stale.state.value.failed)
                assertFalse(stale.state.value.completed)
                assertEquals(1, f.outbox.pending().size)
                val current = TermsUpdateModel(policies) { f.outbox.enqueue(OutboxChange.Policies(it), f.permit) }
                current.accept()
                assertTrue(current.state.value.completed)
                assertEquals(Gate.Walkthrough, f.gate())
            }
        }

    @Test fun revokedPermitCannotAcceptForAnotherSession() =
        runTest(timeout = 60.seconds) {
            withOnboardingFixture(termsPending = true) { f ->
                val model = f.termsModel()
                f.controller.signOut(discardPending = true)
                model.accept()
                assertFalse(model.state.value.completed)
                assertNull(f.controller.activeSession())
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "seen={0}")
        fun seen() = listOf(arrayOf(false), arrayOf(true))
    }
}
