package net.chronoflow.feature.settings

import net.chronoflow.core.testing.compose.BoardCaptureCell
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
    private val cell: BoardCaptureCell,
) : SettingsScreenTest(cell.profile, cell.language, cell.dark) {
    @Test fun capture() {
        show { SettingsBoardFixture(state, wide = profile.widthDp >= 600, nativeSheet = true, shell = true) }
        captureSettingsBoard(
            compose,
            "${board}_${cell.suffix}",
            window = state.startsWith("Delete"),
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-{2}")
        fun cases(): List<Array<Any>> =
            listOf(
                "SetPasskeys" to "PasskeysList",
                "SetPasskeysEmpty" to "PasskeysEmpty",
                "SetDelete" to "DeleteConfirmation",
                "PasswordPromptDelete" to "DeletePrompt",
                "SetPlan" to "PlanFailed",
                "SetPlanCredits" to "PlanPending",
                "SetPlanLoading" to "PlanLoading",
                "SetPlanNoLink" to "PlanCredits",
                "SetPlanOffline" to "PlanOffline",
            ).flatMap { (board, state) ->
                BoardCaptureCell.standard.map { cell -> arrayOf<Any>(board, state, cell) }
            }
    }
}
