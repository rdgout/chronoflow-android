@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.sync.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.core.model.launch.SignedOutReadiness
import net.chronoflow.core.model.session.SignOutResult
import net.chronoflow.core.testing.sync.TestGate
import net.chronoflow.core.testing.sync.eventually
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SessionGateRouterTests {
    private class Harness(
        scope: TestScope,
        teardown: SessionTeardown = SessionTeardown(sleep = { awaitCancellation() }),
    ) {
        val credentials = MemorySessionStore()
        val controller =
            SessionController(
                credentials,
                RecordingAccountStore(),
                teardown,
                StandardTestDispatcher(scope.testScheduler),
            )
        val gates = MutableSharedFlow<Gate>(replay = 1)
        val routes = mutableListOf<String>()
        val verifyEpochs = mutableListOf<Long>()
        val readiness =
            object : LaunchReadiness by SignedOutReadiness {
                override fun gateUpdates() = gates
            }
        val router =
            SessionGateRouter(
                controller,
                readiness,
                scope.backgroundScope,
                onSignedOut = { routes += "signIn" },
                onGate = { routes += it.rawValue },
                onVerify = {
                    verifyEpochs += it
                    routes += "verify"
                },
            )

        fun close() {
            router.stop()
            controller.close()
        }
    }

    @Test fun gatesContinueAfterAcceptanceAndCancelAtSignOut() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this)
            try {
                h.controller.accept(sessionResult())
                h.router.start()
                h.gates.emit(Gate.Verify)
                assertTrue(eventually { h.routes.lastOrNull() == "verify" })
                assertEquals(listOf(h.controller.epoch), h.verifyEpochs)
                h.gates.emit(Gate.Terms)
                assertTrue(eventually { h.routes.lastOrNull() == "terms" })
                h.gates.emit(Gate.Walkthrough)
                assertTrue(eventually { h.routes.lastOrNull() == "walkthrough" })
                h.gates.emit(Gate.App)
                assertTrue(eventually { h.routes.lastOrNull() == "app" })
                h.controller.signOut(true)
                assertTrue(eventually { h.routes.lastOrNull() == "signIn" })
                assertEquals(listOf("verify", "terms", "walkthrough", "app", "signIn"), h.routes)
                h.gates.emit(Gate.Terms)
                runCurrent()
                assertEquals(5, h.routes.size)
            } finally {
                h.close()
            }
        }

    @Test fun signedOutRoutesAndRevokesMutationsBeforeLogoutFinishes() =
        runTest(timeout = 60.seconds) {
            val logout = TestGate()
            val h = Harness(this, SessionTeardown(sleep = { awaitCancellation() }, logout = { logout.block() }))
            try {
                h.controller.accept(sessionResult())
                val snapshot = h.controller.activeSession()!!
                val permit = h.controller.mutationPermit(snapshot)!!
                assertTrue(permit.whileValid { true })
                h.gates.emit(Gate.App)
                h.router.start()
                assertTrue(eventually { h.routes.lastOrNull() == "app" })
                val signOut = backgroundScope.async { h.controller.signOut(true) }
                logout.waitUntilBlocked()
                assertTrue(eventually { h.routes.lastOrNull() == "signIn" })
                try {
                    permit.whileValid { fail("revoked mutation ran") }
                    fail("expected revoked permit")
                } catch (
                    _: CancellationException,
                ) {
                }
                assertNull(h.controller.mutationPermit(snapshot))
                h.gates.emit(Gate.Terms)
                runCurrent()
                assertEquals("signIn", h.routes.last())
                logout.release()
                assertEquals(SignOutResult.SignedOut, signOut.await())
            } finally {
                h.close()
            }
        }

    @Test fun initiallySignedOutKeepsWelcomeRoute() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this)
            try {
                h.controller.restore()
                h.router.start()
                runCurrent()
                assertTrue(h.routes.isEmpty())
                assertEquals(0, h.gates.subscriptionCount.value)
            } finally {
                h.close()
            }
        }

    /** The router must deduplicate even a readiness provider that sends duplicate gates. */
    @Test fun duplicateGateValuesNavigateOnce() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this)
            try {
                h.controller.accept(sessionResult())
                h.router.start()
                h.gates.emit(Gate.Terms)
                assertTrue(eventually { h.routes.size == 1 })
                h.gates.emit(Gate.Terms)
                runCurrent()
                assertEquals(listOf("terms"), h.routes)
            } finally {
                h.close()
            }
        }

    /** A transition invalidates the active snapshot before publishing its replacement epoch. */
    @Test fun staleSnapshotEndsTheGateJob() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this)
            val write = TestGate()
            try {
                h.controller.accept(sessionResult())
                h.router.start()
                h.gates.emit(Gate.App)
                assertTrue(eventually { h.routes.size == 1 })
                h.credentials.writeGate = write
                val replacement = backgroundScope.async { h.controller.accept(sessionResult()) }
                write.waitUntilBlocked()
                h.gates.emit(Gate.Terms)
                assertTrue(eventually { h.gates.subscriptionCount.value == 0 })
                h.gates.emit(Gate.Walkthrough)
                runCurrent()
                assertEquals(listOf("app"), h.routes)
                write.release()
                replacement.await()
            } finally {
                h.close()
            }
        }

    /** Starting the Android observer twice must retain one session and gate subscription. */
    @Test fun startTwiceCollectsOnce() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this)
            try {
                h.controller.accept(sessionResult())
                h.router.start()
                h.router.start()
                assertTrue(eventually { h.gates.subscriptionCount.value == 1 })
                h.gates.emit(Gate.Terms)
                assertTrue(eventually { h.routes.size == 1 })
                assertEquals(listOf("terms"), h.routes)
            } finally {
                h.close()
            }
        }
}
