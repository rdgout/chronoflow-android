package net.chronoflow.app.guest

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.MutableCreationExtras
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import net.chronoflow.app.auth.AuthServices
import net.chronoflow.app.auth.LazyPublicDataSource
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.AuthLanguage
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.WizardMode
import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardForm
import net.chronoflow.core.wizard.model.from
import net.chronoflow.core.wizard.model.inputFingerprint
import net.chronoflow.feature.auth.model.GuestSignupState
import net.chronoflow.feature.auth.model.SessionAcceptor
import net.chronoflow.feature.wizard.TripWizardDependenciesKey
import net.chronoflow.feature.wizard.TripWizardViewModel
import net.chronoflow.feature.wizard.guest.GuestSignUpHost
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardStepKey
import net.chronoflow.feature.wizard.shell.WizardUiOverride
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GuestSignupCoordinatorTest {
    @Test fun secondRequestWhilePresentedIsIgnored() =
        runTest(timeout = 60.seconds) {
            val fixture = GuestAuthFixture(this)
            val coordinator = coordinator(fixture)
            try {
                coordinator.request(Host())
                val first = coordinator.presentation.value
                coordinator.request(Host())
                assertSame(first, coordinator.presentation.value)
            } finally {
                coordinator.clear()
                fixture.close()
            }
        }

    @Test fun dismissIsIgnoredWhileBusy() =
        runTest(timeout = 60.seconds) {
            val fixture = GuestAuthFixture(this)
            val host = Host()
            val coordinator = coordinator(fixture)
            try {
                coordinator.request(host)
                val shown = checkNotNull(coordinator.presentation.value)
                shown.model.form.begin(net.chronoflow.feature.auth.model.AuthFormModel.Action.Register)
                coordinator.dismiss()
                assertSame(shown, coordinator.presentation.value)
                assertTrue(host.dismissals.isEmpty())
            } finally {
                coordinator.clear()
                fixture.close()
            }
        }

    @Test fun dismissInReviewMarksTheReviewExpired() =
        runTest(timeout = 60.seconds) {
            val fixture = GuestAuthFixture(this)
            val coordinator = coordinator(fixture, GuestSignupState.Review)
            val host = Host()
            try {
                coordinator.request(host)
                coordinator.dismiss()
                assertEquals(listOf(true to true), host.dismissals)
                assertNull(coordinator.presentation.value)
            } finally {
                coordinator.clear()
                fixture.close()
            }
        }

    @Test fun reviewDismissesAndExpires() =
        runTest(timeout = 60.seconds) {
            val fixture = GuestAuthFixture(this)
            val coordinator = coordinator(fixture, GuestSignupState.Review)
            val host = Host()
            try {
                coordinator.request(host)
                coordinator.review()
                assertEquals(1, host.reviews)
                assertEquals(listOf(true to true), host.dismissals)
                assertNull(coordinator.presentation.value)
            } finally {
                coordinator.clear()
                fixture.close()
            }
        }

    @Test fun signInInsteadCarriesTheEmail() =
        runTest(timeout = 60.seconds) {
            val fixture = GuestAuthFixture(this)
            val coordinator = coordinator(fixture)
            val order = mutableListOf<String>()
            val host = Host(order)
            try {
                fixture.auth.signIn.editEmail("old@example.com")
                fixture.auth.signIn.form
                    .receive(DataFailure.Offline)
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    fixture.auth.signIn.form.state
                        .drop(1)
                        .collect { if (it.failure == null) order.add("clearSecrets") }
                }
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    fixture.auth.signIn.email
                        .drop(1)
                        .collect { order.add("editEmail:$it") }
                }
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    fixture.navigator.state
                        .drop(1)
                        .collect { order.add("navigate:${it.route}") }
                }
                coordinator.request(host)
                checkNotNull(coordinator.presentation.value).model.editEmail("new@example.com")
                coordinator.signInInstead()
                runCurrent()
                assertEquals(
                    listOf(
                        "prepareSignIn",
                        "clearSecrets",
                        "editEmail:new@example.com",
                        "navigate:SignIn",
                        "signUpDismissed",
                    ),
                    order,
                )
                assertEquals("new@example.com", fixture.auth.signIn.email.value)
                assertEquals(listOf(false to false), host.dismissals)
                assertNull(coordinator.presentation.value)
            } finally {
                coordinator.clear()
                fixture.close()
            }
        }

    @Test fun submitInReviewSendsNothing() =
        runTest(timeout = 60.seconds) {
            val fixture = GuestAuthFixture(this)
            val coordinator = coordinator(fixture, GuestSignupState.Review)
            try {
                coordinator.request(Host())
                coordinator.submit(GuestTestSecret(), GuestTestSecret(), AuthLanguage.En) {}
                runCurrent()
                assertTrue(fixture.source.inbox().isEmpty())
                assertFalse(checkNotNull(coordinator.presentation.value).model.form.busy)
            } finally {
                coordinator.clear()
                fixture.close()
            }
        }

    @Test fun clearCancelsTheRequest() =
        runTest(timeout = 60.seconds) {
            val fixture = GuestAuthFixture(this)
            val blocked = CompletableDeferred<Unit>()
            var cancelled = false
            val auth =
                AuthServices(
                    fixture.navigator,
                    backgroundScope,
                    fixture.dispatchers,
                    fixture.source,
                    SessionAcceptor {},
                    deviceIdentity = {
                        try {
                            blocked.await()
                        } finally {
                            cancelled = true
                        }
                        GuestAuthFixture.DEVICE
                    },
                    deviceName = { "phone" },
                    routeAcceptedSession = { _, _ -> },
                )
            val coordinator = GuestSignupCoordinator(auth, fixture.navigator, fixture.navigator.state.value.revision)
            coordinator.bind(backgroundScope)
            val password = GuestTestSecret()
            val confirmation = GuestTestSecret()
            try {
                coordinator.request(Host())
                coordinator.submit(password, confirmation, AuthLanguage.En) {}
                runCurrent()
                assertTrue(checkNotNull(coordinator.presentation.value).model.form.busy)
                coordinator.clear()
                runCurrent()
                assertTrue(cancelled)
                assertEquals("", password.read())
                assertEquals("", confirmation.read())
                assertNull(coordinator.presentation.value)
            } finally {
                coordinator.clear()
                fixture.close()
            }
        }

    @Test fun dismissalAfterTheRouteRevisionChangesOnlyResetsPresented() =
        runTest(timeout = 60.seconds) {
            val fixture = GuestAuthFixture(this)
            val coordinator = coordinator(fixture, GuestSignupState.Review)
            val host = Host()
            try {
                coordinator.request(host)
                fixture.navigator.navigate(Route.GuestWizard)
                coordinator.dismiss()
                assertEquals(listOf(true to false), host.dismissals)
                assertEquals(0, host.reviews)
            } finally {
                coordinator.clear()
                fixture.close()
            }
        }

    @Test fun readyGuestRequestsOnePresentation() =
        runTest(timeout = 60.seconds) {
            val fixture = GuestAuthFixture(this)
            Dispatchers.setMain(fixture.dispatcher)
            val coordinator = coordinator(fixture)
            val owner = ViewModelStore()
            var vm: TripWizardViewModel? = null
            try {
                val dependencies =
                    GuestWizardDependencies(
                        fixture.guest,
                        LazyPublicDataSource { fixture.source },
                        fixture.secrets,
                        { GuestAuthFixture.DEVICE },
                        { SessionPresence.SignedOut },
                        fixture.dispatchers,
                        backgroundScope,
                        { fixture.clock.now },
                        coordinator,
                        seed = { draft ->
                            val form =
                                WizardForm.from(generationInput()).let { form ->
                                    form.copy(
                                        legs =
                                            listOf(
                                                form.legs.single().copy(
                                                    departLocal = "2030-10-12T14:40",
                                                    arriveLocal = "2030-10-13T09:15",
                                                ),
                                            ),
                                        identity = generationReview().identity,
                                    )
                                }
                            val ready =
                                draft.copy(
                                    form = form,
                                    state =
                                        draft.state.copy(
                                            step = MemberWizardStep.ready,
                                            review = generationReview(),
                                            reviewedAt = fixture.clock.now,
                                            sleepCompleted = true,
                                        ),
                                )
                            ready.copy(state = ready.state.copy(checkedInput = ready.inputFingerprint))
                        },
                        uiOverride = WizardUiOverride(),
                    )
                val provider =
                    ViewModelProvider.create(
                        owner,
                        TripWizardViewModel.factory(WizardMode.Create),
                        MutableCreationExtras().apply { set(TripWizardDependenciesKey, dependencies) },
                    )
                vm = provider[TripWizardViewModel::class.java]
                runCurrent()
                vm.onEvent(WizardEvent.Continue(WizardStepKey.Ready))
                runCurrent()
                val shown = coordinator.presentation.value
                assertNotNull(shown)
                vm.onEvent(WizardEvent.Continue(WizardStepKey.Ready))
                runCurrent()
                assertSame(shown, coordinator.presentation.value)
            } finally {
                withContext(NonCancellable) {
                    owner.clear()
                    vm
                        ?.viewModelScope
                        ?.coroutineContext
                        ?.get(kotlinx.coroutines.Job)
                        ?.cancelAndJoin()
                    coordinator.clear()
                    fixture.close()
                    Dispatchers.resetMain()
                }
            }
        }

    private fun kotlinx.coroutines.test.TestScope.coordinator(
        fixture: GuestAuthFixture,
        state: GuestSignupState = GuestSignupState.Ready,
    ): GuestSignupCoordinator =
        GuestSignupCoordinator(
            fixture.auth,
            fixture.navigator,
            fixture.navigator.state.value.revision,
            object : GuestSignupSeeding {
                override val state = state
            },
        ).also { it.bind(backgroundScope) }

    private class Host(
        private val order: MutableList<String> = mutableListOf(),
    ) : GuestSignUpHost {
        val dismissals = mutableListOf<Pair<Boolean, Boolean>>()
        var reviews = 0

        override fun signUpDismissed(
            needsReview: Boolean,
            onGuestRoute: Boolean,
        ) {
            order.add("signUpDismissed")
            dismissals.add(needsReview to onGuestRoute)
        }

        override suspend fun prepareSignIn() {
            order.add("prepareSignIn")
        }

        override fun reviewExpired() {
            reviews++
        }
    }
}
