@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.wizard

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.ApiReviewPrompt
import net.chronoflow.core.model.contracts.ApiReviewPromptShape
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.plan.CopyCatalog
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.testing.Fixtures
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.feature.wizard.questions.WizardQuestionUi
import net.chronoflow.feature.wizard.shell.WizardEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

internal fun questionPrompts(): List<ApiReviewPrompt> =
    ApiJson.response
        .decodeFromString(
            ApiTripReview.serializer(),
            Fixtures.text("wizard/needs-answers.json"),
        ).prompts

internal fun questionDraft(
    index: Int = 0,
    prompts: List<ApiReviewPrompt> = questionPrompts(),
): MemberWizardDraft {
    val base = sleepDraft()
    return base.copy(
        state =
            base.state.copy(
                step = MemberWizardStep.question,
                lastInputStep = MemberWizardStep.sleep,
                sleepCompleted = true,
                questions = prompts,
                questionIndex = index,
                profile =
                    if (index ==
                        0
                    ) {
                        base.state.profile
                    } else {
                        JsonObject(base.state.profile + ("age_band" to JsonPrimitive("18_64")))
                    },
            ),
    )
}

internal fun questionPrompt(
    kind: PromptKind = PromptKind.AgeBand,
    field: String = "age_band",
    options: List<String> = listOf("under_18", "18_64", "65_plus"),
): ApiReviewPrompt {
    val base =
        questionPrompts().first {
            if (kind ==
                PromptKind.Screening
            ) {
                it.kind == kind
            } else {
                it.kind == PromptKind.AgeBand
            }
        }
    return ApiReviewPrompt(
        base.id,
        kind,
        base.blocking,
        base.required,
        base.text,
        base.scope,
        base.target,
        listOf(field),
        emptyMap(),
        ApiReviewPromptShape(base.shape.kind, options, base.shape.min, base.shape.max),
        base.side,
        base.deviceMin,
    )
}

class TripWizardViewModelQuestionTest {
    @get:Rule val main: org.junit.rules.TestWatcher = WizardMainDispatcherRule()

    @Test fun ageOptionsFollowTheShapeOrder() {
        val prompt = questionPrompt(options = listOf("65_plus", "under_18", "18_64"))
        assertEquals(prompt.shape.options, WizardQuestionUi.options(prompt).map { it.value.content })
    }

    @Test fun shiftOptionsAreNoThenYes() {
        assertEquals(
            listOf(JsonPrimitive(false), JsonPrimitive(true)),
            WizardQuestionUi.options(questionPrompts().last()).map { it.value },
        )
    }

