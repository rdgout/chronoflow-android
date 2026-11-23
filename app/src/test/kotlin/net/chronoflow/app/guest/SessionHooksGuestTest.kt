package net.chronoflow.app.guest

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.data.session.SessionHooks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SessionHooksGuestTest {
    @Test fun onForegroundSweepsAnExpiredGuestDraft() =
        runTest(timeout = 60.seconds) {
            val f = GuestFixture(this)
            var calls = 0
            val next =
                object : SessionHooks {
                    override suspend fun onForeground() {
                        calls++
                        assertFalse(f.file.exists())
                    }

                    override suspend fun onSessionEnding() {
                        calls++
                    }
                }
            val hooks = GuestSessionHooks(f.store, next)
            try {
                f.store.start()
                f.clock.advance(1860.0)
                hooks.onForeground()
                hooks.onSessionEnding()
                assertEquals(2, calls)
                f.file.mkdirs()
                GuestSessionHooks(
                    f.store,
                    object : SessionHooks {
                        override suspend fun onForeground() {
                            calls++
                        }

                        override suspend fun onSessionEnding() { }
                    },
                ).onForeground()
                assertEquals(3, calls)
            } finally {
                f.root.deleteRecursively()
            }
        }
}
