@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.security

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.api.ApiRecoveryCodes
import net.chronoflow.core.model.api.ApiTwoFactorSetup
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.testing.sync.snapshot
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.settings.SettingsDebugCounters
import net.chronoflow.feature.settings.support.FakeSettingsDependencies
import net.chronoflow.feature.settings.support.SettingsScreenTest
import net.chronoflow.feature.settings.support.settingsNow
import net.chronoflow.feature.settings.testing.FakeSecretField
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
class TwoFactorScreenTest : SettingsScreenTest() {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val clipboardScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val deps = FakeSettingsDependencies(clipboardScope)
    private val store = ViewModelStore()
    private var model: SecurityViewModel? = null
    private val codes =
        listOf(
            "8f3k-29dm",
            "q7zp-4wxr",
            "m2vt-8ncb",
            "h6ye-1lsa",
            "c9rk-5jtd",
            "w4bn-7ugq",
            "t1xf-3hzo",
            "p5ma-6evk",
        )

    @After fun clearModel() =
        runTest(dispatcher, timeout = 60.seconds) {
            store.clear()
            model
                ?.viewModelScope
                ?.coroutineContext
                ?.job
                ?.cancelAndJoin()
            clipboardScope.coroutineContext.job.cancelAndJoin()
        }

    private fun route(enabled: Boolean = false): SecurityViewModel {
        deps.settings.value = listOf(SettingsRecord("security", "{\"twoFactorEnabled\":$enabled}"))
        val vm = SecurityViewModel(deps, dispatcher, dispatcher, dispatcher)
        model = vm
        store.put("twoFactor", vm)
        scheduler.runCurrent()
        show { TwoFactorRoute(vm, {}) }
        scheduler.runCurrent()
        compose.waitForIdle()
        return vm
    }

    @Test fun introductionEnableOpensThePrompt() {
        route()
        compose.onNodeWithTag("settings.twoFactor.enable").performClick()
        scheduler.runCurrent()
        compose.onNodeWithTag("settings.security.prompt.submit").assertIsNotEnabled()
        assertEquals(SettingsSecurityPrompt.Enable, model?.state?.value?.prompt)
    }

    @Test fun setupHasTheKeyAndQr() {
        val security = SecurityState(setup = ApiTwoFactorSetup("JBSWY3DPEHPK3PXP", "otpauth://test"))
        val state =
            TwoFactorScreenState(
                SecurityUiState(summary = SecuritySummary(false, null, 2), security = security),
                TwoFactorInputs(),
                { settingsNow },
            )
        show { TwoFactorScreen(state, {}) }
        compose.onNodeWithTag("settings.twoFactor.secret").assertExists()
        compose.onNodeWithTag("settings.twoFactor.qr").assertExists()
        compose.onNodeWithTag("settings.twoFactor.code").assertExists()
    }

    @Test fun failedConfirmationKeepsTheSetupAndShowsTheFailure() {
        val security =
            SecurityState(
                setup = ApiTwoFactorSetup("JBSWY3DPEHPK3PXP", "otpauth://test"),
                message = "Couldn't turn on two-factor.",
            )
        val state =
            TwoFactorScreenState(
                SecurityUiState(summary = SecuritySummary(false, null, 2), security = security),
                TwoFactorInputs(),
                { settingsNow },
            )
        show { TwoFactorScreen(state, {}) }
        compose.onNodeWithTag("settings.twoFactor.failure").performScrollTo().assertExists()
        compose.onNodeWithTag("settings.twoFactor.confirm").performScrollTo().assertExists()
    }

    @Test fun codesCopyUsesTheSensitiveClipboard() {
        deps.security = { AccountSecurityResult.Codes(ApiRecoveryCodes(codes)) }
        val vm = route(enabled = true)
        vm.showPrompt(SettingsSecurityPrompt.Reveal)
        vm.submitPrompt(FakeSecretField("password"))
        scheduler.runCurrent()
        compose.onAllNodesWithTag("settings.twoFactor.recoveryCode").assertCountEquals(8)
        compose.onNodeWithTag("settings.twoFactor.copyCodes").performScrollTo().performClick()
        assertEquals(codes.joinToString("\n"), deps.clipboardPort.text)
        assertTrue(deps.clipboardPort.sensitive)
    }

    @Test fun confirmationPresentsTheFeedbackOnce() {
        deps.security = { change ->
            if (change is AccountSecurityChange.EnableTwoFactor) {
                AccountSecurityResult.Setup(ApiTwoFactorSetup("JBSWY3DPEHPK3PXP", "otpauth://test"))
            } else {
                AccountSecurityResult.Codes(ApiRecoveryCodes(codes))
            }
        }
        val vm = route()
        vm.showPrompt(SettingsSecurityPrompt.Enable)
        vm.submitPrompt(FakeSecretField("password"))
        scheduler.runCurrent()
        SettingsDebugCounters.securityFeedback.set(0)
        compose.onNodeWithTag("settings.twoFactor.code").performScrollTo().performTextInput("123456")
        compose.onNodeWithTag("settings.twoFactor.confirm").performScrollTo().performClick()
        scheduler.runCurrent()
        compose.waitUntil(1000) { SettingsDebugCounters.securityFeedback.get() == 1 }
        scheduler.runCurrent()
        compose.onAllNodesWithTag("settings.twoFactor.recoveryCode").assertCountEquals(8)
    }

    @Test fun promptReopensWithAnEmptyField() {
        route(enabled = true)
        compose.onNodeWithTag("settings.twoFactor.disable").performScrollTo().performClick()
        scheduler.runCurrent()
        compose.onNodeWithTag("settings.security.prompt.password").performTextInput("hunter2")
        compose.onNodeWithTag("settings.security.prompt.cancel").performClick()
        scheduler.runCurrent()
        compose.onNodeWithTag("settings.twoFactor.disable").performScrollTo().performClick()
        scheduler.runCurrent()
        compose.onNodeWithTag("settings.security.prompt.submit").assertIsNotEnabled()
    }

    @Test fun testEnabledTwoFactorCanCancelThenDisableAndReturnToIntroduction() {
        deps.security = { change ->
            assertEquals(AccountSecurityChange.DisableTwoFactor("password"), change)
            deps.settings.value = listOf(SettingsRecord("security", "{\"twoFactorEnabled\":false}"))
            AccountSecurityResult.Snapshot(snapshot())
        }
        route(enabled = true)
        compose.onNodeWithTag("settings.twoFactor.disable").performScrollTo().performClick()
        scheduler.runCurrent()
        compose.onNodeWithTag("settings.security.prompt.password").assertExists()
        compose.onNodeWithTag("settings.security.prompt.cancel").performClick()
        scheduler.runCurrent()
        compose.onNodeWithTag("settings.security.prompt.password").assertDoesNotExist()
        compose.onNodeWithTag("settings.twoFactor.disable").performScrollTo().performClick()
        scheduler.runCurrent()
        compose.onNodeWithTag("settings.security.prompt.password").performTextInput("password")
        compose.onNodeWithTag("settings.security.prompt.submit").performClick()
        scheduler.runCurrent()
        compose.onNodeWithTag("settings.twoFactor.enable").assertExists()
        compose.onNodeWithTag("settings.twoFactor.disable").assertDoesNotExist()
    }
}
