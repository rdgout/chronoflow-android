package net.chronoflow.core.datamock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.ProfileChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.SignInResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds

internal class FailingMockStore : MockStateStore {
    val memory = MemoryMockStateStore()
    private var fail = false
    private var remainingSuccessfulSaves: Int? = null

    @Synchronized fun failAfter(saves: Int?) {
        remainingSuccessfulSaves = saves
    }

    var loads: Int = 0
        @Synchronized get
        private set

    @Synchronized fun setFail(value: Boolean) {
        fail = value
    }

    @Synchronized override fun load(): MockServerState? {
        assertEquals("mock-io", Thread.currentThread().name)
        loads++
        return memory.load()
    }

    @Synchronized override fun save(state: MockServerState) {
        assertEquals("mock-io", Thread.currentThread().name)
        if (fail || remainingSuccessfulSaves == 0) throw DataFailure.Server(null)
        remainingSuccessfulSaves = remainingSuccessfulSaves?.minus(1)
        memory.save(state)
    }
}

class MockPersistenceTests {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun lazyLoadOffMainAndSaveFailurePreservesState() =
        runTest(timeout = 60.seconds) {
            Executors.newSingleThreadExecutor { Thread(it, "mock-io") }.asCoroutineDispatcher().use { io ->
                val store = FailingMockStore()
                val source = mockSource(store = store, io = io)
                assertEquals(0, store.loads)
                login(source)
                assertEquals(1, store.loads)
                store.setFail(true)
                failure { source.send(OutboxChange.Profile(ProfileChange("Lost", "demo@example.com"))) }
                store.setFail(false)
                assertEquals("Demo Traveller", source.fetchIdentity().name)
                store.failAfter(1)
                failure { source.send(OutboxChange.Profile(ProfileChange("Lost", "demo@example.com"))) }
                store.failAfter(null)
                assertEquals("Demo Traveller", source.fetchIdentity().name)
            }
        }

    @Test fun restartPersistsAuthInboxSettingsChecksAndLimits() =
        runTest(timeout = 60.seconds) {
            val root = temporary.root
            val store = FileMockStateStore(root)
            val clock = MockClock()
            val source = mockSource(clock, store, io = Dispatchers.IO)
            assertFalse(store.file.exists())
            val session = login(source)
            source.send(OutboxChange.Walkthrough)
            source.send(
                OutboxChange.Settings(
                    SettingsSection.Preferences,
                    JsonObject(
                        mapOf("time_format" to JsonPrimitive("12")),
                    ),
                ),
            )
            source.requestPasswordReset("demo@example.com")
            val restarted = mockSource(clock, store, MockServerState(), bearer = { session.token }, io = Dispatchers.IO)
            assertTrue(restarted.fetchIdentity().walkthroughSeen)
            assertEquals(JsonPrimitive("12"), restarted.fetchAccount().settings.preferences["timeFormat"])
            assertEquals(1, restarted.inbox().size)
            assertEquals(1, restarted.fetchTrips().size)
            repeat(4) { source.signIn(passwordInput()) }
            assertEquals(
                DataFailure.Throttled(60.0, 429),
                failure { login(mockSource(clock, store, io = Dispatchers.IO)) },
            )
            assertTrue(store.file.path.contains("ChronoFlowMock/Server"))
            assertFalse(File(root, "ChronoFlow/Accounts").exists())
        }

    @Test fun corruptStateNeverFallsBackToSeed() =
        runTest(timeout = 60.seconds) {
            val store = FileMockStateStore(temporary.root)
            store.file.parentFile.mkdirs()
            store.file.writeText("corrupt")
            assertEquals(DataFailure.Server(null), failure { login(mockSource(store = store, io = Dispatchers.IO)) })
            assertEquals("corrupt", store.file.readText())
        }

    @Test fun concurrentRegistrationsKeepAllAccounts() =
        runTest(timeout = 60.seconds) {
            val clock = MockClock()
            val source = mockSource(clock)
            withContext(Dispatchers.Default) {
                coroutineScope {
                    repeat(
                        5,
                    ) { index -> launch { source.signUp(registration("new$index@example.com")) } }
                }
            }
            assertEquals(5, source.inbox().size)
            clock.advance(60.0)
            repeat(5) { index ->
                val result =
                    source.signIn(
                        passwordInput(email = "new$index@example.com", password = newPassword),
                    ) as SignInResult.Session
                assertEquals("new$index@example.com", result.result.account.email)
            }
        }
}
