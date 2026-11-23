package net.chronoflow.feature.wizard

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.wizard.model.WizardAirportSheet
import net.chronoflow.feature.wizard.shell.WizardScreen
import net.chronoflow.feature.wizard.ui.AirportSheetUi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WizardWindowCaptureTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(wizardBoardProfile)).around(compose)

    @Test fun sheetCaptureIncludesTheActivityBackdrop() {
        val state =
            screenState().copy(
                sheet = AirportSheetUi(WizardAirportSheet(0, WizardAirportSheet.Side.from), null),
            )
        compose.setContent { WizardTestHost(wizardBoardProfile) { WizardScreen(state, {}) } }
        val bitmap = wizardWindowBitmap(compose)
        assertEquals(1170, bitmap.width)
        assertEquals(2532, bitmap.height)
    }

    @Test fun alertCaptureIncludesTheActivityBackdrop() {
        compose.setContent {
            WizardTestHost(wizardBoardProfile) { WizardScreen(screenState().copy(discardDialogVisible = true), {}) }
        }
        val bitmap = wizardWindowBitmap(compose)
        assertEquals(1170, bitmap.width)
        assertEquals(2532, bitmap.height)
    }
}
