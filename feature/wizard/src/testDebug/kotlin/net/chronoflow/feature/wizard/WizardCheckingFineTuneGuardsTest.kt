package net.chronoflow.feature.wizard

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.LayoutCase
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.wizard.shell.WizardScreen
import org.junit.Assert.assertTrue
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
class WizardCheckingFineTuneGuardsTest(
    private val case: LayoutCase,
    private val board: String,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(case)).around(compose)

    @Test fun screenFitsTheProfileAndSafeArea() {
        val state = completionBoard(board, Locale.forLanguageTag(case.language.tag))
        lateinit var formatter: CopyLineFormatter
        runTest(timeout = 60.seconds) { formatter = wizardCopyFormatter() }
        compose.setContent { WizardTestHost(case.profile) { WizardScreen(state, formatter, {}) } }
        Guards.assertTextFits(compose, case.profile)
        Guards.assertTouchTargets(
            compose,
            case.profile,
            nativeTouchTargets =
                hasAnyAncestor(hasTestTag("wizard.answer.latest_wake")) and
                    SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button) and
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected),
        )
        Guards.assertInteractiveInSafeArea(compose, case.profile, rootTag = "wizard.window")
        Guards.assertNoInteractiveOverlap(compose, rootTag = "wizard.window")
        Guards.assertHeadings(compose, if (state.answerSheet == null) "wizard.title" else "wizard.answer.title")
        val checkScroll =
            case.language == AppLanguage.Nl &&
                case.profile.name in setOf("small", "largeFont")
        if (checkScroll && board in setOf("WizardFineTuneSleep", "WizardSheetBusy", "WizardSheetDrinks")) {
            val sheet = state.answerSheet != null
            compose
                .onNodeWithTag(if (sheet) "wizard.answer.scroll" else "wizard.scroll")
                .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            val done =
                compose
                    .onNodeWithTag(if (sheet) "wizard.answer.done" else "wizard.fine-tune.continue")
                    .getUnclippedBoundsInRoot()
            assertTrue(done.top.value >= case.profile.insets.top)
            assertTrue(done.bottom.value <= case.profile.heightDp - case.profile.insets.bottom)
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun cases(): List<Array<Any>> {
            val states =
                listOf(
                    "Checking",
                    "CheckingAgain",
                    "CheckingSlow",
                    "CheckOffline",
                    "CheckServer",
                    "CheckThrottle",
                    "FineTune",
                    "FineTuneMulti",
                    "FineTuneSleep",
                    "FineTuneBusy",
                    "FineTuneNone",
                    "Sheet",
                    "SheetRoom",
                    "SheetCheckout",
                    "SheetWake",
                    "SheetBusy",
                    "SheetDrinks",
                    "SheetDrinksError",
                    "SheetLight",
                    "SheetBlue",
                    "SheetBlueError",
                )
            val dutch =
                listOf(
                    DeviceProfiles.small,
                    DeviceProfiles.pixel8,
                    DeviceProfiles.foldInner,
                    DeviceProfiles.largeFont,
                ).flatMap { profile ->
                    states.map { arrayOf<Any>(LayoutCase(profile, AppLanguage.Nl), "Wizard$it") }
                }
            val english = states.map { arrayOf<Any>(LayoutCase(DeviceProfiles.small, AppLanguage.En), "Wizard$it") }
            val side =
                listOf("FineTune", "Sheet").map {
                    arrayOf<Any>(LayoutCase(DeviceProfiles.landscapeSideNav, AppLanguage.Nl), "Wizard$it")
                }
            return (dutch + english + side).also { check(it.size == 107) }
        }
    }
}
