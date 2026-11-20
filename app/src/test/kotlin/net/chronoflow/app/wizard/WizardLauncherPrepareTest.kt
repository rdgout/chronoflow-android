@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.wizard

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.WizardMode
import net.chronoflow.core.plan.actions.TripAction
import net.chronoflow.core.testing.wizard.GenerationSource
import net.chronoflow.core.testing.wizard.InMemoryWizardDraftStore
import net.chronoflow.core.wizard.model.AirportSearchService
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.RecoveryRejected
import net.chronoflow.core.wizard.model.TripWizardAction
import net.chronoflow.feature.trip.actions.TripWizardPrepareOutcome
import net.chronoflow.feature.wizard.TripWizardDependencies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.GeneralSecurityException
import java.time.Instant
import kotlin.coroutines.ContinuationInterceptor
import kotlin.time.Duration.Companion.seconds

internal fun TestScope.prepareDependencies(start: suspend (String, TripWizardAction) -> MemberWizardDraft) =
    object : TripWizardDependencies {
        override val store = InMemoryWizardDraftStore()
        override val source = GenerationSource()
        override val writerScope = backgroundScope
        override val now = { Instant.EPOCH }
        override val airportSearch = AirportSearchService({ emptyList() }, now, {})

        override suspend fun start() = error("prepare must use startTrip")

        override suspend fun startTrip(
            id: String,
            action: TripWizardAction,
        ) = start(id, action)

        override fun newGeneration() = error("prepare must not create a generation service")
    }

class WizardLauncherPrepareTest {
    @Test fun prepareCallsStartTripOnIoAndReturnsReady() =
        runTest(timeout = 60.seconds) {
            val io = StandardTestDispatcher(testScheduler, name = "wizard-io")
            val deps =
                prepareDependencies { id, action ->
                    assertEquals("t", id)
                    assertEquals(TripWizardAction.edit, action)
                    assertEquals(io, currentCoroutineContext()[ContinuationInterceptor])
                    MemberWizardDraft()
                }
            val launcher = WizardLauncher({ null }, { error("prepare must not push") }, deps, io)
            assertEquals(TripWizardPrepareOutcome.Ready, launcher.prepare("t", TripAction.Edit))
        }

    @Test fun duplicateUsesTheDuplicateAction() =
        runTest(timeout = 60.seconds) {
            val deps =
                prepareDependencies { _, action ->
                    assertEquals(TripWizardAction.duplicate, action)
                    MemberWizardDraft()
                }
            val launcher = WizardLauncher({ null }, {}, deps, StandardTestDispatcher(testScheduler))
            assertEquals(TripWizardPrepareOutcome.Ready, launcher.prepare("t", TripAction.Duplicate))
        }

    private fun failure(
        error: Throwable,
        offline: Boolean,
    ) = runTest(timeout = 60.seconds) {
        val launcher =
            WizardLauncher(
                { null },
                {},
                prepareDependencies { _, _ -> throw error },
                StandardTestDispatcher(testScheduler),
            )
        assertEquals(TripWizardPrepareOutcome.Failed(offline), launcher.prepare("t", TripAction.Edit))
    }

    @Test fun offlineMapsToOfflineFailure() = failure(DataFailure.Offline, true)

    @Test fun serverMapsToOtherFailure() = failure(DataFailure.Server(500), false)

    @Test fun shapeFailureMapsToOtherFailure() = failure(IllegalStateException(), false)

    @Test fun writeFailureMapsToOtherFailure() = failure(java.io.IOException(), false)

    @Test fun sessionFailureMapsToOtherFailure() = failure(SessionFailure.Superseded, false)

    @Test fun identityFailureMapsToOtherFailure() = failure(DeviceIdentityFailure.InvalidPersistedIdentity, false)

    @Test fun storeFailureMapsToOtherFailure() = failure(StoreFailure.Closed, false)

    @Test fun recoveryFailureMapsToOtherFailure() = failure(RecoveryRejected(), false)

    @Test fun securityFailureMapsToOtherFailure() = failure(GeneralSecurityException(), false)

    @Test fun foreignCancellationMapsToOtherFailure() = failure(CancellationException(), false)

    @Test fun cancellationIsRethrown() =
        runTest(timeout = 60.seconds) {
            val gate = CompletableDeferred<Unit>()
            var calls = 0
            val deps =
                prepareDependencies { _, _ ->
                    if (++calls == 1) gate.await()
                    MemberWizardDraft()
                }
            val launcher = WizardLauncher({ null }, {}, deps, StandardTestDispatcher(testScheduler))
            var completed = false
            val caller =
                launch {
                    launcher.prepare("t", TripAction.Edit)
                    completed = true
                }
            runCurrent()
            caller.cancelAndJoin()
            assertTrue(caller.isCancelled)
            assertEquals(false, completed)
            assertEquals(TripWizardPrepareOutcome.Ready, launcher.prepare("t", TripAction.Edit))
        }

    @Test fun ignoredWhileAnOverlayIsOpen() =
        runTest(timeout = 60.seconds) {
            val deps = prepareDependencies { _, _ -> error("overlay blocks the request") }
            val launcher =
                WizardLauncher({ Route.Wizard(WizardMode.Create) }, {}, deps, StandardTestDispatcher(testScheduler))
            assertEquals(TripWizardPrepareOutcome.Ignored, launcher.prepare("t", TripAction.Edit))
        }

    @Test fun ignoredWhilePrepareIsPending() =
        runTest(timeout = 60.seconds) {
            val gate = CompletableDeferred<Unit>()
            var calls = 0
            val launcher =
                WizardLauncher(
                    { null },
                    {},
                    prepareDependencies { _, _ ->
                        calls++
                        gate.await()
                        MemberWizardDraft()
                    },
                    StandardTestDispatcher(testScheduler),
                )
            val first = launch { assertEquals(TripWizardPrepareOutcome.Ready, launcher.prepare("t", TripAction.Edit)) }
            runCurrent()
            assertEquals(TripWizardPrepareOutcome.Ignored, launcher.prepare("t", TripAction.Duplicate))
            assertEquals(1, calls)
            gate.complete(Unit)
            first.join()
        }

    @Test fun ignoredWhileCreateIsPending() =
        runTest(timeout = 60.seconds) {
            val launcher =
                WizardLauncher(
                    { null },
                    {},
                    prepareDependencies { _, _ -> error("pending create") },
                    StandardTestDispatcher(testScheduler),
                )
            launcher.open()
            assertEquals(TripWizardPrepareOutcome.Ignored, launcher.prepare("t", TripAction.Edit))
        }

    @Test fun noSessionIsIgnored() =
        runTest(timeout = 60.seconds) {
            assertEquals(TripWizardPrepareOutcome.Ignored, WizardLauncher({ null }, {}).prepare("t", TripAction.Edit))
        }
}
