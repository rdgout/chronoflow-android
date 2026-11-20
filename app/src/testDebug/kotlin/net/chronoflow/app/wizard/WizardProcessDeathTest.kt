@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.wizard

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.api.ApiTripSaveInput
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.testing.wizard.WizardTestClock
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardExit
import net.chronoflow.feature.wizard.shell.WizardRoot
import net.chronoflow.feature.wizard.shell.WizardStepKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = android.app.Application::class)
class WizardProcessDeathTest {
    private val compose = createComposeRule()
    private val main =
        object : TestWatcher() {
            override fun starting(description: Description) = Dispatchers.setMain(StandardTestDispatcher())

            override fun finished(description: Description) = Dispatchers.resetMain()
        }

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(DeviceProfiles.pixel8),
            ).around(main)
            .around(compose)
    private val exits = mutableListOf<WizardExit>()
    private val graphs = mutableListOf<WizardSaveGraphFixture>()

    private suspend fun TestScope.withGraphs(block: suspend TestScope.(File, WizardTestClock) -> Unit) {
        val root = File(RuntimeEnvironment.getApplication().filesDir, "wizard-death-${UUID.randomUUID()}")
        val clock = WizardTestClock().apply { now = Instant.parse("2026-11-04T12:00:00Z") }
        try {
            block(root, clock)
        } finally {
            withContext(NonCancellable) {
                graphs.asReversed().forEach { it.kill() }
                withContext(StandardTestDispatcher(testScheduler)) { root.deleteRecursively() }
            }
        }
    }

    private suspend fun TestScope.open(
        root: File,
        clock: WizardTestClock,
        hold: SaveHold = SaveHold.None,
        fresh: Boolean = false,
        existing: Boolean = true,
    ) = WizardSaveGraphFixture.open(this, root, clock, hold, fresh, existing).also {
        graphs.add(it)
        runCurrent()
    }

    private suspend fun kill(graph: WizardSaveGraphFixture) {
        graph.kill()
        graphs.remove(graph)
    }

    private fun show(graph: WizardSaveGraphFixture) {
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(darkTheme = false, reduceMotion = true) {
                    WizardRoot(graph.model, exits::add)
                }
            }
        }
    }

    private fun TestScope.save(graph: WizardSaveGraphFixture) {
        assertEquals(
            WizardStepKey.Ready,
            graph.model.uiState.value!!
                .step,
        )
        graph.model.onEvent(WizardEvent.Continue(WizardStepKey.Ready))
        runCurrent()
    }

    @Test fun calculatingRowResumesTheSaveWithTheSameIdentity() =
        runTest(timeout = 60.seconds) {
            withGraphs { root, clock ->
                val a = open(root, clock, SaveHold.BeforeRequest, fresh = true)
                val before = a.remote.fetchTrips().size
                save(a)
                val identity = a.source.requests.single()
                assertEquals(
                    MemberWizardStep.calculating,
                    a.store
                        .recover()!!
                        .state.step,
                )
                kill(a)
                val b = open(root, clock, SaveHold.BeforeRequest)
                show(b)
                compose.onNodeWithTag("wizard.calculating").assertExists()
                assertEquals(listOf(identity), b.source.requests)
                b.source.gate.complete(Unit)
                runCurrent()
                compose.runOnIdle { assertEquals(listOf(WizardExit.Saved(b.source.created!!)), exits) }
                assertEquals(before + 1, b.remote.fetchTrips().size)
                assertNull(b.store.recover())
            }
        }

    @Test fun lostResponseResumeReturnsTheSameTrip() =
        runTest(timeout = 60.seconds) {
            withGraphs { root, clock ->
                val a = open(root, clock, SaveHold.AfterReceipt, fresh = true)
                val before = a.remote.fetchTrips().size
                save(a)
                val created = checkNotNull(a.source.created)
                val identity = a.source.requests.single()
                assertNull(
                    a.store
                        .recover()!!
                        .state.savedTripId,
                )
                kill(a)
                val b = open(root, clock, SaveHold.BeforeRequest)
                show(b)
                compose.onNodeWithTag("wizard.calculating").assertExists()
                b.source.gate.complete(Unit)
                runCurrent()
                compose.runOnIdle { assertEquals(listOf(WizardExit.Saved(created)), exits) }
                assertEquals(listOf(identity), b.source.requests)
                assertEquals(before + 1, b.remote.fetchTrips().size)
                assertNull(b.store.recover())
            }
        }

    @Test fun savedTripIdRowSkipsTheCreate() =
        runTest(timeout = 60.seconds) {
            withGraphs { root, clock ->
                val a = open(root, clock, SaveHold.DuringPull, fresh = true)
                save(a)
                val id =
                    checkNotNull(
                        a.store
                            .recover()!!
                            .state.savedTripId,
                    )
                kill(a)
                val b = open(root, clock)
                show(b)
                compose.runOnIdle { assertEquals(listOf(WizardExit.Saved(id)), exits) }
                assertTrue(b.source.requests.isEmpty())
                assertNull(b.store.recover())
            }
        }

    @Test fun readyRowRechecksOnceAfterRecovery() =
        runTest(timeout = 60.seconds) {
            withGraphs { root, clock ->
                val a = open(root, clock, fresh = true)
                a.model.wizard!!.flush()
                kill(a)
                val b = open(root, clock)
                show(b)
                compose.onNodeWithTag("wizard.ready").assertExists()
                assertEquals(1, b.source.reviews)
                assertTrue(b.source.requests.isEmpty())
                assertTrue(exits.isEmpty())
            }
        }

    private fun reminders(firstTrip: Boolean) =
        runTest(timeout = 60.seconds) {
            withGraphs { root, clock ->
                val a = open(root, clock, fresh = true, existing = !firstTrip)
                assertEquals(
                    firstTrip,
                    a.model.wizard!!
                        .draft.state.firstTrip,
                )
                save(a)
                show(a)
                compose.runOnIdle { assertEquals(listOf(WizardExit.Saved(a.source.created!!)), exits) }
                val setting = SettingsRepository(a.database).settings().firstOrNull { it.section == "wizardReminders" }
                assertEquals(if (firstTrip) "\"pending\"" else null, setting?.value)
            }
        }

    @Test fun firstTripSaveLeavesTheRemindersOfferPending() = reminders(true)

    @Test fun existingTripSaveDoesNotQueueTheOffer() = reminders(false)

    @Test fun expiredRowDoesNotResume() =
        runTest(timeout = 60.seconds) {
            withGraphs { root, clock ->
                val a = open(root, clock, SaveHold.BeforeRequest, fresh = true)
                save(a)
                val instance =
                    a.store
                        .recover()!!
                        .state.instance
                kill(a)
                clock.advance(1_800.0)
                val b = open(root, clock)
                show(b)
                compose.onNodeWithTag("wizard.where.footer").assertExists()
                assertTrue(b.source.requests.isEmpty())
                assertEquals(0, b.source.reviews)
                assertNotEquals(
                    instance,
                    b.store
                        .recover()!!
                        .state.instance,
                )
            }
        }
}
