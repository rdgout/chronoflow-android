package net.chronoflow.feature.wizard

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
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
import net.chronoflow.feature.wizard.ui.WizardUiState
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
class WizardSleepQuestionGuardsTest(
    private val case: LayoutCase,
    private val name: String,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(case)).around(compose)

    @Test fun screenFitsTheProfileAndSafeArea() {
        lateinit var state: WizardUiState
        lateinit var formatter: CopyLineFormatter
        runTest(timeout = 60.seconds) {
            state =
                boardState(if (name == "PickerTime") name else "Wizard$name", Locale.forLanguageTag(case.language.tag))
            formatter = wizardCopyFormatter()
        }
        compose.setContent { WizardTestHost(case.profile) { WizardScreen(state, formatter, {}) } }
        val heading =
            when {
                state.sleep?.openTime != null -> "wizard.time-sheet.title"
                state.answerSheet != null -> "wizard.answer.title"
                else -> "wizard.title"
            }
        if (heading != "wizard.title") compose.onNodeWithTag(heading).assertIsFocused()
        Guards.assertTextFits(compose, case.profile)
        Guards.assertTouchTargets(
            compose,
            case.profile,
            nativeTouchTargets =
                hasAnyAncestor(hasTestTag("wizard.time-sheet")) and
                    SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button) and
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected),
        )
        Guards.assertInteractiveInSafeArea(compose, case.profile, rootTag = "wizard.window")
        Guards.assertNoInteractiveOverlap(compose, rootTag = "wizard.window")
        Guards.assertHeadings(compose, heading)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun cases(): List<Array<Any>> {
            val states =
                listOf(
                    "Sleep",
                    "SleepError",
                    "PickerTime",
                    "SheetFreeSleep",
                    "QAge",
                    "QShift",
                    "Excluded",
                    "ExcludedShift",
                )
            val dutch =
                listOf(DeviceProfiles.small, DeviceProfiles.pixel8, DeviceProfiles.foldInner, DeviceProfiles.largeFont)
                    .flatMap { profile -> states.map { arrayOf<Any>(LayoutCase(profile, AppLanguage.Nl), it) } }
            val english = states.map { arrayOf<Any>(LayoutCase(DeviceProfiles.small, AppLanguage.En), it) }
            val side =
                listOf("Sleep", "QShift").map {
                    arrayOf<Any>(LayoutCase(DeviceProfiles.landscapeSideNav, AppLanguage.Nl), it)
                }
            return (dutch + english + side).also { check(it.size == 42) }
        }
    }
}
