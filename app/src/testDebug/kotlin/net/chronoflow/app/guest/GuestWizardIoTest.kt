@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.guest

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.MutableCreationExtras
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.app.AppDispatchers
import net.chronoflow.core.model.guest.GuestSecretStore
import net.chronoflow.core.model.guest.MemoryGuestSecretStore
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.navigation.WizardMode
import net.chronoflow.core.testing.wizard.CheckingTestClock
import net.chronoflow.core.testing.wizard.GenerationSource
import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.wizard.model.GuestWizardStore
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardDrafts
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardDraftStore
import net.chronoflow.debug.GuestWizardDebugSecrets
import net.chronoflow.feature.wizard.TripWizardDependencies
import net.chronoflow.feature.wizard.TripWizardDependenciesKey
import net.chronoflow.feature.wizard.TripWizardViewModel
import net.chronoflow.feature.wizard.debug.GuestWizardBoardSeed
import net.chronoflow.feature.wizard.guest.GuestSignUpLauncher
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardStepKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.util.Locale
import kotlin.coroutines.ContinuationInterceptor
import kotlin.time.Duration.Companion.seconds

class GuestWizardIoTest {
    @Test fun sleepReviewAgeAndRecheckKeepGuestStorageOffMain() =
        runTest(timeout = 60.seconds) {
            withGuest { f, vm ->
                runCurrent()
                assertEquals(
                    MemberWizardStep.sleep,
                    vm.uiState.value
                        ?.step
                        ?.step,
                )
                vm.onEvent(WizardEvent.SetSleep("habitual_bed", JsonPrimitive("23:45")))
                runCurrent()
                vm.onEvent(WizardEvent.Continue(WizardStepKey.Sleep))
                runCurrent()
                assertEquals(
                    MemberWizardStep.checking,
                    vm.uiState.value
                        ?.step
                        ?.step,
                )
                assertEquals(1, f.source.guestInputs.size)
                assertEquals(listOf(null to "first"), f.secrets.rotations)
                assertNull(f.source.guestInputs[0].identity)
                assertNull(f.source.guestInputs[0].guestSecret)

                advanceTimeBy(1_500)
                runCurrent()
                assertEquals(
                    MemberWizardStep.question,
                    vm.uiState.value
                        ?.step
                        ?.step,
                )
                vm.onEvent(WizardEvent.AnswerQuestion(JsonPrimitive("18_64")))
                runCurrent()
                advanceTimeBy(250)
                runCurrent()
                assertEquals(
                    MemberWizardStep.checking,
                    vm.uiState.value
                        ?.step
                        ?.step,
                )
                assertEquals(2, f.source.guestInputs.size)
                val recheck = f.source.guestInputs[1]
                assertEquals("first", recheck.identity?.token)
                assertEquals("minted", recheck.guestSecret)
                assertEquals(JsonPrimitive("18_64"), recheck.profile["age_band"])
                assertEquals(listOf(null to "first", "first" to "second"), f.secrets.rotations)
                assertEquals(1, f.secrets.reads)

                advanceTimeBy(1_500)
                runCurrent()
                assertEquals(
                    MemberWizardStep.fineTune,
                    vm.uiState.value
                        ?.step
                        ?.step,
                )
                checkNotNull(vm.wizard).flush()
                val expectedSteps =
                    listOf(
                        MemberWizardStep.sleep,
                        MemberWizardStep.checking,
                        MemberWizardStep.question,
                        MemberWizardStep.fineTune,
                    )
                assertTrue(f.persistedSteps.containsAll(expectedSteps))
                assertTrue(f.fileAccesses > 0)
                assertFalse(checkNotNull(vm.wizard).persistenceFailed)
                assertNull(
                    checkNotNull(vm.wizard)
                        .draft.form.identity
                        ?.guestSecret,
                )
                assertNull(
                    checkNotNull(vm.wizard)
                        .draft.state.review
                        ?.identity
                        ?.guestSecret,
                )
                assertTrue(GuestWizardDebugSecrets.hasReviewSecret(f.actualStore, f.secrets, f.io))
                withContext(f.io) { f.secrets.deleteAll() }
                assertFalse(GuestWizardDebugSecrets.hasReviewSecret(f.actualStore, f.secrets, f.io))
            }
        }

    @Test fun guestFileStoreHopsToIoFromMain() =
        runTest(timeout = 60.seconds) {
            withGuest { f, _ ->
                runCurrent()
                val accesses = f.fileAccesses
                withContext(Dispatchers.Main) {
                    assertFalse(currentCoroutineContext()[ContinuationInterceptor] === f.io)
                    f.actualStore.persist(checkNotNull(f.persistedDraft))
                }
                assertTrue(f.fileAccesses > accesses)
            }
        }

    @Test fun storageGuardsRejectCallsOnMain() =
        runTest(timeout = 60.seconds) {
            withGuest { f, _ ->
                runCurrent()
                val draft = checkNotNull(f.persistedDraft)
                withContext(Dispatchers.Main) {
                    assertRejected("guest draft storage requires IO") { f.guardedStore.persist(draft) }
                    assertRejected("guest secret storage requires a background dispatcher") { f.secrets.get("first") }
                }
            }
        }

