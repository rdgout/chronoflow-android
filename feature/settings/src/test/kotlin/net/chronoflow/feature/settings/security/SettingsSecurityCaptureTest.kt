package net.chronoflow.feature.settings.security

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.BoardCaptureCell
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
    private val cell: BoardCaptureCell,
) : SettingsScreenTest(cell.profile, cell.language, cell.dark) {
    @Test fun capture() {
        show { SettingsBoardFixture(state, wide = profile.widthDp >= 600, nativeSheet = true, shell = true) }
        if (state == "PromptForgot") compose.onNodeWithTag("settings.security.prompt.forgot").performClick()
        val name = "${board}_${cell.suffix}"
        if (state == "PromptForgot") {
            BoardCapture.captureWindowStack(
                compose,
                name,
                "settings.capture.window",
                listOf(
                    if (profile.widthDp >= 600) "settings.security.prompt.dialog" else "settings.security.prompt.sheet",
                    "settings.security.prompt.forgotAlert",
                ),
            )
        } else {
            captureSettingsBoard(compose, name, window = state.startsWith("Prompt"))
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-{2}")
        fun cases(): List<Array<Any>> =
            listOf(
                "SetSecurity" to "Security",
                "SetPassword" to "PasswordError",
                "PasswordPrompt" to "Prompt",
                "PasswordPrompt2FAOn" to "PromptEnable",
                "PasswordPromptChecking" to "PromptChecking",
                "PasswordPromptForgot" to "PromptForgot",
                "PasswordPromptLocked" to "PromptLocked",
                "PasswordPromptNewCodes" to "PromptRegenerate",
                "PasswordPromptNL" to "PromptRegenerateWrong",
                "PasswordPromptWide" to "Prompt",
                "PasswordPromptWrong" to "PromptWrong",
                "Set2FA" to "TwoFactorIntro",
                "Set2FASetup" to "TwoFactorSetup",
                "Set2FAOn" to "TwoFactorOn",
                "Set2FACodes" to "TwoFactorCodes",
                "Set2FAManage" to "TwoFactorManage",
                "Set2FAManageWide" to "TwoFactorManage",
                "Set2FAWide" to "TwoFactorIntro",
            ).flatMap { (board, state) ->
                val cells =
                    when (board) {
                        "PasswordPromptWide", "Set2FAManageWide", "Set2FAWide" -> BoardCaptureCell.wide
                        "PasswordPrompt", "Set2FA", "Set2FAManage" -> BoardCaptureCell.phoneWithWideSibling
                        else -> BoardCaptureCell.standard
                    }
                cells.map { cell -> arrayOf<Any>(board, state, cell) }
            }
    }
}
