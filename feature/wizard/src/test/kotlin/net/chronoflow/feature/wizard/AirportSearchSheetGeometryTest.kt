@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
class AirportSearchSheetGeometryTest(
    private val profile: DeviceProfile,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile)).around(compose)

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
        assertEquals(44f, bounds("wizard.airport.code.AMS").width, 0.5f)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = listOf(DeviceProfiles.pixel8, DeviceProfiles.foldInner)
    }
}
