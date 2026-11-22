@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.wizard

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.testing.wizard.FakeWizardGeneration
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.core.wizard.model.inputFingerprint
import net.chronoflow.feature.wizard.guest.GuestSignUpHost
import net.chronoflow.feature.wizard.guest.GuestSignUpLauncher
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardStepKey
import net.chronoflow.feature.wizard.shell.WizardUiOverride
import net.chronoflow.feature.wizard.ui.WizardInitialPresentation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

class TripWizardViewModelGuestSignUpTest {
    @get:Rule val main: org.junit.rules.TestWatcher = WizardMainDispatcherRule()

    @Test fun readyWaitsForTheSignupSheetAndDismissalAllowsAnotherRequest() =
        runTest(timeout = 60.seconds) {
            withGuest {
                tap()
                runCurrent()
                assertEquals(1, callbacks.size)
                assertTrue(checkNotNull(vm.wizard).guestAuthenticationPresented)
                tap()
                runCurrent()
                assertEquals(1, callbacks.size)
                callbacks.single().signUpDismissed(needsReview = false, onGuestRoute = true)
                runCurrent()
                assertFalse(checkNotNull(vm.wizard).guestAuthenticationPresented)
                tap()
                runCurrent()
                assertEquals(2, callbacks.size)
            }
        }

    @Test fun liveReadyRequestsSignUpWithoutSaving() =
        runTest(timeout = 60.seconds) {
            withGuest {
                tap()
                runCurrent()
                assertEquals(1, callbacks.size)
                assertEquals(0, generation.saves)
            }
        }

    @Test fun staleReadyRechecksInsteadOfRequestingSignUp() =
        runTest(timeout = 60.seconds) {
            withGuest(stale = true) {
                tap()
                runCurrent()
                assertTrue(callbacks.isEmpty())
                assertEquals(MemberWizardStep.checking, checkNotNull(vm.wizard).draft.state.step)
                assertEquals(0, generation.saves)
            }
        }

    @Test fun persistenceFailureNeverPresentsAuthentication() =
        runTest(timeout = 60.seconds) {
            withGuest {
                fake.recovery.failure = IOException()
                val model = checkNotNull(vm.wizard)
                model.setGuestSigningIn(false)
                tap()
                runCurrent()
                assertTrue(callbacks.isEmpty())
                assertFalse(model.guestAuthenticationPresented)
                assertTrue(model.persistenceFailed)
            }
        }

    @Test fun dismissalWithReviewMarksTheReviewExpired() =
        runTest(timeout = 60.seconds) {
            withGuest {
                tap()
                runCurrent()
                val model = checkNotNull(vm.wizard)
                callbacks.single().signUpDismissed(needsReview = true, onGuestRoute = true)
                assertFalse(model.guestAuthenticationPresented)
                assertEquals(MemberWizardStep.checking, model.draft.state.step)
                assertTrue(model.draft.state.returnToReady == true)
                org.junit.Assert.assertNull(model.draft.state.reviewedAt)
                org.junit.Assert.assertNull(model.saveOutcome)
            }
        }

    @Test fun dismissalAfterLeavingTheRouteOnlyResetsPresented() =
        runTest(timeout = 60.seconds) {
            withGuest {
                tap()
                runCurrent()
                val model = checkNotNull(vm.wizard)
                val before = model.draft
                callbacks.single().signUpDismissed(needsReview = true, onGuestRoute = false)
                assertFalse(model.guestAuthenticationPresented)
                assertEquals(before, model.draft)
            }
        }

    @Test fun prepareSignInPersistsSigningIn() =
        runTest(timeout = 60.seconds) {
            withGuest {
                vm.prepareSignIn()
                assertTrue(
                    fake.recovery.last
                        ?.state
                        ?.signingIn == true,
                )
            }
        }

    @Test fun initialPresentationRequestsSignUpOnce() =
        runTest(timeout = 60.seconds) {
            withGuest(presentSignUp = true) {
                assertEquals(1, callbacks.size)
                runCurrent()
                assertEquals(1, callbacks.size)
                org.junit.Assert.assertNull(checkNotNull(vm.wizard).saveOutcome)
            }
        }

    private class Fixture(
        val fake: FakeWizardDependencies,
        val vm: TripWizardViewModel,
        val generation: FakeWizardGeneration,
        val callbacks: MutableList<GuestSignUpHost>,
    ) {
        fun tap() = vm.onEvent(WizardEvent.Continue(WizardStepKey.Ready))
    }

    private suspend fun TestScope.withGuest(
        stale: Boolean = false,
        presentSignUp: Boolean = false,
        block: suspend Fixture.() -> Unit,
    ) {
        val fake = FakeWizardDependencies(this)
        val generation = FakeWizardGeneration()
        val callbacks = mutableListOf<GuestSignUpHost>()
        var draft = fake.checkedDraft(MemberWizardStep.ready)
        draft =
            draft.copy(
                state =
                    draft.state.copy(
                        mode = WizardMode.guest,
                        sleepCompleted = true,
                        profile =
                            JsonObject(
                                mapOf(
                                    "habitual_bed" to JsonPrimitive("23:00"),
                                    "habitual_wake" to JsonPrimitive("07:00"),
                                ),
                            ),
                    ),
            )
        fake.draft =
            draft.copy(state = draft.state.copy(checkedInput = if (stale) "changed" else draft.inputFingerprint))
        val deps =
            object : TripWizardDependencies by fake {
                override val guest = true
                override val uiOverride = WizardUiOverride()
                override val signUpLauncher = GuestSignUpLauncher { callbacks += it }

                override fun newGeneration() = generation
            }
        val fixture =
            WizardViewModelFixture(
                deps,
                net.chronoflow.core.navigation.WizardMode.Create,
                WizardInitialPresentation(presentSignUp = presentSignUp),
            )
        try {
            runCurrent()
            Fixture(fake, fixture.model, generation, callbacks).block()
        } finally {
            withContext(NonCancellable) {
                fixture.clear()
                fake.close()
            }
        }
    }
}
