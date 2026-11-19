package net.chronoflow.feature.auth.signup

import androidx.compose.ui.test.junit4.v2.createComposeRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.feature.auth.model.SignInModel
import net.chronoflow.feature.auth.testing.AUTH_DEVICE_ID
import net.chronoflow.feature.auth.testing.FakeSessionController
import net.chronoflow.feature.auth.testing.authTestSource
import org.junit.After
import org.junit.Rule
import org.junit.rules.RuleChain

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
abstract class SignUpTestFixture(
    private val profile: DeviceProfile = DeviceProfiles.pixel8,
) {
    protected val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile)).around(compose)
    protected val scheduler = TestCoroutineScheduler()
    protected val dispatcher = StandardTestDispatcher(scheduler)
    protected val scope = CoroutineScope(SupervisorJob() + dispatcher)
    protected val session = FakeSessionController()
    protected val source = authTestSource(dispatcher)
    protected val navigator = RootNavigator().apply { navigate(Route.SignUp) }
    protected var identities = 0
    protected var identity: suspend () -> String = { AUTH_DEVICE_ID }
    protected val model =
        SignInModel(source, session, {
            identities++
            identity()
        }, { "test" }, accepted = { _, _ -> }, onTwoFactor = {})
            .makeSignUpModel { navigator.navigate(Route.SignIn) }
    protected val vm = SignUpViewModel(model, scope)
    protected val opened = mutableListOf<String>()
    protected val opener = PolicyOpener { opened += it }

    protected fun host() {
        compose.setContent {
            ProfileHost(profile) { ChronoFlowTheme(darkTheme = false) { SignUpRoute(vm, opener) } }
        }
        flush()
    }

    protected fun flush() {
        compose.runOnIdle { scheduler.runCurrent() }
        compose.waitForIdle()
    }

    protected fun mixed(field: String = "password_confirmation") {
        compose.runOnIdle {
            model.form.receive(
                DataFailure.Validation(
                    mapOf(
                        "email" to FieldMessage("This email address is already in use."),
                        field to FieldMessage("Please fill this in."),
                    ),
                    422,
                ),
            )
        }
        flush()
    }

    @After fun close() {
        scope.cancel()
        scheduler.runCurrent()
    }
}
