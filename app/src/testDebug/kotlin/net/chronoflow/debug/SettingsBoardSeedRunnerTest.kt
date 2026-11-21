package net.chronoflow.debug

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.datamock.FileMockStateStore
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.datamock.MockScenarioValues
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.sync.session.MemorySessionStore
import net.chronoflow.core.testing.junit.measureClassWarmup
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class SettingsBoardSeedRunnerTest {
    @Test fun fixtureIsAppliedBeforeSignInAndTheFullPullStoresIt() =
        runTest(timeout = 60.seconds) {
            val io = StandardTestDispatcher(testScheduler)
            val root = withContext(io) { TestRoots.temporaryStoreRoot() }
            val now = Instant.parse("2026-11-21T12:00:00Z")
            val seed =
                TestSeed.parse(
                    mapOf(
                        TestSeed.TEST_ID_KEY to "019a562b-8000-7000-8000-000000000082",
                        "CF_SETTINGS_BOARD_STATE" to "bodyMissing",
                        "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                    ),
                )
            val factory = JdbcAccountDriverFactory()
            val source =
                MockDataSource(
                    FileMockStateStore(root),
                    seed = MockAppSeed.make(now, walkthroughSeen = true),
                    sleep = {},
                    io = io,
                )
            val database = AccountDatabase(root, StorageNamespace.Mock, factory, io)
            try {
                SeedRunner(
                    root,
                    MemorySessionStore(),
                    factory,
                    source,
                    io,
                    beforeSignIn = { SettingsBoardCaptureFixture.prepare(root, seed, now, io) },
                ).reseed(SeedAccount.Demo, checkNotNull(seed.testId))
                database.open(MockScenarioValues.demoID)
                val rows = SettingsRepository(database).settings().associate { it.section to it.value }
                val body = Json.parseToJsonElement(checkNotNull(rows["bodyClock"])) as JsonObject
                val readiness = Json.parseToJsonElement(checkNotNull(rows["readiness"])) as JsonObject
                assertEquals(JsonNull, body["ageBand"])
                assertEquals(JsonNull, body["shiftWork"])
                assertEquals(JsonPrimitive("needs-answers"), readiness["variant"])
            } finally {
                withContext(NonCancellable) {
                    try {
                        database.close()
                    } finally {
                        withContext(io) { check(root.deleteRecursively()) }
                    }
                }
            }
        }

    companion object {
        @JvmStatic @BeforeClass
        fun warmJdbc() {
            measureClassWarmup(SettingsBoardSeedRunnerTest::class.java, 0, "jdbc") {
                Class.forName("org.sqlite.JDBC")
            }
        }
    }
}
