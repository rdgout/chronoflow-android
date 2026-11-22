package net.chronoflow.feature.wizard

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.testing.wizard.InMemoryWizardDraftStore
import net.chronoflow.core.wizard.model.MemberWizardDrafts
import net.chronoflow.core.wizard.model.MemberWizardModel
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardCredits
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.core.wizard.model.WizardPresentation
import net.chronoflow.core.wizard.model.guestAnsweredQuestionCount
import net.chronoflow.core.wizard.model.optionalPrompts
import net.chronoflow.core.wizard.model.reviewInput
import net.chronoflow.feature.wizard.debug.GuestWizardBoardSeed
import net.chronoflow.feature.wizard.debug.WizardBoardCaptureSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class GuestWizardBoardSeedTest(
    private val board: String,
) {
    @Test fun guestBoardMatchesItsState() =
        runTest(timeout = 60.seconds) {
            val original = MemberWizardDrafts.guest()
            val draft = GuestWizardBoardSeed.draft(original, board, Locale.ENGLISH)
            val name = WizardBoardCaptureSeed.baseName(board)
            assertEquals(original.state.instance, draft.state.instance)
            assertTrue(draft.state.revision > original.state.revision)
            assertEquals(WizardMode.guest, draft.state.mode)
            assertEquals(MemberWizardStep.sleep, draft.state.lastInputStep)
            assertFalse(draft.state.firstTrip)
            assertFalse(draft.state.fromLastTrip)
            assertTrue(draft.state.sleepNeeded)
            assertNull(draft.state.savedProfile)
            assertEquals("one_way", draft.form.end.kind)
            assertFalse(draft.form.override.enabled)
            when (name) {
                "WizardWhereGuest" -> {
                    assertEquals(MemberWizardStep.whereTo, draft.state.step)
                }

                "WizardWhereGuestEmpty" -> {
                    assertEquals(MemberWizardStep.whereTo, draft.state.step)
                    assertFalse(draft.state.entered)
                }

                "WizardOutboundGuest", "WizardOutboundPastGuest", "WizardDiscardGuest" -> {
                    assertEquals(MemberWizardStep.outbound, draft.state.step)
                    assertEquals(
                        name == "WizardDiscardGuest",
                        GuestWizardBoardSeed.presentation(board).discardDialogVisible,
                    )
                    if (name == "WizardOutboundPastGuest") {
                        assertEquals(
                            "A new plan needs an upcoming arrival. Check your flight dates.",
                            draft.fieldErrors["review"]?.key,
                        )
                    }
                }

                "WizardCheckingGuest" -> {
                    assertEquals(MemberWizardStep.checking, draft.state.step)
                }

                "WizardFineTuneGuest", "WizardReadyGuest", "WizardReadyGuestFree" -> {
                    assertEquals(
                        if (name ==
                            "WizardFineTuneGuest"
                        ) {
                            MemberWizardStep.fineTune
                        } else {
                            MemberWizardStep.ready
                        },
                        draft.state.step,
                    )
                    assertEquals(true, draft.state.sleepCompleted)
                    if (name != "WizardFineTuneGuest") {
                        assertEquals(
                            "2026-10-14",
                            draft.state.review
                                ?.summary
                                ?.planEndDate,
                        )
                    }
                    assertEquals(JsonPrimitive("18_64"), draft.state.profile["age_band"])
                    assertEquals(JsonPrimitive(false), draft.state.profile["shift_work"])
                    val model =
                        MemberWizardModel(
                            draft,
                            InMemoryWizardDraftStore(),
                            backgroundScope,
                            { WizardBoardCaptureSeed.now },
                        )
                    assertEquals(2, model.guestAnsweredQuestionCount)
                    if (name == "WizardFineTuneGuest") {
                        assertEquals(
                            listOf(
                                PromptKind.Room,
                                PromptKind.LatestWake,
                                PromptKind.SeatSide,
                                PromptKind.Cups,
                                PromptKind.LightDevice,
                                PromptKind.BlueBlockers,
                            ),
                            model.optionalPrompts.map { it.kind },
                        )
                        val input = draft.form.reviewInput(draft.form.identity, draft.state.profile)
                        assertEquals(
                            List(6) { "Add" },
                            model.optionalPrompts.map { prompt ->
                                WizardPresentation.promptAnswerText(prompt, input, Language.En) { key, _ -> key }
                            },
                        )
                    }
                    assertFalse(
                        model.optionalPrompts.any {
                            it.kind in
                                setOf(PromptKind.LateCheckOut, PromptKind.PreDepartureBusyHours)
                        },
                    )
                    if (name ==
                        "WizardReadyGuestFree"
                    ) {
                        assertEquals(WizardCredits.free, WizardCredits.of(checkNotNull(draft.state.review)))
                    } else {
                        assertEquals(
                            1,
                            draft.state.review
                                ?.price
                                ?.cost,
                        )
                        assertEquals(
                            3,
                            draft.state.review
                                ?.price
                                ?.balance,
                        )
                    }
                }

                "WizardGuestJourney" -> {
                    assertEquals(MemberWizardStep.whereTo, draft.state.step)
                    assertEquals(
                        "HND",
                        draft.form.legs
                            .last()
                            .toIata,
                    )
                    val departure =
                        checkNotNull(
                            draft.form.legs
                                .first()
                                .departLocal,
                        )
                    assertTrue(departure >= "2026-10-30")
                    assertEquals(JsonPrimitive(false), draft.state.profile["shift_work"])
                    assertNull(GuestWizardBoardSeed.uiOverride(board))
                }
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            (
                GuestWizardBoardSeed.names +
                    listOf("WizardWhereGuestEmpty", "WizardGuestJourney")
            ).map { arrayOf(it) }
    }
}
