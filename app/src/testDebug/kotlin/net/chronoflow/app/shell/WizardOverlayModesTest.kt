@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.shell

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.WizardMode
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.testing.wizard.GenerationSource
import net.chronoflow.core.testing.wizard.InMemoryWizardDraftStore
import net.chronoflow.core.wizard.generation.TripEditContext
import net.chronoflow.core.wizard.generation.TripGenerationEffects
import net.chronoflow.core.wizard.generation.TripGenerationService
import net.chronoflow.core.wizard.model.AirportSearchService
import net.chronoflow.core.wizard.model.TripWizardAction
import net.chronoflow.core.wizard.model.inputFingerprint
import net.chronoflow.feature.wizard.TripWizardDependencies
import net.chronoflow.feature.wizard.debug.WizardBoardCaptureSeed
import net.chronoflow.feature.wizard.shell.WizardUiOverride
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = android.app.Application::class)
class WizardOverlayModesTest {
    @get:Rule val compose = createComposeRule()

    @Test fun editReadyBackClosesOnlyTheOverlay() =
        runTest(timeout = 60.seconds) {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val original = WizardBoardCaptureSeed.draft("WizardReady", Locale.ENGLISH)
            val draft =
                original.copy(
                    state =
                        original.state.copy(
                            edit = TripEditContext("t1", "source"),
                            originalForm = original.form,
                            originalInput = original.inputFingerprint,
                        ),
                )
            val source = GenerationSource()
            val service =
                TripGenerationService(
                    source,
                    TripGenerationEffects({}, {}, {}),
                    parentContext = backgroundScope.coroutineContext,
                )
            val deps =
                object : TripWizardDependencies {
                    override val store = InMemoryWizardDraftStore()
                    override val source = source
                    override val writerScope = backgroundScope
                    override val now = { WizardBoardCaptureSeed.now }
                    override val airportSearch = AirportSearchService({ emptyList() }, now, {})
                    override val uiOverride = WizardUiOverride(reviewing = false, retrySeconds = 0)

                    override suspend fun recover() = draft

                    override suspend fun start() = error("prepared edit must recover")

                    override suspend fun startTrip(
                        id: String,
                        action: TripWizardAction,
                    ) = error("must not reload")

                    override fun newGeneration() = service
                }
            lateinit var holder: ShellViewModel
            lateinit var activity: ComponentActivity
            try {
                compose.setContent {
                    ProfileHost(DeviceProfiles.pixel8) {
                        ChronoFlowTheme(reduceMotion = true) {
                            val shell = rememberShellViewModel(Route.Trips)
                            val currentActivity = LocalActivity.current as ComponentActivity
                            SideEffect {
                                holder = shell
                                activity = currentActivity
                            }
                            AppShellScaffold(
                                shell,
                                {},
                                overlayOwnsBack = shell.state.overlay is Route.Wizard,
                                today = {},
                                trips = { Box(Modifier.fillMaxSize().testTag("trips.fixture")) },
                                settings = {},
                                page = {},
                                overlay = { route ->
                                    if (route is Route.Wizard) WizardOverlay(route.mode, deps, null, shell::closeWizard)
                                },
                            )
                        }
                    }
                }
                compose.runOnIdle { holder.push(Route.Wizard(WizardMode.Edit("t1"))) }
                compose.waitUntil(4_000) {
                    testScheduler.runCurrent()
                    compose.onAllNodesWithTag("wizard.ready").fetchSemanticsNodes().size == 1
                }
                compose.onNodeWithTag("wizard.ready").assertExists()
                val stacks = holder.state.stacks
                compose.runOnIdle { activity.onBackPressedDispatcher.onBackPressed() }
                runCurrent()
                compose.onNodeWithTag("trips.fixture").assertExists()
                assertNull(holder.state.overlay)
                assertEquals(stacks, holder.state.stacks)
            } finally {
                withContext(NonCancellable) {
                    holder.overlayOwner.viewModelStore.clear()
                    runCurrent()
                    service.cancel()
                    Dispatchers.resetMain()
                }
            }
        }
}
