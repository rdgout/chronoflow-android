package net.chronoflow.core.model.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

internal class MemorySessionStore(
    var data: ByteArray? = null,
) : SessionStore {
    var deletes = 0

    override suspend fun read() = data

    override suspend fun write(data: ByteArray) {
        this.data = data
    }

    override suspend fun delete() {
        data = null
        deletes++
    }
}

class SessionStoreTests {
    @Test fun nativeSessionRoundTripsWithoutDeleting() =
        runTest(timeout = 60.seconds) {
            val value = StoredSession("secret", "account")
            assertEquals(value, StoredSession.parse(value.encode()))
            val store = MemorySessionStore()
            assertEquals(SessionPresence.SignedOut, store.loadSession())
            store.write(value.encode())
            assertEquals(SessionPresence.SignedIn("account"), store.loadSession())
            assertEquals(0, store.deletes)
        }

    @Test fun toStringRedactsTheToken() {
        assertFalse(StoredSession("secret", "account").toString().contains("secret"))
    }

    @Test fun parseKeepsUntrimmedValues() {
        assertEquals(
            StoredSession(" t ", "a"),
            StoredSession.parse("""{"token":" t ","accountId":"a"}""".encodeToByteArray()),
        )
    }

    @Test fun loadSessionRethrowsCancellation() =
        runTest(timeout = 60.seconds) {
            val store =
                object : SessionStore {
                    override suspend fun read(): ByteArray? = throw CancellationException()

                    override suspend fun write(data: ByteArray) = Unit

                    override suspend fun delete() = Unit
                }
            try {
                store.loadSession()
                fail()
            } catch (_: CancellationException) {
            }
        }
}
