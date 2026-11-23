package net.chronoflow.feature.wizard

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.wizard.model.WizardAirportSheet
import net.chronoflow.feature.wizard.shell.WizardScreen
import net.chronoflow.feature.wizard.ui.AirportRowUi
import net.chronoflow.feature.wizard.ui.AirportSearchUi
import net.chronoflow.feature.wizard.ui.AirportSheetUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class AirportCodeLayoutTest(
    private val profile: DeviceProfile,
    private val language: AppLanguage,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile, language)).around(compose)

    @Test fun airportCodeStaysOnOneLineWithoutClipping() {
        val state =
            screenState().copy(
                sheet = AirportSheetUi(WizardAirportSheet(0, WizardAirportSheet.Side.from), null),
                search = AirportSearchUi.Results(listOf(AirportRowUi.from(AMS).copy(iata = "AMM"))),
            )
        compose.setContent { WizardTestHost(profile) { WizardScreen(state, {}) } }
        val layouts = mutableListOf<TextLayoutResult>()
        compose
            .onNodeWithTag("wizard.airport.code.AMM", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        assertEquals("TripAirportSearch keeps its IATA code on one line", 1, layout.lineCount)
        assertTrue(
            "The complete airport code must fit: right=${layout.getLineRight(0)}, width=${layout.size.width}",
            layout.getLineRight(0) <= layout.size.width + 1,
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf(DeviceProfiles.pixel8, AppLanguage.En),
                arrayOf(DeviceProfiles.largeFont, AppLanguage.Nl),
            )
    }
}