    @Test fun answerWaitsTwoHundredFiftyMillisecondsThenShowsTheNextQuestion() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = questionDraft() }) { fixture ->
                runCurrent()
                fixture.model.onEvent(WizardEvent.AnswerQuestion(JsonPrimitive("18_64")))
                runCurrent()
                fixture.deps.checking.advance(249)
                assertEquals(
                    0,
                    fixture.model.uiState.value!!
                        .question!!
                        .index,
                )
                assertTrue(
                    fixture.model.uiState.value!!
                        .question!!
                        .pending,
                )
                fixture.deps.checking.advance(250)
                assertEquals(
                    1,
                    fixture.model.uiState.value!!
                        .question!!
                        .index,
                )
                assertFalse(
                    fixture.model.uiState.value!!
                        .question!!
                        .pending,
                )
            }
        }

    @Test fun lastAnswerRechecksWithBothAnswers() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = questionDraft(index = 1) }) { fixture ->
                runCurrent()
                fixture.model.onEvent(WizardEvent.AnswerQuestion(JsonPrimitive(false)))
                runCurrent()
                fixture.deps.checking.advance(250)
                val state =
                    fixture.model.wizard!!
                        .draft.state
                assertEquals(MemberWizardStep.checking, state.step)
                assertEquals(true, state.checkingAnswers)
                assertEquals(JsonPrimitive("18_64"), state.profile["age_band"])
                assertEquals(JsonPrimitive(false), state.profile["shift_work"])
            }
        }

    @Test fun secondTapDuringThePauseIsIgnored() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = questionDraft() }) { fixture ->
                runCurrent()
                fixture.model.onEvent(WizardEvent.AnswerQuestion(JsonPrimitive("under_18")))
                fixture.model.onEvent(WizardEvent.AnswerQuestion(JsonPrimitive("18_64")))
                runCurrent()
                assertEquals(
                    JsonPrimitive("under_18"),
                    fixture.model.wizard!!
                        .draft.state.profile["age_band"],
                )
                fixture.deps.checking.advance(250)
                assertEquals(
                    1,
                    fixture.model.wizard!!
                        .draft.state.questionIndex,
                )
            }
        }

    @Test fun backFromTheFirstQuestionSkipsChecking() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = questionDraft() }) { fixture ->
                runCurrent()
                fixture.model.onEvent(WizardEvent.Back)
                runCurrent()
                assertEquals(
                    MemberWizardStep.sleep,
                    fixture.model.wizard!!
                        .draft.state.step,
                )
            }
        }

    @Test fun backDuringThePauseCancelsTheAnswer() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = questionDraft() }) { fixture ->
                runCurrent()
                fixture.model.onEvent(WizardEvent.AnswerQuestion(JsonPrimitive("18_64")))
                fixture.model.onEvent(WizardEvent.Back)
                runCurrent()
                fixture.deps.checking.advance(250)
                assertEquals(
                    MemberWizardStep.sleep,
                    fixture.model.wizard!!
                        .draft.state.step,
                )
                assertFalse(fixture.model.wizard!!.choicePending)
            }
        }

    @Test fun questionUiCarriesOnlyTheSupportedField() {
        val ui =
            WizardQuestionUi.from(
                questionDraft(prompts = listOf(questionPrompt(PromptKind.Screening, "unknown"))),
                false,
                true,
            )!!
        assertTrue(ui.options.isEmpty())
    }

    @Test fun unknownAgeOptionKeepsItsRawValue() {
        val option = WizardQuestionUi.options(questionPrompt(options = listOf("future"))).single()
        assertEquals("future", option.rawTitle)
        assertEquals(JsonPrimitive("future"), option.value)
        assertEquals(null, option.title)
    }

    @Test fun disclaimerOnlyForGuestAge() {
        val member = questionDraft()
        val guest = member.copy(state = member.state.copy(mode = net.chronoflow.core.wizard.model.WizardMode.guest))
        assertFalse(WizardQuestionUi.from(member, false, true)!!.disclaimer)
        assertTrue(WizardQuestionUi.from(guest, false, true)!!.disclaimer)
        assertFalse(
            WizardQuestionUi.from(guest.copy(state = guest.state.copy(questionIndex = 1)), false, true)!!.disclaimer,
        )
    }

    @Test fun eyebrowAndHintAreSet() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = questionDraft() }) { fixture ->
                runCurrent()
                val ui = fixture.model.uiState.value!!
                assertEquals("A few quick questions", ui.question!!.eyebrow.key)
                assertEquals("Tap an answer to continue.", ui.chrome.footerHint?.key)
                assertEquals(null, ui.chrome.footerId)
            }
        }

    @Test fun copyFormatterLoadsOnce() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft = questionDraft()
                formatter = CopyLineFormatter(CopyCatalog("""{"entries":[]}""".encodeToByteArray()))
            }) { fixture ->
                runCurrent()
                assertEquals(1, fixture.deps.formatterCalls)
                assertEquals(fixture.deps.formatter, fixture.model.copyFormatter.value)
                fixture.model.onEvent(WizardEvent.AnswerQuestion(JsonPrimitive("18_64")))
                runCurrent()
                fixture.deps.checking.advance(250)
                assertEquals(1, fixture.deps.formatterCalls)
            }
        }
}
