package net.chronoflow.feature.settings

import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.tiers.Capture
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
class AccountPlanCaptureTest(
    private val board: String,
    private val state: String,
    dark: Boolean,
) : SettingsScreenTest(dark = dark) {
    @Test fun capture() {
        show { SettingsBoardFixture(state, nativeSheet = true, shell = true) }
        captureSettingsBoard(
            compose,
            "${board}_pixel8_en_${if (dark) "dark" else "light"}",
            window = state.startsWith("Delete"),
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-{2}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf("SetPasskeys", "PasskeysList", false),
                arrayOf("SetPasskeysEmpty", "PasskeysEmpty", false),
                arrayOf("SetDelete", "DeleteConfirmation", false),
                arrayOf("PasswordPromptDelete", "DeletePrompt", false),
                arrayOf("SetPlan", "PlanFailed", false),
                arrayOf("SetPlanCredits", "PlanPending", false),
                arrayOf("SetPlanLoading", "PlanLoading", false),
                arrayOf("SetPlanNoLink", "PlanCredits", false),
                arrayOf("SetPlanOffline", "PlanOffline", false),
                arrayOf("DarkSetPlan", "PlanFailed", true),
            )
    }
}
