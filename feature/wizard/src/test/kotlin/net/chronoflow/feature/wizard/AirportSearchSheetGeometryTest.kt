@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.SafeInsetsDp
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
class AirportSearchSheetGeometryTest(
    private val profile: DeviceProfile,
    private val language: AppLanguage,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile, language)).around(compose)

    private fun bounds(tag: String): Rect =
        compose.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot().let {
            Rect(it.left.value, it.top.value, it.right.value, it.bottom.value)
        }

    @Test fun sheetMatchesTheBoardAndNativeWidthCap() {
        val state =
            screenState().copy(
                sheet = AirportSheetUi(WizardAirportSheet(0, WizardAirportSheet.Side.from), null),
                search = AirportSearchUi.Results(listOf(AirportRowUi.from(AMS))),
            )
        compose.setContent { WizardTestHost(profile) { WizardScreen(state, {}) } }
        val sheet = bounds("wizard.airport-search")
        println("WizardSheet geometry: device=${profile.name}, topDp=${sheet.top}, widthDp=${sheet.width}")
        assertEquals(profile.insets.top + 56, sheet.top, 1f)
        assertTrue(sheet.width <= 641)
        assertEquals(bounds("wizard.window").center.x, sheet.center.x, 1f)
        assertEquals(40f, bounds("wizard.airport.query.box").height, 0.5f)
        assertTrue(bounds("wizard.airport.query").height >= 48)
        assertTrue(bounds("wizard.airport.AMS").height >= 60)
        assertTrue(bounds("wizard.airport.title").width <= 220.5f)
        assertEquals(44f, bounds("wizard.airport.code.AMS").width, 0.5f)
    }

    @Test fun fromTitleFitsOnOneLine() = assertTitleFitsOnOneLine(WizardAirportSheet.Side.from)

    @Test fun toTitleFitsOnOneLine() = assertTitleFitsOnOneLine(WizardAirportSheet.Side.to)

    @Test fun homeTitleFitsOnOneLine() = assertTitleFitsOnOneLine(WizardAirportSheet.Side.home)

    private fun assertTitleFitsOnOneLine(side: WizardAirportSheet.Side) {
        val state = screenState().copy(sheet = AirportSheetUi(WizardAirportSheet(0, side), null))
        compose.setContent { WizardTestHost(profile) { WizardScreen(state, {}) } }
        val title = bounds("wizard.airport.title")
        assertTrue(
            "${profile.name}/$side: title height ${title.height}",
            title.height <= requireNotNull(CfTypeScale.wizardButton.lineHeight) + 0.5f,
        )
        val sheet = bounds("wizard.airport-search")
        val cancel = bounds("wizard.airport.cancel")
        val rowStart = sheet.left + 20f
        val rowWidth = sheet.width - 40f
        val sharedSide = (rowWidth - title.width - 16f) / 2
        val leadingSide = maxOf(cancel.width, sharedSide)
        println(
            "WizardSheet title: ${profile.name}/$language/$side, heightDp=${title.height}, leftDp=${title.left}, gridLeftDp=${rowStart + leadingSide + 8f}",
        )
        assertEquals(rowStart + leadingSide + 8f, title.left, 0.5f)
        assertTrue("Cancel and title keep their gap", title.left - cancel.right >= 7.5f)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf(DeviceProfiles.pixel8, AppLanguage.En),
                arrayOf(DeviceProfiles.foldInner, AppLanguage.En),
                arrayOf(
                    DeviceProfiles.pixel8.copy(
                        name = "wizard390",
                        widthDp = 390,
                        heightDp = 844,
                        density = 3f,
                        insets = SafeInsetsDp(0f, 0f, 0f, 0f),
                        source = "Wizard 390 x 844 board",
                    ),
                    AppLanguage.Nl,
                ),
            )
    }
}
