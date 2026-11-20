package net.chronoflow.app.auth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import net.chronoflow.app.AppDispatchers
import net.chronoflow.app.RootContent
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.auth.model.SignUpModel
import net.chronoflow.feature.auth.signup.PolicyOpener
import net.chronoflow.feature.auth.signup.SignUpRoute
import net.chronoflow.feature.auth.signup.SignUpViewModel
import net.chronoflow.feature.trip.timeline.LocalTimelineJankTagger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = android.app.Application::class)
class SignUpEntryScopeTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun removedEntryIsClearedAndARecreatedActivityKeepsItsCurrentEntry() {
        val scheduler = TestCoroutineScheduler()
        val dispatcher = StandardTestDispatcher(scheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val navigator =
            net.chronoflow.core.navigation
                .RootNavigator()
        val services =
            AuthServices(
                navigator,
                scope,
                AppDispatchers(dispatcher, dispatcher, dispatcher),
                net.chronoflow.core.testing.sync
                    .ScriptedSource(),
                net.chronoflow.feature.auth.model
                    .SessionAcceptor {},
                { "id" },
                { "test" },
                {},
            )
        val fixture =
            EntryScopeFixture(
                navigator,
                viewModelFactory {
                    initializer {
                        val model = services.signIn.makeSignUpModel { navigator.navigate(Route.SignIn) }
                        EntryScopeActivity.fixture.lastModel = model
                        SignUpViewModel(model, CoroutineScope(SupervisorJob() + dispatcher))
                    }
                },
            )
        EntryScopeActivity.fixture = fixture
        navigator.navigate(Route.SignUp)
        val controller = Robolectric.buildActivity(EntryScopeActivity::class.java).setup()

        fun flush() {
            compose.runOnIdle { scheduler.runCurrent() }
            compose.waitForIdle()
        }
        try {
            flush()
            compose.onNodeWithTag("sign-up.name").performTextInput("first")
            flush()
            val first = fixture.vm
            val firstModel = checkNotNull(fixture.lastModel)
            compose.runOnIdle { navigator.navigate(Route.SignIn) }
            flush()
            firstModel.editName("ignored")
            assertEquals("first", firstModel.name.value)
            compose.runOnIdle { navigator.navigate(Route.SignUp) }
            flush()
            assertNotSame(first, fixture.vm)
            assertEquals("", fixture.lastModel!!.name.value)
            compose.onNodeWithTag("sign-up.name").performTextInput("kept")
            flush()
            val kept = fixture.vm
            controller.recreate()
            flush()
            assertSame(kept, fixture.vm)
            assertEquals("kept", fixture.lastModel!!.name.value)
            compose.onNodeWithTag("sign-up.name").assertExists()
        } finally {
            controller.pause().stop().destroy()
            scope.cancel()
            scheduler.runCurrent()
        }
    }
}

class EntryScopeFixture(
    val navigator: net.chronoflow.core.navigation.RootNavigator,
    val factory: ViewModelProvider.Factory,
) {
    var lastModel: SignUpModel? = null
    var vm: SignUpViewModel? = null
}

class EntryScopeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by fixture.navigator.state
                .collectAsState()
            ChronoFlowTheme {
                CompositionLocalProvider(
                    LocalSafeInsets provides WindowInsets(0),
                    LocalLayoutDirection provides LayoutDirection.Ltr,
                    LocalTimelineJankTagger provides null,
                ) {
                    RootContent(state, {}) { route ->
                        if (route == Route.SignUp) {
                            val vm: SignUpViewModel = viewModel(factory = fixture.factory)
                            SideEffect { fixture.vm = vm }
                            SignUpRoute(vm, PolicyOpener {})
                        }
                    }
                }
            }
        }
    }

    companion object {
        lateinit var fixture: EntryScopeFixture
    }
}
