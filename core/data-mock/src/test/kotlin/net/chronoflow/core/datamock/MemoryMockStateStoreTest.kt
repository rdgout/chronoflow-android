package net.chronoflow.core.datamock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.api.ResetPasswordInput
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/** Android shared-store atomicity coverage (new). */
class MemoryMockStateStoreTest {
    @Test fun concurrentUpdatesKeepEveryMail() =
        runTest(timeout = 60.seconds) {
            val store = MemoryMockStateStore()
            val initial = MockServerState()
            coroutineScope {
                repeat(8) { worker ->
                    launch(Dispatchers.Default) {
                        repeat(50) { index ->
                            val mail =
                                MockMail.PasswordReset(
                                    "$worker-$index",
                                    "demo@example.com",
                                    0.0,
                                    ResetPasswordInput("artificial-token", "demo@example.com", "", ""),
                                )
                            store.update(initial) { state -> state.copy(mail = state.mail + mail) to Unit }
                        }
                    }
                }
            }
            val mail = store.load()!!.mail
            assertEquals(400, mail.size)
            assertEquals(400, mail.toSet().size)
        }
}
