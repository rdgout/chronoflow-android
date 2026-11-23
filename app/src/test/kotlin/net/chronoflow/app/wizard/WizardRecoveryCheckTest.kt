@file:Suppress("ktlint:standard:max-line-length")
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.wizard

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.app.AppDispatchers
import net.chronoflow.core.wizard.model.MemberWizardRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

class WizardRecoveryCheckTest {
    @Test fun liveRecoveryOpensOnTheFirstCall() =
        runTest(timeout = 60.seconds) {
            val io = StandardTestDispatcher(testScheduler)
            val check = WizardRecoveryCheck(AppDispatchers(io, io, io), { true }, { null })
            assertTrue(check.shouldOpen(1))
        }

    @Test fun secondCallInTheSameEpochDoesNotReadAgain() =
        runTest(timeout = 60.seconds) {
            val io = StandardTestDispatcher(testScheduler)
            var reads = 0
            val check =
                WizardRecoveryCheck(AppDispatchers(io, io, io), {
                    reads++
                    true
                }, { null })
            assertTrue(check.shouldOpen(1))
            assertFalse(check.shouldOpen(1))
            assertEquals(1, reads)
        }

    @Test fun aNewEpochChecksOnceMore() =
        runTest(timeout = 60.seconds) {
            val io = StandardTestDispatcher(testScheduler)
            var reads = 0
            val check =
                WizardRecoveryCheck(AppDispatchers(io, io, io), {
                    reads++
                    true
                }, { null })
            assertTrue(check.shouldOpen(1))
            assertTrue(check.shouldOpen(2))
            assertFalse(check.shouldOpen(2))
            assertFalse(check.shouldOpen(1))
            assertEquals(2, reads)
        }

    @Test fun expiredRecoveryIsDeletedByTheStore() =
        runTest(timeout = 60.seconds) {
            val io = StandardTestDispatcher(testScheduler)
            var deleted = false
            val check =
                WizardRecoveryCheck(AppDispatchers(io, io, io), {
                    if (MemberWizardRecord.isExpired(0, MemberWizardRecord.EXPIRY_MILLIS)) {
                        deleted = true
                        false
                    } else {
                        true
                    }
                }, { null })
            assertFalse(check.shouldOpen(1))
            assertTrue(deleted)
        }

    @Test fun captureSeedSkipsTheStore() =
        runTest(timeout = 60.seconds) {
            val io = StandardTestDispatcher(testScheduler)
            val check =
                WizardRecoveryCheck(
                    AppDispatchers(io, io, io),
                    { error("capture must not read recovery") },
                    { "WizardWhere" },
                )
            assertFalse(check.shouldOpen(1))
        }

    @Test fun theChecksOwnWorkRunsOnInjectedIo() =
        runTest(timeout = 60.seconds) {
            val marker = ThreadLocal<Boolean>()
            val delegate = StandardTestDispatcher(testScheduler)
            val io =
                object : CoroutineDispatcher() {
                    override fun dispatch(
                        context: CoroutineContext,
                        block: Runnable,
                    ) {
                        delegate.dispatch(context) {
                            marker.set(true)
                            try {
                                block.run()
                            } finally {
                                marker.remove()
                            }
                        }
                    }
                }
            val check =
                WizardRecoveryCheck(AppDispatchers(delegate, io, delegate), {
                    assertSame(io, currentCoroutineContext()[ContinuationInterceptor])
                    true
                }, {
                    assertEquals(true, marker.get())
                    null
                })
            assertTrue(check.shouldOpen(1))
        }
}
