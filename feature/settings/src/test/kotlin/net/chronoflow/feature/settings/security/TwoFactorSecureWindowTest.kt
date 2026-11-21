package net.chronoflow.feature.settings.security

import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import net.chronoflow.core.model.api.ApiTwoFactorSetup
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.settings.support.SettingsScreenTest
import net.chronoflow.feature.settings.support.settingsNow
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TwoFactorSecureWindowTest(
    private val name: String,
    private val expected: Boolean,
) : SettingsScreenTest() {
    @Test fun secureOnlyWhileTheSecretStateIsShown() {
        val visible = mutableStateOf(true)
        val security =
            when (name) {
                "setup" -> SecurityState(setup = ApiTwoFactorSetup("secret", "otpauth://test"))
                "codes" -> SecurityState(codes = listOf("secret-code"))
                else -> SecurityState()
            }
        val state =
            TwoFactorScreenState(
                SecurityUiState(summary = SecuritySummary(name == "manage", null, 0), security = security),
                TwoFactorInputs(),
                { settingsNow },
            )
        show { if (visible.value) TwoFactorScreen(state, {}) }
        compose.runOnIdle {
            assertEquals(expected, activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
            visible.value = false
        }
        compose.runOnIdle {
            assertEquals(0, activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf("intro", false),
                arrayOf("setup", true),
                arrayOf("codes", true),
                arrayOf("manage", false),
            )
    }
}
