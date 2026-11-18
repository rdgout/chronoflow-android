package net.chronoflow.feature.auth.twofactor

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.datamock.MockMethod
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.feature.auth.model.SignInModel
import net.chronoflow.feature.auth.model.TestGate
import net.chronoflow.feature.auth.model.TwoFactorModel
import net.chronoflow.feature.auth.model.challenged
import net.chronoflow.feature.auth.model.twoFactorSource
import net.chronoflow.feature.auth.testing.FakeSessionController
import net.chronoflow.feature.auth.testing.RecordingRoutes
import org.junit.After
import org.junit.Rule
import org.junit.rules.RuleChain
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
abstract class TwoFactorTestFixture {
    protected val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)
    protected val scheduler = TestCoroutineScheduler()
    protected val dispatcher = StandardTestDispatcher(scheduler)
    protected val scope = CoroutineScope(SupervisorJob() + dispatcher)
    protected val routes = RecordingRoutes()
    protected val session = FakeSessionController()
    protected val gate = TestGate()
    protected val source = twoFactorSource(dispatcher, sleep = { gate.wait() })
    protected lateinit var signIn: SignInModel
    protected lateinit var vm: TwoFactorViewModel
    protected val model: TwoFactorModel get() = signIn.twoFactor.value!!
    protected val tag: String get() = "two-factor.${model.field}"

    protected fun prepare(recovery: Boolean = false) {
        runTest(dispatcher, timeout = 60.seconds) { signIn = challenged(source, session, routes) }
        if (recovery) model.toggleMode()
        vm = TwoFactorViewModel(signIn, routes, scope)
    }

    @Composable protected fun Content() {
        ProfileHost(DeviceProfiles.pixel8) { ChronoFlowTheme(darkTheme = false) { TwoFactorScreen(vm) } }
    }

    protected fun host(recovery: Boolean = false) {
        prepare(recovery)
        compose.setContent { Content() }
        flush()
    }

    protected fun flush() {
        compose.runOnIdle { scheduler.advanceUntilIdle() }
        compose.waitForIdle()
    }

    protected fun failNext(failure: DataFailure) {
        runTest(dispatcher, timeout = 60.seconds) { source.failNext(MockMethod.CompleteTwoFactor, failure) }
    }

    protected fun fieldValue(): String =
        compose
            .onNodeWithTag(tag)
            .fetchSemanticsNode()
            .config[SemanticsProperties.EditableText]
            .text

    @After fun closeScope() {
        scope.cancel()
        scheduler.runCurrent()
    }
}
