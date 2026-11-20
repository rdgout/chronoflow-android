@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.LayoutCase
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.wizard.shell.WizardScreen
import net.chronoflow.feature.wizard.ui.WizardUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WizardFlightsGuardsTest(
    private val case: LayoutCase,
    private val name: String,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(case)).around(compose)

    @Test fun screenFitsTheProfileAndSafeArea() {
        lateinit var state: WizardUiState
        runTest(timeout = 60.seconds) { state = boardState("Wizard$name", Locale.forLanguageTag(case.language.tag)) }
        compose.setContent {
            ProfileHost(
                case.profile,
            ) { ChronoFlowTheme(darkTheme = false, reduceMotion = true) { WizardScreen(state, {}) } }
        }
        Guards.assertTextFits(compose, case.profile)
        Guards.assertTouchTargets(compose, case.profile)
        Guards.assertInteractiveInSafeArea(compose, case.profile, rootTag = "wizard.window")
        Guards.assertNoInteractiveOverlap(compose, rootTag = "wizard.window")
        Guards.assertHeadings(compose, "wizard.title")
        state.times
            ?.cards
            ?.firstOrNull()
            ?.let { Guards.assertHeadings(compose, "wizard.flight.${it.index}.route") }
        if (name == "OutboundThree" && case.language == AppLanguage.Nl &&
            case.profile.name in setOf("small", "largeFont")
        ) {
            val before = compose.onNodeWithTag("wizard.outbound.continue").getUnclippedBoundsInRoot()
            compose.onNodeWithTag("wizard.flight.2").performScrollTo()
            assertEquals(before, compose.onNodeWithTag("wizard.outbound.continue").getUnclippedBoundsInRoot())
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun cases(): List<Array<Any>> {
            val states =
                listOf(
                    "Outbound",
                    "OutboundEmpty",
                    "OutboundErrors",
                    "OutboundTwoErrors",
                    "OutboundStop",
                    "OutboundStopEmpty",
                    "OutboundOvernight",
                    "OutboundThree",
                    "OutboundPast",
                    "ReturnFlight",
                    "ReturnFlightPast",
                    "Return",
                )
            val nl =
                listOf(DeviceProfiles.small, DeviceProfiles.pixel8, DeviceProfiles.foldInner, DeviceProfiles.largeFont)
                    .flatMap { profile -> states.map { arrayOf<Any>(LayoutCase(profile, AppLanguage.Nl), it) } }
            val en = states.map { arrayOf<Any>(LayoutCase(DeviceProfiles.small, AppLanguage.En), it) }
            val landscape =
                listOf("Outbound", "Return").map {
                    arrayOf<Any>(LayoutCase(DeviceProfiles.landscapeSideNav, AppLanguage.Nl), it)
                }
            return (nl + en + landscape).also { check(it.size == 62) }
        }
    }
}
