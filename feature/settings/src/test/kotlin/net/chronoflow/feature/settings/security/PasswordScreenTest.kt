@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.security

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.settings.SettingsDebugCounters
import net.chronoflow.feature.settings.support.FakeSettingsDependencies
import net.chronoflow.feature.settings.support.SettingsScreenTest
import net.chronoflow.feature.settings.support.settingsNow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PasswordScreenTest : SettingsScreenTest() {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val store = ViewModelStore()
    private var model: SecurityViewModel? = null

    @After fun clearModel() =
        runTest(dispatcher, timeout = 60.seconds) {
            store.clear()
            model
                ?.viewModelScope
                ?.coroutineContext
                ?.job
                ?.cancelAndJoin()
        }

    @Test fun testPasswordFieldsSubmitAndShowFeedback() {
        val deps = FakeSettingsDependencies()
        val vm = SecurityViewModel(deps, dispatcher, dispatcher, dispatcher)
        model = vm
        store.put("security", vm)
        scheduler.runCurrent()
        SettingsDebugCounters.securityFeedback.set(0)
        show { PasswordRoute(vm, {}) }
        compose.onNodeWithTag("settings.password.current").performTextInput("password")
        compose.onNodeWithTag("settings.password.new").performTextInput("New password1!")
        compose.onNodeWithTag("settings.password.confirmation").performTextInput("New password1!")
        compose.onNodeWithTag("settings.password.save").performClick()
        scheduler.runCurrent()
        compose.waitUntil(1000) { SettingsDebugCounters.securityFeedback.get() == 1 }
        scheduler.runCurrent()
        compose.onNodeWithTag("settings.security.feedback").assertExists()
        assertEquals("Password updated", vm.state.value.message)
    }

    @Test fun noticeAndErrorsBelongToTheThreeFields() {
        val state =
            SecurityState(
                errors =
                    mapOf(
                        "current_password" to "The provided password was incorrect.",
                        "password" to "Use at least one number.",
                        "password_confirmation" to "The passwords don't match.",
                    ),
            )
        show { PasswordScreen(PasswordScreenState(state, PasswordInputs(), { settingsNow }), {}) }
        compose.onNodeWithTag("settings.password.notice").assertExists()
        compose
            .onNodeWithTag(
                "settings.password.current",
            ).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        compose.onNodeWithTag("settings.password.new").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        compose
            .onNodeWithTag(
                "settings.password.confirmation",
            ).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }

    @Test fun saveIsDisabledWhileLocked() {
        val state = SecurityState(retryAt = settingsNow.plusSeconds(60))
        show { PasswordScreen(PasswordScreenState(state, PasswordInputs(), { settingsNow }), {}) }
        compose.onNodeWithTag("settings.password.save").assertIsNotEnabled()
        compose.onNodeWithTag("settings.security.lock").assertExists()
    }
}
