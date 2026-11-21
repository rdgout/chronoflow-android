@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.settings

import android.os.Bundle
import android.os.Parcel
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.settings.security.SecretField
import net.chronoflow.feature.settings.security.SecurityViewModel
import net.chronoflow.feature.settings.security.SettingsSecurityPrompt
import net.chronoflow.feature.settings.security.TwoFactorRoute
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.seconds

class SecurityMemoryActivity : ComponentActivity() {
    lateinit var security: SecurityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dependencies = checkNotNull(deps)
        val dispatcher = checkNotNull(main)
        val factory =
            viewModelFactory {
                initializer { SecurityViewModel(dependencies, dispatcher, dispatcher, dispatcher) }
            }
        security = ViewModelProvider(this, factory)[SecurityViewModel::class.java]
        setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(reduceMotion = true) { TwoFactorRoute(security, {}) }
            }
        }
    }

    companion object {
        internal var deps: SecuritySavedStateDependencies? = null
        internal var main: kotlinx.coroutines.CoroutineDispatcher? = null
    }
}

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SecuritySecretsNotSavedTest {
    private val compose = createEmptyComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    @Test fun setupSurvivesRecreationAndThePromptReopensEmptyWithoutSavedSecrets() = verify(codes = false)

    @Test fun recoveryCodesSurviveRecreationAndThePromptReopensEmptyWithoutSavedSecrets() = verify(codes = true)

    private fun verify(codes: Boolean) {
        val scheduler = TestCoroutineScheduler()
        val dispatcher = StandardTestDispatcher(scheduler)
        val deps = SecuritySavedStateDependencies()
        SecurityMemoryActivity.deps = deps
        SecurityMemoryActivity.main = dispatcher
        var host = Robolectric.buildActivity(SecurityMemoryActivity::class.java).setup()
        val model = host.get().security
        try {
            scheduler.runCurrent()
            val password =
                object : SecretField {
                    override fun read() = "password"

                    override fun clear() = Unit
                }
            model.showPrompt(if (codes) SettingsSecurityPrompt.Reveal else SettingsSecurityPrompt.Enable)
            model.submitPrompt(password)
            scheduler.runCurrent()
            compose.waitForIdle()
            model.showPrompt(SettingsSecurityPrompt.Regenerate)
            scheduler.runCurrent()
            compose.onNodeWithTag("settings.security.prompt.password").performTextInput("hunter2")
            val saved = Bundle()
            host.saveInstanceState(saved)
            val parcel = Parcel.obtain()
            val bytes =
                try {
                    saved.writeToParcel(parcel, 0)
                    parcel.marshall()
                } finally {
                    parcel.recycle()
                }
            val needles = deps.codes + listOf(deps.setup.secretKey, deps.setup.otpauthUrl, "hunter2", "123456")
            assertFalse(needles.any { contains(bytes, it.toByteArray(Charsets.UTF_8)) })
            assertFalse(needles.any { contains(bytes, it.toByteArray(Charsets.UTF_16LE)) })
            host = host.recreate()
            scheduler.runCurrent()
            compose.waitForIdle()
            assertSame(model, host.get().security)
            if (codes) {
                assertTrue(
                    model.state.value.codes
                        .isNotEmpty(),
                )
            } else {
                assertNotNull(model.state.value.setup)
            }
            compose.onNodeWithTag("settings.security.prompt.password").assertExists()
            compose.onNodeWithTag("settings.security.prompt.submit").assertIsNotEnabled()
        } finally {
            host.pause().stop().destroy()
            runTest(dispatcher, timeout = 60.seconds) {
                model.viewModelScope.coroutineContext.job
                    .cancelAndJoin()
            }
            SecurityMemoryActivity.deps = null
            SecurityMemoryActivity.main = null
        }
    }

    private fun contains(
        bytes: ByteArray,
        needle: ByteArray,
    ): Boolean = (0..bytes.size - needle.size).any { offset -> needle.indices.all { bytes[offset + it] == needle[it] } }
}
