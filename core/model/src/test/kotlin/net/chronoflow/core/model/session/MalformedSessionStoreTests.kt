package net.chronoflow.core.model.session

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class MalformedSessionStoreTests(
    private val json: String,
) {
    @Test fun malformedSessionsAreDeletedAndSignedOut() =
        runTest(timeout = 60.seconds) {
            val store = MemorySessionStore(json.encodeToByteArray())
            assertEquals(SessionPresence.SignedOut, store.loadSession())
            assertEquals(1, store.deletes)
            assertNull(store.read())
            assertEquals(SessionPresence.SignedOut, store.loadSession())
            assertEquals(1, store.deletes)
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}")
        fun cases() =
            listOf(
                "",
                "null",
                "[]",
                "{}",
                "{bad",
                """{"token":"a"}""",
                """{"token":12,"accountId":"a"}""",
                """{"token":"","accountId":"a"}""",
                """{"token":"a","accountId":"  "}""",
                """{"token":"a","accountID":"a"}""",
            )
    }
}
