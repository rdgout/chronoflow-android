package net.chronoflow.feature.auth.verify

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.EmailVerificationStatus
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.feature.auth.model.VerificationContext
import net.chronoflow.feature.auth.model.VerifyEmailModel
import net.chronoflow.feature.auth.testing.AUTH_DEVICE_ID
import net.chronoflow.feature.auth.testing.authTestSource
import org.junit.After
import org.junit.Rule
import org.junit.rules.RuleChain
import kotlin.time.Duration.Companion.seconds

class VerifyLifecycleOwner : LifecycleOwner {
    val registry = LifecycleRegistry.createUnsafe(this)
    override val lifecycle: Lifecycle get() = registry
}

@OptIn(ExperimentalCoroutinesApi::class)
abstract class VerifyTestFixture {
    protected val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)
    protected val scheduler = TestCoroutineScheduler()
    protected val dispatcher = StandardTestDispatcher(scheduler)
    protected val scope = CoroutineScope(SupervisorJob() + dispatcher)
    protected val base = authTestSource(dispatcher, verified = false)
    protected var checks = 0
    protected var nowReads = 0
    protected var now = 1_800_000_000.0
    protected val source =
        object : DataSource by base {
            override suspend fun verificationStatus(): EmailVerificationStatus {
                checks++
                return base.verificationStatus()
            }
        }
    private lateinit var account: net.chronoflow.core.model.api.SessionAccount

    init {
        runTest(dispatcher, timeout = 60.seconds) {
            account =
                (
                    source.signIn(
                        PasswordSignInInput("demo@example.com", "Password123!", AUTH_DEVICE_ID, "test"),
                    ) as SignInResult.Session
                ).result.account
        }
    }

    protected val model =
        VerifyEmailModel(
            VerificationContext(account.id, account.email, 7),
            source,
            {
                nowReads++
                now
            },
            { VerificationContext(account.id, account.email, 7) },
            { true },
            {},
            { throw DataFailure.Offline },
            scope = scope,
            main = dispatcher,
            io = dispatcher,
        )
    protected val lifecycle = VerifyLifecycleOwner()
    protected val visible = mutableStateOf(true)
    protected val probe = VerifyCompositionProbe()

    protected fun host(initial: Lifecycle.State = Lifecycle.State.STARTED) {
        lifecycle.registry.currentState = initial
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(darkTheme = false) {
                    CompositionLocalProvider(
                        LocalLifecycleOwner provides lifecycle,
                        LocalVerifyCompositionProbe provides probe,
                    ) {
                        if (visible.value) VerifyEmailRoute(model) { false }
                    }
                }
            }
        }
        flush()
    }

    protected fun flush() {
        compose.runOnIdle { scheduler.runCurrent() }
        compose.waitForIdle()
    }

    protected fun act(block: suspend () -> Unit) {
        val task = scope.async { block() }
        compose.runOnIdle { scheduler.runCurrent() }
        check(task.isCompleted)
        task.getCompleted()
        flush()
    }

    @After fun close() {
        scope.cancel()
        scheduler.runCurrent()
    }
}
