package net.chronoflow.feature.settings.security

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.captureWindowStack
import net.chronoflow.core.testing.tiers.Capture
import net.chronoflow.feature.settings.SettingsBoardFixture
import net.chronoflow.feature.settings.captureSettingsBoard
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Capture
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsSecurityCaptureTest(
    private val board: String,
    private val state: String,
    profile: DeviceProfile,
    private val language: AppLanguage,
    dark: Boolean,
) : SettingsScreenTest(profile, language, dark) {
    @Test fun capture() {
        show { SettingsBoardFixture(state, wide = profile.widthDp >= 600, nativeSheet = true, shell = true) }
        if (state == "PromptForgot") compose.onNodeWithTag("settings.security.prompt.forgot").performClick()
        val name = "${board}_${profile.name}_${language.tag}_${if (dark) "dark" else "light"}"
        if (state == "PromptForgot") {
            BoardCapture.captureWindowStack(
                compose,
                name,
                "settings.capture.window",
                listOf(
                    "settings.security.prompt.sheet",
                    "settings.security.prompt.forgotAlert",
                ),
            )
        } else {
            captureSettingsBoard(compose, name, window = state.startsWith("Prompt"))
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-{2}-{3}-{4}")
        fun cases(): List<Array<Any>> {
            fun phone(
                board: String,
                state: String,
                language: AppLanguage = AppLanguage.En,
                dark: Boolean = false,
            ) = arrayOf<Any>(board, state, DeviceProfiles.pixel8, language, dark)

            fun wide(
                board: String,
                state: String,
            ) = arrayOf<Any>(board, state, DeviceProfiles.foldInner, AppLanguage.En, false)
            return listOf(
                phone("SetSecurity", "Security"),
                phone("SetPassword", "PasswordError"),
                phone("PasswordPrompt", "Prompt"),
                phone("PasswordPrompt2FAOn", "PromptEnable"),
                phone("PasswordPromptChecking", "PromptChecking"),
                phone("PasswordPromptForgot", "PromptForgot"),
                phone("PasswordPromptLocked", "PromptLocked"),
                phone("PasswordPromptNewCodes", "PromptRegenerate"),
                phone("PasswordPromptNL", "PromptRegenerateWrong", AppLanguage.Nl),
                wide("PasswordPromptWide", "Prompt"),
                phone("PasswordPromptWrong", "PromptWrong"),
                phone("Set2FA", "TwoFactorIntro"),
                phone("Set2FANL", "TwoFactorIntro", AppLanguage.Nl),
                phone("Set2FASetup", "TwoFactorSetup"),
                phone("Set2FAOn", "TwoFactorOn"),
                phone("Set2FACodes", "TwoFactorCodes"),
                phone("Set2FAManage", "TwoFactorManage"),
                phone("Set2FAManageNL", "TwoFactorManage", AppLanguage.Nl),
                wide("Set2FAManageWide", "TwoFactorManage"),
                wide("Set2FAWide", "TwoFactorIntro"),
                phone("SetSecurity", "Security", dark = true),
                phone("PasswordPrompt", "Prompt", dark = true),
                phone("Set2FASetup", "TwoFactorSetup", dark = true),
                phone("Set2FACodes", "TwoFactorCodes", dark = true),
            )
        }
    }
}
