@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.app

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.launch.SignedOutReadiness
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.sync.session.MemorySessionStore
import net.chronoflow.core.sync.session.ReadinessValues
import net.chronoflow.core.sync.session.RecordingAccountStore
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionTeardown
import net.chronoflow.core.sync.session.StoredDataReadiness
import net.chronoflow.core.testing.sync.eventually
import net.chronoflow.core.testing.sync.syncAccount
import net.chronoflow.feature.launch.model.LaunchCoordinator
import net.chronoflow.feature.launch.model.LaunchRoute
import net.chronoflow.feature.launch.model.ManualLaunchClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class StoredDataReadinessLaunchTests {
    private class Harness(
        scope: TestScope,
        val values: ReadinessValues = ReadinessValues(),
        signedIn: Boolean = true,
    ) {
        val credentials = MemorySessionStore(if (signedIn) StoredSession("test-token", syncAccount).encode() else null)
        val controller =
            SessionController(
                credentials,
                RecordingAccountStore(),
                SessionTeardown(sleep = {
                    awaitCancellation()
                }),
                StandardTestDispatcher(scope.testScheduler),
            )
        val readiness = StoredDataReadiness(controller, values, StandardTestDispatcher(scope.testScheduler))
        val clock = ManualLaunchClock()
        val coordinator =
            LaunchCoordinator(readiness, clock, false, scope = scope.backgroundScope, onChange = {
                _,
                _,
                ->
            })

        fun close() {
            coordinator.stop()
            readiness.close()
            controller.close()
        }
    }

    @Test fun freshDataKeepsTwoAndEightSecondRules() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this)
            try {
                h.coordinator.start()
                assertTrue(eventually { h.coordinator.state.hasStoredData != null })
                h.clock.advance(2.seconds)
                assertTrue(eventually { h.coordinator.state.showsStatus })
                assertFalse(h.coordinator.state.isReady)
                h.clock.advance(8.seconds)
                assertTrue(eventually { h.coordinator.state.didForceReady })
                assertTrue(eventually { h.readiness.firstLoadEpoch != null })
                assertEquals(LaunchRoute.Today, h.coordinator.state.route)
                assertFalse(h.readiness.hasStoredData())
            } finally {
                h.close()
            }
        }

    @Test fun verificationIsReadyWithoutMarkerAndSignedOutDoesNotReadDatabase() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this, ReadinessValues(Gate.Verify))
            val signedOut = Harness(this, signedIn = false)
            try {
                h.coordinator.start()
                assertTrue(eventually { h.coordinator.state.gate != null })
                assertEquals(LaunchRoute.Verify, h.coordinator.state.route)
                assertTrue(h.coordinator.state.isReady)
                assertEquals(SessionPresence.SignedOut, signedOut.readiness.session())
                assertEquals(0, signedOut.values.reads)
                assertEquals(SessionPresence.SignedOut, SignedOutReadiness.session())
            } finally {
                h.close()
                signedOut.close()
            }
        }

    @Test fun unknownSessionUsesExistingEightSecondFallbackWithoutDeletingData() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this)
            try {
                h.credentials.readFails = true
                h.coordinator.start()
                assertTrue(eventually { h.readiness.failure != null })
                h.clock.advance(8.seconds)
                assertTrue(eventually { h.coordinator.state.didForceReady })
                assertEquals(SessionPresence.SignedOut, h.coordinator.state.session)
                assertEquals(LaunchRoute.Welcome, h.coordinator.state.route)
                assertNull(h.controller.presence)
                assertNotNull(h.credentials.data)
                assertEquals(0, h.credentials.deletes)
                assertEquals(0, h.values.reads)
            } finally {
                h.close()
            }
        }

    @Test fun verificationDiscoveredDuringFirstPullReleasesLaunchWithoutMarker() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this)
            try {
                h.coordinator.start()
                assertTrue(eventually { h.coordinator.state.gate != null && h.coordinator.state.hasStoredData != null })
                assertFalse(h.coordinator.state.isReady)
                h.values.set(Gate.Verify, false)
                assertTrue(eventually { h.coordinator.state.gate == Gate.Verify })
                assertTrue(h.coordinator.state.isReady)
                assertEquals(LaunchRoute.Verify, h.coordinator.state.route)
                assertFalse(h.coordinator.state.didForceReady)
            } finally {
                h.close()
            }
        }
}
