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
class WizardGuardsTest(
    private val case: LayoutCase,
    private val name: String,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(case)).around(compose)

    @Test fun screenFitsTheProfileAndSafeArea() {
        lateinit var state: WizardUiState
        val search = if (name.startsWith("Search")) SearchBoardState.valueOf(name.removePrefix("Search")) else null
        runTest(timeout = 60.seconds) {
            state =
                boardState(
                    if (search ==
                        null
                    ) {
                        "Wizard$name"
                    } else {
                        "WizardStopSearch"
                    },
                    Locale.forLanguageTag(case.language.tag),
                    search,
                )
        }
        compose.setContent {
            ProfileHost(
                case.profile,
            ) { ChronoFlowTheme(darkTheme = false, reduceMotion = true) { WizardScreen(state, {}) } }
        }
        val root = if (state.sheet == null) "wizard.window" else "wizard.airport-search"
        Guards.assertTextFits(compose, case.profile)
        Guards.assertTouchTargets(compose, case.profile)
        Guards.assertInteractiveInSafeArea(compose, case.profile, rootTag = root)
        Guards.assertNoInteractiveOverlap(compose, exemptTags = setOf("wizard.route.stops"), rootTag = root)
        Guards.assertHeadings(compose, "wizard.title")
        if (state.sheet != null) Guards.assertHeadings(compose, "wizard.airport.title")
        if (name == "WhereLimit" && case.language == AppLanguage.Nl &&
            case.profile.name in setOf("small", "largeFont")
        ) {
            val before = compose.onNodeWithTag("wizard.where.continue").getUnclippedBoundsInRoot()
            compose.onNodeWithTag("wizard.route.limit").performScrollTo()
            assertEquals(before, compose.onNodeWithTag("wizard.where.continue").getUnclippedBoundsInRoot())
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun cases(): List<Array<Any>> {
            val states =
                listOf(
                    "Where",
                    "WhereEmpty",
                    "WhereStop",
                    "WhereStops",
                    "HomeRoute",
                    "HomeRouteStop",
                    "AirportFrom",
                    "AirportHome",
                    "StopSearch",
                    "StopSearchSecond",
                    "StopSearchHome",
                    "Discard",
                    "WhereSame",
                    "WhereLimit",
                    "SearchHint",
                    "SearchSearching",
                    "SearchEmpty",
                    "SearchFailed",
                    "SearchThrottled",
                )
            val nl =
                listOf(DeviceProfiles.small, DeviceProfiles.pixel8, DeviceProfiles.foldInner, DeviceProfiles.largeFont)
                    .flatMap { profile -> states.map { arrayOf<Any>(LayoutCase(profile, AppLanguage.Nl), it) } }
            val en = states.map { arrayOf<Any>(LayoutCase(DeviceProfiles.small, AppLanguage.En), it) }
            val landscape =
                listOf("Where", "StopSearch").map {
                    arrayOf<Any>(LayoutCase(DeviceProfiles.landscapeSideNav, AppLanguage.Nl), it)
                }
            return (nl + en + landscape).also { check(it.size == 97) }
        }
    }
}
