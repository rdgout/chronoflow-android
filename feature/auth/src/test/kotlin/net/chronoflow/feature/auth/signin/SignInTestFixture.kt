package net.chronoflow.feature.auth.signin

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.feature.auth.testing.AuthTestFixture
import org.junit.After
import org.junit.Rule
import org.junit.rules.RuleChain

@OptIn(ExperimentalCoroutinesApi::class)
abstract class SignInTestFixture {
    protected val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)
    protected val scheduler = TestCoroutineScheduler()
    protected val dispatcher = StandardTestDispatcher(scheduler)
    protected val scope = CoroutineScope(SupervisorJob() + dispatcher)

    protected fun vm(fixture: AuthTestFixture): SignInViewModel = SignInViewModel(fixture.model, scope)

    protected fun host(fixture: AuthTestFixture) =
        mutableStateOf(vm(fixture)).also { displayed ->
            compose.setContent {
                ProfileHost(DeviceProfiles.pixel8) {
                    ChronoFlowTheme(darkTheme = false) { SignInScreen(displayed.value, fixture.routes) }
                }
            }
            flush()
        }

    protected fun flush() {
        compose.runOnIdle { scheduler.advanceUntilIdle() }
        compose.waitForIdle()
    }

    @After fun closeScope() {
        scope.cancel()
        scheduler.runCurrent()
    }
}