    private suspend fun assertRejected(
        message: String,
        block: suspend () -> Unit,
    ) {
        var rejected: AssertionError? = null
        try {
            block()
        } catch (failure: AssertionError) {
            rejected = failure
        }
        assertNotNull(rejected)
        assertEquals(message, rejected?.message)
    }

    private suspend fun TestScope.withGuest(block: suspend TestScope.(Fixture, TripWizardViewModel) -> Unit) {
        val f = Fixture(this)
        Dispatchers.setMain(f.main)
        var vm: TripWizardViewModel? = null
        try {
            vm = f.createViewModel()
            block(f, vm)
        } finally {
            try {
                withContext(NonCancellable) {
                    try {
                        f.owners.clear()
                        vm
                            ?.viewModelScope
                            ?.coroutineContext
                            ?.get(Job)
                            ?.cancelAndJoin()
                    } finally {
                        f.writerJob.cancelAndJoin()
                        withContext(f.io) { f.root.deleteRecursively() }
                    }
                }
            } finally {
                Dispatchers.resetMain()
            }
        }
    }

    private class Fixture(
        scope: TestScope,
    ) {
        val main = StandardTestDispatcher(scope.testScheduler, "main")
        val io = StandardTestDispatcher(scope.testScheduler, "io")
        private val worker = StandardTestDispatcher(scope.testScheduler, "worker")
        val writerJob = SupervisorJob(scope.backgroundScope.coroutineContext[Job])
        private val writer = CoroutineScope(writerJob + io)
        val owners = ViewModelStore()
        val root: File = Files.createTempDirectory("guest-io").toFile()
        private val now = { Instant.parse("2026-11-22T12:00:00Z").plusMillis(scope.testScheduler.currentTime) }
        var fileAccesses = 0
        var persistedDraft: MemberWizardDraft? = null
        val persistedSteps = mutableListOf<MemberWizardStep>()
        val actualStore =
            GuestWizardStore({
                assertEquals("guest file access requires IO", io, currentCoroutineContext()[ContinuationInterceptor])
                fileAccesses++
                File(root, "guest-wizard.json")
            }, io, now)
        val guardedStore =
            object : WizardDraftStore by actualStore {
                override suspend fun persist(draft: MemberWizardDraft) {
                    val dispatcher = currentCoroutineContext()[ContinuationInterceptor]
                    assertTrue("guest draft storage requires IO", dispatcher === io)
                    actualStore.persist(draft)
                    persistedDraft = draft
                    persistedSteps += draft.state.step
                }
            }
        val secrets =
            GuardedSecrets {
                val dispatcher = currentCoroutineContext()[ContinuationInterceptor]
                val background = dispatcher === io || dispatcher === worker
                assertTrue("guest secret storage requires a background dispatcher", background)
            }
        val source =
            GenerationSource().apply {
                guestHandler = {
                    val index = guestInputs.size
                    val base = generationReview()
                    val seed = GuestWizardBoardSeed.draft(MemberWizardDrafts.guest(), "WizardQAge", Locale.ENGLISH)
                    val age = checkNotNull(seed.state.questions).first()
                    val identity =
                        base.identity.copy(
                            token = if (index == 1) "first" else "second",
                            guestSecret = if (index == 1) "minted" else null,
                        )
                    base.copy(
                        identity = identity,
                        prompts = if (index == 1) listOf(age) else emptyList(),
                    )
                }
            }
        private val actualDependencies =
            GuestWizardDependencies(
                actualStore,
                source,
                secrets,
                { "0199a3c0-0000-7000-8000-000000000001" },
                { SessionPresence.SignedOut },
                AppDispatchers(main, io, worker),
                writer,
                now,
                GuestSignUpLauncher { it.signUpDismissed(needsReview = false, onGuestRoute = true) },
                seed = { original ->
                    val draft = GuestWizardBoardSeed.draft(original, "WizardGuestJourney", Locale.ENGLISH, now())
                    persistedDraft = draft
                    draft
                },
            )
        private val dependencies =
            object : TripWizardDependencies by actualDependencies {
                override val store = guardedStore
                override val checkingClock = CheckingTestClock(scope.testScheduler).clock
            }

        fun createViewModel(): TripWizardViewModel =
            ViewModelProvider.create(
                owners,
                TripWizardViewModel.factory(WizardMode.Create),
                MutableCreationExtras().apply { set(TripWizardDependenciesKey, dependencies) },
            )[TripWizardViewModel::class.java]
    }

    private class GuardedSecrets(
        private val checkDispatcher: suspend () -> Unit,
    ) : GuestSecretStore {
        private val memory = MemoryGuestSecretStore()
        val rotations = mutableListOf<Pair<String?, String>>()
        var reads = 0

        override suspend fun get(token: String): String? {
            checkDispatcher()
            reads++
            return memory.get(token)
        }

        override suspend fun set(
            secret: String,
            token: String,
        ) {
            checkDispatcher()
            memory.set(secret, token)
        }

        override suspend fun rotate(
            oldToken: String?,
            newToken: String,
            secret: String,
        ) {
            checkDispatcher()
            rotations += oldToken to newToken
            memory.rotate(oldToken, newToken, secret)
        }

        override suspend fun delete(token: String) {
            checkDispatcher()
            memory.delete(token)
        }

        override suspend fun deleteAll() {
            checkDispatcher()
            memory.deleteAll()
        }
    }
}
