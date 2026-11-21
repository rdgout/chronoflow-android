@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.passkeys

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.passkey.DeterministicRegistrationProvider
import net.chronoflow.core.model.passkey.PasskeyCapability
import net.chronoflow.feature.settings.support.FakeSettingsDependencies
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.After
import kotlin.time.Duration.Companion.seconds

abstract class PasskeysTestHost : SettingsScreenTest() {
    protected val scheduler = TestCoroutineScheduler()
    protected val dispatcher = StandardTestDispatcher(scheduler)
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    protected val deps = FakeSettingsDependencies(scope)
    private val store = ViewModelStore()
    protected var model: PasskeysViewModel? = null

    protected fun route(
        enabled: Boolean = true,
        onBack: () -> Unit = {},
    ): PasskeysViewModel {
        deps.passkeyCapability = PasskeyCapability(enabled)
        deps.passkeyRegistration = DeterministicRegistrationProvider()
        val model = PasskeysViewModel(deps, dispatcher, dispatcher)
        this.model = model
        store.put("passkeys", model)
        scheduler.runCurrent()
        show { PasskeysRoute(model, onBack) }
        scheduler.runCurrent()
        compose.waitForIdle()
        return model
    }

    @After fun clearModel() =
        runTest(dispatcher, timeout = 60.seconds) {
            withContext(NonCancellable) {
                store.clear()
                model
                    ?.viewModelScope
                    ?.coroutineContext
                    ?.job
                    ?.cancelAndJoin()
                scope.coroutineContext.job.cancelAndJoin()
            }
        }
}
