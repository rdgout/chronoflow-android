package net.chronoflow.app.guest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import net.chronoflow.app.AppDispatchers
import net.chronoflow.app.RootContentTestHost
import net.chronoflow.app.RoutedWelcomeDestinations
import net.chronoflow.core.model.guest.MemoryGuestSecretStore
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.WizardMode
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.testing.wizard.FakeWizardGeneration
import net.chronoflow.core.testing.wizard.GenerationSource
import net.chronoflow.core.wizard.model.AirportSearchService
import net.chronoflow.core.wizard.model.GuestWizardStore
import net.chronoflow.core.wizard.model.MemberWizardDrafts
import net.chronoflow.core.wizard.model.TripWizardAction
import net.chronoflow.feature.wizard.TripWizardDependencies
import net.chronoflow.feature.wizard.TripWizardViewModel
import net.chronoflow.feature.wizard.debug.GuestWizardBoardSeed
import net.chronoflow.feature.wizard.debug.WizardBoardCaptureSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.nio.file.Files
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = android.app.Application::class)
class GuestWizardHostTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun planMyTripOpensGuestWhere() =
        withHost("WizardWhereGuestEmpty", welcome = true) { f, _ ->
            compose.runOnIdle { RoutedWelcomeDestinations(f.navigator).planMyTrip() }
            flush(f)
            compose.onNodeWithTag("wizard.where.continue").assertExists()
            assertEquals(Route.GuestWizard, f.navigator.state.value.route)
        }

    @Test fun rotationKeepsTheViewModel() =
        withHost("WizardSleep") { f, controller ->
            val first = checkNotNull(f.vm)
            val before = checkNotNull(first.wizard).draft
            controller.recreate()
            flush(f)
            assertSame(first, f.vm)
            assertEquals(before, checkNotNull(f.vm?.wizard).draft)
            compose.onNodeWithTag("wizard.sleep.continue").assertExists()
        }

    @Test fun leavingTheRouteClearsTheViewModel() =
        withHost("WizardWhereGuestEmpty") { f, _ ->
            val first = f.vm
            val process = ProcessLifecycleOwner.get().lifecycle as LifecycleRegistry
            var attachedObservers = 0
            compose.runOnIdle {
                attachedObservers = process.observerCount
                assertTrue(attachedObservers > 0)
                f.navigator.navigate(Route.Welcome)
            }
            flush(f)
            assertTrue(f.generations.single().cancelled)
            compose.runOnIdle {
                assertEquals(attachedObservers - 1, process.observerCount)
                RoutedWelcomeDestinations(f.navigator).planMyTrip()
            }
            flush(f)
            assertNotSame(first, f.vm)
            assertEquals(2, f.generations.size)
            compose.runOnIdle { assertEquals(attachedObservers, process.observerCount) }
        }

    @Test fun backOnWhereClosesWithoutFinishingTheActivity() =
        withHost("WizardWhereGuestEmpty") { f, controller ->
            compose.runOnIdle { controller.get().onBackPressedDispatcher.onBackPressed() }
            flush(f)
            compose.waitUntil(timeoutMillis = 1_000) {
                f.scheduler.runCurrent()
                f.navigator.state.value.route == Route.Welcome
            }
            assertEquals(Route.Welcome, f.navigator.state.value.route)
            assertEquals(0, f.rootBacks)
            org.junit.Assert.assertFalse(controller.get().isFinishing)
        }

    private fun flush(f: GuestHostFixture) {
        compose.runOnIdle { f.scheduler.runCurrent() }
        compose.waitForIdle()
    }

    private fun withHost(
        board: String,
        welcome: Boolean = false,
        block: (GuestHostFixture, ActivityController<GuestHostActivity>) -> Unit,
    ) {
        val f = GuestHostFixture(board, welcome)
        GuestHostActivity.fixture = f
        val controller = Robolectric.buildActivity(GuestHostActivity::class.java).setup()
        try {
            flush(f)
            block(f, controller)
        } finally {
            controller.pause().stop().destroy()
            runTest(f.dispatcher, timeout = 60.seconds) { f.job.cancelAndJoin() }
            f.root.deleteRecursively()
        }
    }
}

internal class GuestHostFixture(
    board: String,
    welcome: Boolean,
) {
    val scheduler = TestCoroutineScheduler()
    val dispatcher = StandardTestDispatcher(scheduler)
    val job = SupervisorJob()
    val scope = CoroutineScope(job + dispatcher)
    val root: File = Files.createTempDirectory("guest-entry").toFile()
    val navigator = RootNavigator().apply { navigate(if (welcome) Route.Welcome else Route.GuestWizard) }
    var vm: TripWizardViewModel? = null
    var rootBacks = 0
    val generations = mutableListOf<FakeWizardGeneration>()
    private val dispatchers = AppDispatchers(dispatcher, dispatcher, dispatcher)
    private val draft = GuestWizardBoardSeed.draft(MemberWizardDrafts.guest(), board, Locale.ENGLISH)
    private val actualStore =
        GuestWizardStore({ File(root, "guest-wizard.json") }, dispatcher, { WizardBoardCaptureSeed.now })
    private val deps =
        object : TripWizardDependencies {
            override val guest = true
            override val store = actualStore
            override val source = GenerationSource()
            override val now = { WizardBoardCaptureSeed.now }
            override val writerScope = scope
            override val airportSearch = AirportSearchService(source, now, sleep = {})

            override suspend fun start(): net.chronoflow.core.wizard.model.MemberWizardDraft {
                val original = actualStore.start()
                val seeded =
                    draft.copy(
                        state =
                            draft.state.copy(
                                instance = original.state.instance,
                                revision =
                                    original.state.revision + 1,
                            ),
                    )
                actualStore.persist(seeded)
                return seeded
            }

            override suspend fun startTrip(
                id: String,
                action: TripWizardAction,
            ) = draft

            override fun newGeneration() = FakeWizardGeneration().also { generations += it }
        }

    fun factory(revision: Long) =
        viewModelFactory {
            initializer {
                GuestWizardHolder(
                    deps,
                    GuestWizardExits(
                        navigator,
                        revision,
                        { SessionPresence.SignedOut },
                        actualStore,
                        MemoryGuestSecretStore(),
                        dispatchers,
                    ),
                    scope,
                )
            }
        }
}

internal class GuestHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RootContentTestHost { GuestEntryContent(fixture) } }
    }

    companion object {
        lateinit var fixture: GuestHostFixture
    }
}

@Composable
private fun GuestEntryContent(f: GuestHostFixture) {
    val state by f.navigator.state.collectAsState()
    val current = state
    val entries =
        remember(current) {
            listOf(
                NavEntry(checkNotNull(current.route), contentKey = "guest-test:${current.revision}") { route ->
                    if (route == Route.GuestWizard) {
                        val holder: GuestWizardHolder =
                            viewModel(
                                key = "guestWizardHolder:${current.revision}",
                                factory = f.factory(current.revision),
                            )
                        GuestWizardRoute(holder, current.revision)
                        val model: TripWizardViewModel =
                            viewModel(
                                key = "guestWizard:${current.revision}",
                                factory = TripWizardViewModel.factory(WizardMode.Create),
                            )
                        SideEffect { f.vm = model }
                    } else {
                        Box(Modifier.testTag("welcome.plan"))
                    }
                },
            )
        }
    val decorated =
        rememberDecoratedNavEntries(
            entries,
            listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
        )
    NavDisplay(
        entries = decorated,
        onBack = { f.rootBacks++ },
        transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        predictivePopTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
    )
}
