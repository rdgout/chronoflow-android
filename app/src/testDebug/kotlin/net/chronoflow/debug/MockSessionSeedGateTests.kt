@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.debug

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.session.AccountReadinessRepository
import net.chronoflow.core.data.session.SessionAccountDatabase
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.datamock.FileMockStateStore
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.datamock.MockScenarioValues
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.model.session.readStoredSession
import net.chronoflow.core.sync.session.MemorySessionStore
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionTeardown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class MockSessionSeedGateTests(
    private val name: String,
    private val account: SeedAccount,
    private val gate: Gate,
) {
    @Test fun signedInSeedUsesRestorableCredentialsAndAccountGates() =
        runTest(timeout = 60.seconds) {
            val io = StandardTestDispatcher(testScheduler)
            val root = withContext(io) { TestRoots.temporaryStoreRoot() }
            val credentials = MemorySessionStore()
            val source =
                MockDataSource(
                    FileMockStateStore(root),
                    seed =
                        MockAppSeed.make(
                            Instant.ofEpochSecond(1_800_000_000),
                            unverified = gate == Gate.Verify,
                            walkthroughSeen = gate != Gate.Walkthrough,
                            termsPending =
                                gate == Gate.Terms,
                        ),
                    delay = { 0.0 },
                    sleep = {},
                    bearer = { credentials.readStoredSession()?.token },
                    io = io,
                )
            val input =
                PasswordSignInInput(
                    if (account ==
                        SeedAccount.Demo
                    ) {
                        MockScenarioValues.demoEmail
                    } else {
                        MockScenarioValues.twoFactorEmail
                    },
                    MockScenarioValues.password,
                    "019a562b-8000-7000-8000-000000000001",
                    "UI test",
                )
            val accounts = SessionAccountDatabase({ root }, StorageNamespace.Mock, JdbcAccountDriverFactory(), io)
            val controller = SessionController(credentials, accounts, SessionTeardown(sleep = {}), io)
            try {
                MockSessionSeed.run(
                    root,
                    StorageNamespace.Mock,
                    credentials,
                    source,
                    input,
                    MockScenarioValues.twoFactorCode,
                    JdbcAccountDriverFactory(),
                    io,
                )
                val stored = checkNotNull(credentials.readStoredSession())
                assertEquals(
                    if (account ==
                        SeedAccount.Demo
                    ) {
                        MockScenarioValues.demoID
                    } else {
                        MockScenarioValues.twoFactorID
                    },
                    stored.accountId,
                )
                assertEquals(SessionPresence.SignedIn(stored.accountId), controller.restore())
                val db = checkNotNull(accounts.activeAccountDatabase())
                val value = checkNotNull(AccountReadinessRepository(db).values(stored.accountId).first())
                assertEquals(gate, value.gate)
                assertEquals(gate != Gate.Verify, value.hasStoredData)
                val restarted =
                    MockDataSource(
                        FileMockStateStore(root),
                        delay = { 0.0 },
                        sleep = {},
                        bearer = { stored.token },
                        io = io,
                    )
                assertEquals(stored.accountId, restarted.fetchIdentity().id)
                accounts.close()
                credentials.delete()
                MockSessionSeed.run(
                    root,
                    StorageNamespace.Mock,
                    credentials,
                    source,
                    input,
                    MockScenarioValues.twoFactorCode,
                    JdbcAccountDriverFactory(),
                    io,
                )
                assertNull(credentials.readStoredSession())
            } finally {
                controller.close()
                accounts.close()
                withContext(io) { check(root.deleteRecursively()) }
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            SeedAccount.entries.flatMap { account ->
                listOf(Gate.App, Gate.Verify, Gate.Terms, Gate.Walkthrough).map { gate ->
                    arrayOf<Any>("${account.raw}-$gate", account, gate)
                }
            }
    }
}
