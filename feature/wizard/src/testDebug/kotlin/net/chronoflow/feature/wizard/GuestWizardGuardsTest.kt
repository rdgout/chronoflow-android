package net.chronoflow.feature.wizard

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.LayoutCase
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.wizard.shell.WizardScreen
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
class GuestWizardGuardsTest(
    private val case: LayoutCase,
    private val board: String,
    private val dark: Boolean,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(case, dark)).around(compose)

    @Test fun screenFitsTheProfileAndSafeArea() {
        val state = guestBoard(board, Locale.forLanguageTag(case.language.tag))
        lateinit var formatter: CopyLineFormatter
        runTest(timeout = 60.seconds) { formatter = wizardCopyFormatter() }
        compose.setContent {
            ProfileHost(case.profile) {
                ChronoFlowTheme(darkTheme = dark, reduceMotion = true) { WizardScreen(state, formatter, {}) }
            }
        }
        Guards.assertTextFits(compose, case.profile)
        Guards.assertTouchTargets(compose, case.profile)
        Guards.assertInteractiveInSafeArea(compose, case.profile, rootTag = "wizard.window")
        Guards.assertNoInteractiveOverlap(compose, exemptTags = setOf("wizard.route.stops"), rootTag = "wizard.window")
        Guards.assertHeadings(compose, "wizard.title")
        if (case.profile.name == "largeFont" && case.language == AppLanguage.Nl && state.ready != null) {
            compose.onNodeWithTag("wizard.ready.route.stacked", useUnmergedTree = true).assertExists()
            val footer = compose.onNodeWithTag("wizard.ready.footer").getUnclippedBoundsInRoot()
            compose.onNodeWithTag("wizard.ready.questions").performScrollTo()
            compose.onNodeWithTag("wizard.ready.route").performScrollTo()
            assertEquals(footer, compose.onNodeWithTag("wizard.ready.footer").getUnclippedBoundsInRoot())
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-dark={2}")
        fun cases(): List<Array<Any>> {
            val states =
                listOf(
                    "WhereGuest",
                    "OutboundGuest",
                    "OutboundPastGuest",
                    "CheckingGuest",
                    "FineTuneGuest",
                    "ReadyGuest",
                    "ReadyGuestFree",
                    "DiscardGuest",
                )
            val profiles =
                listOf(DeviceProfiles.small, DeviceProfiles.pixel8, DeviceProfiles.foldInner, DeviceProfiles.largeFont)
            val main =
                profiles.flatMap { profile ->
                    listOf(AppLanguage.En, AppLanguage.Nl).flatMap { language ->
                        listOf(false, true).flatMap { dark ->
                            states.map { arrayOf<Any>(LayoutCase(profile, language), "Wizard$it", dark) }
                        }
                    }
                }
            val side =
                listOf(false, true).flatMap { dark ->
                    listOf("ReadyGuest", "FineTuneGuest").map { state ->
                        arrayOf<Any>(LayoutCase(DeviceProfiles.landscapeSideNav, AppLanguage.Nl), "Wizard$state", dark)
                    }
                }
            return (main + side).also { check(it.size == 132) }
        }
    }
}
