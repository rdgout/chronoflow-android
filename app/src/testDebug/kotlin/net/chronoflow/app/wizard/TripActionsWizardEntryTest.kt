@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.wizard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import net.chronoflow.app.shell.AppShellScaffold
import net.chronoflow.app.shell.ShellViewModel
import net.chronoflow.app.shell.WizardOverlay
import net.chronoflow.app.shell.rememberShellViewModel
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockMethod
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.WizardMode
import net.chronoflow.core.navigation.shell.ShellNav
import net.chronoflow.core.navigation.shell.ShellTab
import net.chronoflow.core.plan.actions.TripAction
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.junit.measureClassWarmup
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.wizard.model.editForm
import net.chronoflow.feature.trip.actions.TripActionFailure
import net.chronoflow.feature.trip.actions.TripActionsDebugCounters
import net.chronoflow.feature.trip.page.TripPage
import net.chronoflow.feature.trip.page.TripPageViewModel
import net.chronoflow.feature.wizard.TripWizardUiState
import net.chronoflow.feature.wizard.TripWizardViewModel
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardExit
import net.chronoflow.feature.wizard.shell.WizardStepKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
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
import java.time.ZoneOffset
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = android.app.Application::class)
class TripActionsWizardEntryTest {
    private val compose = createComposeRule()
    private val main =
        object : TestWatcher() {
            override fun starting(description: Description) = Dispatchers.setMain(StandardTestDispatcher())

            override fun finished(description: Description) = Dispatchers.resetMain()
        }

    @get:Rule val rules: RuleChain =
        RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(main).around(compose)
    private lateinit var holder: ShellViewModel

    private suspend fun TestScope.withEntry(
        id: String = TripActionsWizardFixture.ACTIVE,
        block: suspend TestScope.(TripActionsWizardFixture, TripPageViewModel) -> Unit,
    ) {
        val root = File(RuntimeEnvironment.getApplication().filesDir, "actions-entry-${UUID.randomUUID()}")
        val fixture = TripActionsWizardFixture.open(this, root, id)
        val launcher = WizardLauncher({ holder.state.overlay }, { holder.push(it) }, fixture.deps, fixture.io)
        val page = fixture.page(id, launcher)
        try {
            runCurrent()
            compose.setContent {
                ProfileHost(DeviceProfiles.pixel8) {
                    ChronoFlowTheme(reduceMotion = true) {
                        val shell = rememberShellViewModel(Route.Trips)
                        SideEffect { holder = shell }
                        AppShellScaffold(
                            shell,
                            {},
                            overlayOwnsBack = shell.state.overlay is Route.Wizard,
                            today = {},
                            trips = {},
                            settings = {},
                            page = { TripPage(page, {}, null, onWizard = { shell.push(Route.Wizard(it)) }) },
                            overlay = { route ->
                                if (route is Route.Wizard) {
                                    WizardOverlay(route.mode, fixture.deps, null, shell::closeWizard)
                                    val vm =
                                        ViewModelProvider.create(
                                            shell.overlayOwner,
                                            TripWizardViewModel.factory(route.mode),
                                        )[TripWizardViewModel::class.java]
                                    SideEffect { fixture.track(vm) }
                                }
                            },
                        )
                    }
                }
            }
            compose.runOnIdle { holder.openTrip(id) }
            runCurrent()
            compose.runOnIdle { page.openTripActions() }
            settleTag("trip.actions.edit")
            block(fixture, page)
        } finally {
            withContext(NonCancellable) {
                if (::holder.isInitialized) holder.overlayOwner.viewModelStore.clear()
                runCurrent()
                fixture.close()
                withContext(fixture.io) { root.deleteRecursively() }
            }
        }
    }

    private fun TestScope.settleTag(tag: String) {
        compose.waitUntil(4_000) {
            testScheduler.runCurrent()
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size == 1
        }
    }

    @Test fun testEditOpensMemberWizardFromActions() =
        runTest(timeout = 60.seconds) {
            withEntry { _, _ ->
                compose.onNodeWithTag("trip.actions.edit").performClick()
                settleTag("wizard.title")
                compose.onNodeWithTag("wizard.close").assertExists()
                compose.onNodeWithTag("trip.actions").assertDoesNotExist()
                assertEquals(Route.Wizard(WizardMode.Edit(TripActionsWizardFixture.ACTIVE)), holder.state.overlay)
            }
        }

    @Test fun testDuplicateOpensOutboundFromActions() =
        runTest(timeout = 60.seconds) {
            withEntry { _, _ ->
                compose.onNodeWithTag("trip.actions.duplicate").performClick()
                settleTag("wizard.outbound.continue")
                compose.onNodeWithTag("wizard.outbound.continue").assertIsNotEnabled()
                compose.onNodeWithTag("wizard.back").assertDoesNotExist()
                compose.onNodeWithTag("trip.actions").assertDoesNotExist()
            }
        }

