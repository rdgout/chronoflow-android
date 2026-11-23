@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.perf

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.datamock.MockServerState
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.navigation.WizardMode
import net.chronoflow.core.testing.junit.measureClassWarmup
import net.chronoflow.core.testing.wizard.CheckingTestClock
import net.chronoflow.core.testing.wizard.InMemoryWizardDraftStore
import net.chronoflow.core.wizard.generation.TripGenerationEffects
import net.chronoflow.core.wizard.generation.TripGenerationService
import net.chronoflow.core.wizard.model.AirportSearchService
import net.chronoflow.core.wizard.model.MemberWizardDrafts
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.TripWizardAction
import net.chronoflow.feature.wizard.TripWizardDependencies
import net.chronoflow.feature.wizard.TripWizardViewModel
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardStepKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.time.Instant
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

class PerfWizardViewModelTest {
    @Test fun beforeAccountSnapshotTheSameSeedRequiresSleepAndDisablesContinue() =
        withJourney(snapshotLoaded = false) { viewModel ->
            val ui = viewModel.uiState.value!!
            assertEquals(MemberWizardStep.sleep, ui.step.step)
            assertNull(ui.fineTune)
            assertFalse(ui.chrome.continueEnabled)
            val sleep = checkNotNull(ui.sleep)
            assertNull(sleep.bed)
            assertNull(sleep.wake)
            assertNull(sleep.chronotype)
            assertTrue(
                viewModel.wizard!!
                    .draft.state.sleepNeeded,
            )
        }

    @Test fun afterAccountSnapshotOneWayEnablesFineTuneAndContinuesToReady() =
        withJourney(snapshotLoaded = true) { viewModel ->
            val fineTune = viewModel.uiState.value!!
            assertEquals(MemberWizardStep.fineTune, fineTune.step.step)
            assertTrue(fineTune.chrome.continueEnabled)
            viewModel.onEvent(WizardEvent.Continue(WizardStepKey.FineTune))
            runCurrent()
            assertEquals(
                MemberWizardStep.ready,
                viewModel.uiState.value!!
                    .step.step,
            )
            assertTrue(
                viewModel.uiState.value!!
                    .chrome.continueEnabled,
            )
        }

    private fun withJourney(
        snapshotLoaded: Boolean,
        verify: suspend TestScope.(TripWizardViewModel) -> Unit,
    ) = runTest(timeout = 60.seconds) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val checking = CheckingTestClock(testScheduler)
        val source = MockDataSource(seed = seed, now = { now }, sleep = {}, debugAppSeed = false, io = dispatcher)
        val generation =
            TripGenerationService(
                source,
                TripGenerationEffects({}, {}, {}),
                parentContext = backgroundScope.coroutineContext,
            )
        var model: TripWizardViewModel? = null
        try {
            assertTrue(
                source.signIn(
                    PasswordSignInInput(
                        "demo@example.com",
                        "password",
                        "019a562b-8000-7000-8000-000000000013",
                        "perf wizard test",
                    ),
                ) is SignInResult.Session,
            )
            val settings =
                seed.accounts.values
                    .single { it.snapshot.email == "demo@example.com" }
                    .snapshot.settings
            val initial =
                MemberWizardDrafts.memberInitial(
                    hasTrip = snapshotLoaded,
                    latestOrigin = null,
                    savedProfile = settings.bodyClock.takeIf { snapshotLoaded },
                    sleepAnswered = settings.sleepAnswered.takeIf { snapshotLoaded },
                )
            val draft = PerfWizardJourney.apply(initial, Locale.ENGLISH, now)
            val dependencies =
                object : TripWizardDependencies {
                    override val store = InMemoryWizardDraftStore()
                    override val airportSearch =
                        AirportSearchService({ emptyList() }, { PerfWizardViewModelTest.now }, {})
                    override val source = source
                    override val now = { PerfWizardViewModelTest.now }
                    override val writerScope = backgroundScope
                    override val checkingClock = checking.clock

                    override suspend fun start() = draft

                    override suspend fun startTrip(
                        id: String,
                        action: TripWizardAction,
                    ) = error("unexpected edit")

                    override fun newGeneration() = generation
                }
            val viewModel = TripWizardViewModel(WizardMode.Create, dependencies).also { model = it }
            runCurrent()
            assertEquals(
                MemberWizardStep.returnChoice,
                viewModel.uiState.value!!
                    .step.step,
            )
            viewModel.onEvent(WizardEvent.ChooseReturn("one_way"))
            checking.advance(5_000)
            verify(viewModel)
        } finally {
            withContext(NonCancellable) {
                model?.wizard?.close()
                model
                    ?.viewModelScope
                    ?.coroutineContext
                    ?.job
                    ?.cancelAndJoin()
                generation.cancel()
            }
            Dispatchers.resetMain()
        }
    }

    companion object {
        private val now = Instant.parse("2026-11-22T12:00:00Z")
        private lateinit var seed: MockServerState

        @JvmStatic @BeforeClass
        fun warmSeed() {
            measureClassWarmup(PerfWizardViewModelTest::class.java, 0, "mock-app-seed") {
                seed = MockAppSeed.make(now, walkthroughSeen = true)
            }
        }
    }
}
