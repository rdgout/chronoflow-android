package net.chronoflow.app.guest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import net.chronoflow.app.AppDispatchers
import net.chronoflow.app.auth.AuthServices
import net.chronoflow.app.auth.LazyPublicDataSource
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.guest.MemoryGuestSecretStore
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.WizardMode
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.wizard.CheckingTestClock
import net.chronoflow.core.testing.wizard.FakeWizardGeneration
import net.chronoflow.core.testing.wizard.GenerationSource
import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.wizard.generation.TripGenerationReviewResult
import net.chronoflow.core.wizard.model.GuestWizardStore
import net.chronoflow.debug.guest.GuestSignupSeed
import net.chronoflow.feature.auth.model.SessionAcceptor
import net.chronoflow.feature.auth.signin.SignInContent
import net.chronoflow.feature.auth.signin.SignInUiState
import net.chronoflow.feature.wizard.TripWizardDependencies
import net.chronoflow.feature.wizard.TripWizardViewModel
import net.chronoflow.feature.wizard.debug.GuestWizardBoardSeed
import net.chronoflow.feature.wizard.debug.WizardBoardCaptureSeed
import java.io.File
import java.nio.file.Files
import java.util.Locale

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal class GuestSignupScreenFixture(
    val board: String,
    val profile: DeviceProfile,
) {
    val scheduler = TestCoroutineScheduler()
    val dispatcher = StandardTestDispatcher(scheduler)
    val job = SupervisorJob()
    val scope = CoroutineScope(job + dispatcher)
    val root: File = Files.createTempDirectory("guest-signup-screen").toFile()
    val file = File(root, "guest-wizard.json")
    val store = GuestWizardStore({ file }, dispatcher, { WizardBoardCaptureSeed.now })
    val secrets = MemoryGuestSecretStore()
    val navigator = RootNavigator().apply { navigate(Route.GuestWizard) }
    private val dispatchers = AppDispatchers(dispatcher, dispatcher, dispatcher)
    private val source = GenerationSource()
    val auth =
        AuthServices(
            navigator,
            scope,
            dispatchers,
            source,
            SessionAcceptor {},
            { GuestAuthFixture.DEVICE },
            { "phone" },
            routeAcceptedSession = { _, _ -> },
        )
    val coordinator =
        GuestSignupCoordinator(auth, navigator, navigator.state.value.revision, GuestSignupSeed.board(board))
    private val generation =
        FakeWizardGeneration().apply {
            reviewHandler = { TripGenerationReviewResult(inputs.last(), generationReview()) }
        }
    private val base =
        GuestWizardDependencies(
            store,
            LazyPublicDataSource { source },
            secrets,
            { GuestAuthFixture.DEVICE },
            { SessionPresence.SignedOut },
            dispatchers,
            scope,
            { WizardBoardCaptureSeed.now },
            coordinator,
            seed = { GuestWizardBoardSeed.draft(it, board, Locale.ENGLISH) },
            uiOverride = GuestWizardBoardSeed.uiOverride(board),
        )
    private val dependencies =
        object : TripWizardDependencies by base {
            override val checkingClock = CheckingTestClock(scheduler).clock

            override fun newGeneration() = generation
        }
    var vm: TripWizardViewModel? = null

    fun factory(revision: Long) =
        viewModelFactory {
            initializer {
                GuestWizardHolder(
                    dependencies,
                    GuestWizardExits(navigator, revision, { SessionPresence.SignedOut }, store, secrets, dispatchers),
                    scope,
                    GuestWizardBoardSeed.presentation(board),
                    coordinator,
                )
            }
        }
}

internal class GuestSignupTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fixture = checkNotNull(fixture)
        setContent {
            ProfileHost(fixture.profile) { ChronoFlowTheme(darkTheme = false) { GuestSignupTestContent(fixture) } }
        }
    }

    companion object {
        var fixture: GuestSignupScreenFixture? = null
    }
}

@Composable
private fun GuestSignupTestContent(fixture: GuestSignupScreenFixture) {
    val state by fixture.navigator.state.collectAsState()
    val current = state
    val entries =
        remember(current) {
            listOf(
                NavEntry(checkNotNull(current.route), contentKey = "guest-signup:${current.revision}") { route ->
                    if (route == Route.GuestWizard) {
                        val holder: GuestWizardHolder =
                            viewModel(
                                key = "guestWizardHolder:${current.revision}",
                                factory = fixture.factory(current.revision),
                            )
                        GuestWizardRoute(holder, current.revision)
                        val vm: TripWizardViewModel =
                            viewModel(
                                key = "guestWizard:${current.revision}",
                                factory = TripWizardViewModel.factory(WizardMode.Create),
                            )
                        SideEffect { fixture.vm = vm }
                    } else if (route == Route.SignIn) {
                        SignInContent(
                            SignInUiState(),
                            remember { TextFieldState(fixture.auth.signIn.email.value) },
                            remember { TextFieldState() },
                            onEvent = {},
                        )
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
        onBack = {},
        transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        predictivePopTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
    )
}
