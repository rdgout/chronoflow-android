package net.chronoflow.debug

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.session.SessionAccountDatabase
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.contracts.TripDetailResponse
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.sync.session.MemorySessionStore
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionTeardown
import net.chronoflow.core.testing.junit.measureClassWarmup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class SettingsTestSeedsTest {
    @Test fun seedCreatesOneRealCheckOffAfterTheFullPull() =
        runTest(timeout = 60.seconds) {
            withSeedFixture(pending = true) { accounts ->
                val database = checkNotNull(accounts.activeAccountDatabase())
                val row = OutboxRepository(database).pending().single()
                val change = OutboxPayload.change(row) as OutboxChange.CheckOff
                assertTrue(TripRepository(database).plan(change.change.tripID, change.change.planInputHash) != null)
            }
        }

    @Test fun pendingKeyWithoutAuthTestIdIsIgnored() =
        runTest(timeout = 60.seconds) {
            val seed = TestSeed.parse(mapOf("CF_SETTINGS_TEST_PENDING" to "1"))
            assertFalse(seed.settingsPending)
            assertFalse(seed.mutationsOffline)
            withSeedFixture(pending = seed.settingsPending) { accounts ->
                assertTrue(OutboxRepository(checkNotNull(accounts.activeAccountDatabase())).pending().isEmpty())
            }
        }

    @Test fun anotherAccountEmailReceivesNoPendingRow() =
        runTest(timeout = 60.seconds) {
            withSeedFixture(pending = true, account = SeedAccount.TwoFactor) { accounts ->
                assertTrue(OutboxRepository(checkNotNull(accounts.activeAccountDatabase())).pending().isEmpty())
            }
        }

    @Test fun noActionableStepThrowsTheSpecifiedMessage() =
        runTest(timeout = 60.seconds) {
            try {
                withSeedFixture(pending = true, noActions = true) {
                    fail("The seed must reject cached plans without an actionable step")
                }
                fail("Expected the seed failure")
            } catch (failure: IllegalStateException) {
                assertEquals("settings pending seed failed: no cached actionable step", failure.message)
            }
        }

    companion object {
        private val warm by lazy {
            runTest(timeout = 60.seconds) { withSeedFixture(pending = false) {} }
        }

        @JvmStatic @BeforeClass
        fun warmFixtureOncePerJvm() {
            measureClassWarmup(SettingsTestSeedsTest::class.java, 0, "jdbc-and-seeded-full-pull") { warm }
        }
    }
}

private suspend fun TestScope.withSeedFixture(
    pending: Boolean,
    account: SeedAccount = SeedAccount.Demo,
    noActions: Boolean = false,
    block: suspend (SessionAccountDatabase) -> Unit,
) {
    val io = StandardTestDispatcher(testScheduler)
    val root = withContext(io) { TestRoots.temporaryStoreRoot() }
    val credentials = MemorySessionStore()
    val base = MockDataSource(seed = MockAppSeed.make(Instant.parse("2026-11-20T12:00:00Z")), sleep = {}, io = io)
    val source =
        if (noActions) {
            object : DataSource by base {
                override suspend fun fetchTripDetail(id: String): TripDetailResponse {
                    val detail = base.fetchTripDetail(id).detail
                    return TripDetailResponse.encoding(detail.copy(plan = detail.plan.copy(blocks = emptyList())))
                }
            }
        } else {
            base
        }
    val factory = JdbcAccountDriverFactory()
    val accounts = SessionAccountDatabase({ root }, StorageNamespace.Mock, factory, io)
    val controller = SessionController(credentials, accounts, SessionTeardown(sleep = {}), io)
    try {
        SeedRunner(root, credentials, factory, source, io).reseed(
            account,
            "019a562b-8000-7000-8000-000000000001",
            settingsPending = pending,
        )
        controller.restore()
        block(accounts)
    } finally {
        withContext(NonCancellable) {
            controller.close()
            val field = SessionController::class.java.getDeclaredField("scope").apply { isAccessible = true }
            (field.get(controller) as CoroutineScope).coroutineContext.job.cancelAndJoin()
            accounts.close()
            withContext(io) { check(root.deleteRecursively()) }
        }
    }
}