    @Test fun editLoadFailureKeepsTheSheetWithTheServerMessage() =
        runTest(timeout = 60.seconds) {
            withEntry(TripActionsWizardFixture.UPCOMING) { fixture, page ->
                TripActionsDebugCounters.failurePresented.set(0)
                fixture.remote.failNext(MockMethod.TripInput, DataFailure.Server(500))
                compose.onNodeWithTag("trip.actions.edit").performClick()
                runCurrent()
                settleTag("trip.actions.failure")
                compose.waitUntil(4_000) { TripActionsDebugCounters.failurePresented.get() == 1 }
                assertEquals(TripActionFailure.Edit, page.actionFailure.value?.failure)
                compose.onNodeWithTag("trip.actions").assertExists()
                compose.onNodeWithTag("wizard.title").assertDoesNotExist()
            }
        }

    @Test fun duplicateOfflineShowsNeedsAConnection() =
        runTest(timeout = 60.seconds) {
            withEntry(TripActionsWizardFixture.UPCOMING) { fixture, page ->
                TripActionsDebugCounters.failurePresented.set(0)
                fixture.remote.failNext(MockMethod.TripInput, DataFailure.Offline)
                compose.onNodeWithTag("trip.actions.duplicate").performClick()
                runCurrent()
                settleTag("trip.actions.failure")
                compose.waitUntil(4_000) { TripActionsDebugCounters.failurePresented.get() == 1 }
                assertEquals(TripActionFailure.Offline, page.actionFailure.value?.failure)
                compose.onNodeWithTag("trip.actions").assertExists()
                compose.onNodeWithTag("wizard.title").assertDoesNotExist()
            }
        }

    @Test fun editSaveReplacesTheStoredDetail() =
        runTest(timeout = 60.seconds) {
            val root = File(RuntimeEnvironment.getApplication().filesDir, "edit-revision-${UUID.randomUUID()}")
            val id = TripActionsWizardFixture.UPCOMING
            val fixture = TripActionsWizardFixture.open(this, root, id)
            try {
                val beforeInput = fixture.remote.tripInput(id)
                val beforeTrip = fixture.trips.trips().single { it.id == id }
                val beforePlan = checkNotNull(fixture.trips.plan(id, checkNotNull(beforeTrip.currentInputHash)))
                val launcher = WizardLauncher({ null }, {}, fixture.deps, fixture.io)
                launcher.prepare(id, TripAction.Edit)
                val vm = fixture.wizard(WizardMode.Edit(id))
                runCurrent()
                advanceTimeBy(1_500)
                runCurrent()
                assertEquals(WizardStepKey.Ready, vm.uiState.value!!.step)
                val model = checkNotNull(vm.wizard)
                model.editForm { form ->
                    form.copy(preparation = form.preparation.copy(days = (form.preparation.days + 1) % 5))
                }
                vm.onEvent(WizardEvent.ReadyTick)
                runCurrent()
                assertTrue((vm.state.value as TripWizardUiState.Active).hasChanges)
                vm.onEvent(WizardEvent.Continue(WizardStepKey.Ready))
                runCurrent()
                assertEquals(WizardExit.Saved(id), vm.exit.value)
                assertNotEquals(beforeInput.sourceRevision, fixture.remote.tripInput(id).sourceRevision)
                val afterTrip = fixture.trips.trips().single { it.id == id }
                val afterPlan = checkNotNull(fixture.trips.plan(id, checkNotNull(afterTrip.currentInputHash)))
                assertNotEquals(beforePlan.inputHash, afterPlan.inputHash)
                assertFalse(beforePlan.detail.contentEquals(afterPlan.detail))
                val shell = ShellViewModel(ShellNav.openTrip(ShellNav.initial(ShellTab.Trips), id))
                shell.push(Route.Wizard(WizardMode.Edit(id)))
                shell.closeWizard(checkNotNull(vm.exit.value))
                assertEquals(ShellTab.Trips, shell.state.selected)
                assertEquals(
                    listOf(Route.Trips, Route.TripPage(id, awaitingSync = true)),
                    shell.state.stacks[ShellTab.Trips],
                )
            } finally {
                withContext(NonCancellable) {
                    fixture.close()
                    withContext(fixture.io) { root.deleteRecursively() }
                }
            }
        }

    companion object {
        @JvmStatic @BeforeClass
        fun warmFixtureOncePerJvm() {
            measureClassWarmup(TripActionsWizardEntryTest::class.java, 36, "mock-seed") {
                MockAppSeed.make(TripActionsWizardFixture.instant, zone = ZoneOffset.UTC)
            }
        }
    }
}
