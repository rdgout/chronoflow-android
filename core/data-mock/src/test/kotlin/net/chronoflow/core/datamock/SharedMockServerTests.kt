package net.chronoflow.core.datamock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.DataFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SharedMockServerTests {
    @Test fun cachedSourcesShareMailAndSessionInvalidation() =
        runTest(timeout = 60.seconds) {
            val store = MemoryMockStateStore()
            val first = mockSource(store = store)
            val session = login(first)
            val second =
                MockDataSource(store = store, delay = {
                    0.0
                }, sleep = {}, bearer = { session.token }, zone = mockZone, io = Dispatchers.Unconfined)
            assertEquals(demoID, first.fetchIdentity().id)
            assertEquals(demoID, second.fetchIdentity().id)
            assertTrue(second.inbox().isEmpty())
            first.requestPasswordReset("demo@example.com")
            assertEquals(1, second.inbox().size)
            second.requestPasswordReset("demo@example.com")
            assertEquals(2, first.inbox().size)
            second.signOut()
            assertEquals(DataFailure.Unauthorized(401), failure { first.fetchIdentity() })
        }

    @Test fun concurrentMemoryMutationsRetainEveryMail() =
        runTest(timeout = 60.seconds) {
            val store = MemoryMockStateStore()
            val clock = MockClock()
            val first = mockSource(clock, store)
            first.inbox()
            val second = mockSource(clock, store)
            second.inbox()
            withContext(Dispatchers.Default) {
                coroutineScope {
                    launch { first.requestPasswordReset("demo@example.com") }
                    launch { second.requestPasswordReset("twofactor@example.com") }
                }
            }
            val mail = first.inbox()
            assertEquals(2, mail.size)
            assertEquals(mail, second.inbox())
        }
}
