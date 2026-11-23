package net.chronoflow.debug

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.datamock.MemoryMockStateStore
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.model.session.readStoredSession
import net.chronoflow.core.sync.session.MemorySessionStore
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class ReseedEqualsColdSeedTest {
    @Test fun productionSeedRunnerReseedEqualsTheColdSeed() =
        runTest(timeout = 60.seconds) {
            val io = StandardTestDispatcher(testScheduler)
            val root = withContext(io) { TestRoots.temporaryStoreRoot() }
            val credentials = MemorySessionStore()
            val source =
                MockDataSource(
                    MemoryMockStateStore(),
                    seed = MockAppSeed.make(Instant.ofEpochSecond(1_800_000_000), walkthroughSeen = true),
                    delay = { 0.0 },
                    sleep = {},
                    bearer = { credentials.readStoredSession()?.token },
                    io = io,
                )
            val secrets =
                net.chronoflow.core.model.guest
                    .MemoryGuestSecretStore()
            val runner =
                SeedRunner(
                    root,
                    credentials,
                    JdbcAccountDriverFactory(),
                    source,
                    io,
                    guestSecrets =
                        object : net.chronoflow.core.data.session.GuestSecrets {
                            override suspend fun deleteAll() = secrets.deleteAll()
                        },
                )
            try {
                reseedContract(root, credentials, io) { runner.reseed(it, "019a562b-8000-7000-8000-000000000001") }
                val file = java.io.File(root, "ChronoFlowMock/guest-wizard.json")
                withContext(io) {
                    checkNotNull(file.parentFile).mkdirs()
                    file.writeText("{}")
                    java.io.File(file.path + ".tmp").writeText("{}")
                }
                secrets.set("before-reseed", "guest")
                runner.reseed(null, "019a562b-8000-7000-8000-000000000001")
                org.junit.Assert.assertNull(secrets.get("guest"))
                org.junit.Assert.assertFalse(file.exists())
                org.junit.Assert.assertFalse(java.io.File(file.path + ".tmp").exists())
            } finally {
                withContext(io) { check(root.deleteRecursively()) }
            }
        }
}
