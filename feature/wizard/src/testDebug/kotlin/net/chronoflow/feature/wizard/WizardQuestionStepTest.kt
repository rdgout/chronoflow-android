package net.chronoflow.feature.wizard

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.RecordingHaptics
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardScreen
import net.chronoflow.feature.wizard.ui.WizardUiState
import org.junit.Assert.assertEquals
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

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WizardQuestionStepTest(
    private val board: String,
    private val language: AppLanguage,
    private val titleSize: Int,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8, language)).around(compose)
    private val haptics = RecordingHaptics()
    private val events = mutableListOf<WizardEvent>()
    private lateinit var formatter: CopyLineFormatter

    private fun state(): WizardUiState {
        lateinit var state: WizardUiState
        runTest(timeout = 60.seconds) {
            state = boardState(board, Locale.forLanguageTag(language.tag))
            formatter = wizardCopyFormatter()
        }
        return state
    }

    @Test fun questionHasTheDesignedTitleAndRadioStructure() {
        val state = state()
        val question = state.question!!
        compose.setContent { WizardTestHost { WizardScreen(state, formatter, events::add) } }
        compose
            .onNodeWithTag("wizard.eyebrow")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Heading))
        compose.onNodeWithTag("wizard.title").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag("wizard.title").performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            assertTrue(it(results))
        }
        assertEquals(
            titleSize.sp,
            results
                .single()
                .layoutInput.style.fontSize,
        )
        compose
            .onNodeWithTag(question.options.first().tag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        compose
            .onNodeWithTag(question.options[1].tag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        compose
            .onNodeWithTag("wizard.question.options")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
        compose.onNodeWithTag("wizard.question.why").assertExists()
        if (question.disclaimer) {
            compose.onNodeWithTag("wizard.question.disclaimer").assertExists()
        } else {
            compose.onNodeWithTag("wizard.question.disclaimer").assertDoesNotExist()
        }
        compose.onNodeWithTag("wizard.question.${question.index}.continue").assertDoesNotExist()
        compose.onNodeWithTag("wizard.question.hint").assertExists()
    }

    @Test fun tapEmitsAnAnswerAndOneSelection() {
        val state = state()
        compose.setContent {
            WizardTestHost {
                CompositionLocalProvider(LocalHaptics provides haptics) { WizardScreen(state, formatter, events::add) }
            }
        }
        val first = state.question!!.options.first()
        compose.onNodeWithTag(first.tag).performClick()
        assertEquals(listOf(WizardEvent.AnswerQuestion(first.value)), events)
        assertEquals(listOf(HapticEvent.Selection), haptics.events)
    }

    @Test fun pendingDisablesCardsWithoutChangingTheDrawnBorder() {
        val initial = state()
        val state = mutableStateOf(initial)
        val first =
            initial.question!!
                .options
                .first()
                .tag
        val second = initial.question.options[1].tag
        compose.setContent { WizardTestHost { WizardScreen(state.value, formatter, events::add) } }
        val before = compose.onNodeWithTag(first).captureToImage().toPixelMap()
        val border = before[1, before.height / 2]
        val beforeSecond = compose.onNodeWithTag(second).captureToImage().toPixelMap()
        val secondBorder = beforeSecond[1, beforeSecond.height / 2]
        compose.runOnIdle { state.value = initial.copy(question = initial.question.copy(pending = true)) }
        compose.onNodeWithTag(first).assertIsNotEnabled()
        compose.onNodeWithTag(second).assertIsNotEnabled()
        val after = compose.onNodeWithTag(first).captureToImage().toPixelMap()
        assertEquals(border, after[1, after.height / 2])
        val afterSecond = compose.onNodeWithTag(second).captureToImage().toPixelMap()
        assertEquals(secondBorder, afterSecond[1, afterSecond.height / 2])
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-{2}sp")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf("WizardQAge", AppLanguage.En, 30),
                arrayOf("WizardQAgeMember", AppLanguage.En, 30),
                arrayOf("WizardQShift", AppLanguage.En, 24),
                arrayOf("WizardQShift", AppLanguage.Nl, 24),
            )
    }
}
