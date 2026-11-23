package net.chronoflow.debug

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.session.SessionAccountDatabase
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.sync.session.MemorySessionStore
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionTeardown
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class TermsPendingSeedTest {
    @Test fun termsChangesPendingSeedsOneRowOnce() =
        runTest(timeout = 60.seconds) {
            val io = StandardTestDispatcher(testScheduler)
            val root = withContext(io) { TestRoots.temporaryStoreRoot() }
            val credentials = MemorySessionStore()
            val seed = MockAppSeed.make(Instant.parse("2026-11-20T12:00:00Z"))
            val source = MockDataSource(seed = seed, sleep = {}, io = io)
            val factory = JdbcAccountDriverFactory()
            val accounts = SessionAccountDatabase({ root }, StorageNamespace.Mock, factory, io)
            val controller = SessionController(credentials, accounts, SessionTeardown(sleep = {}), io)
            try {
                SeedRunner(root, credentials, factory, source, io)
                    .reseed(SeedAccount.Demo, "019a562b-8000-7000-8000-000000000001", termsChangesPending = true)
                controller.restore()
                val database = checkNotNull(accounts.activeAccountDatabase())
                val snapshot = checkNotNull(controller.activeSession())
                val permit = checkNotNull(controller.mutationPermit(snapshot))
                val outbox = OutboxRepository(database)
                val first = outbox.pending().single()
                assertEquals("settings", first.kind)
                val change = OutboxPayload.change(first) as OutboxChange.Settings
                assertEquals(SettingsSection.Preferences, change.section)
                seedTermsChangesPending(database, permit)
                seedTermsChangesPending(database, permit)
                assertEquals(listOf(first.id), outbox.pending().map { it.id })
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
}
