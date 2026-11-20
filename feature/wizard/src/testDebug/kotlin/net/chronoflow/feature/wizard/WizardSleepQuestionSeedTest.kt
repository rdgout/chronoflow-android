package net.chronoflow.feature.wizard

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.wizard.model.MemberWizardModel
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.core.wizard.model.exclusionWasSaved
import net.chronoflow.core.wizard.model.progress
import net.chronoflow.feature.wizard.debug.WizardBoardCaptureSeed
import net.chronoflow.feature.wizard.debug.WizardMemoryDraftStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class WizardSleepQuestionSeedTest(
    private val name: String,
) {
    @Test fun draftMatchesTheBoardContract() =
        runTest(timeout = 60.seconds) {
            val base = WizardBoardCaptureSeed.baseName(name)
            val draft = WizardBoardCaptureSeed.draft(name, Locale.ENGLISH)
            val state = draft.state
            val guest = base in setOf("WizardSleep", "PickerTime", "WizardQAge", "WizardQShift", "WizardExcluded")
            assertEquals(if (guest) WizardMode.guest else WizardMode.member, state.mode)
            val expectedStep =
                when (base) {
                    "WizardQAge", "WizardQAgeMember", "WizardQShift" -> MemberWizardStep.question
                    "WizardExcluded", "WizardExcludedShift" -> MemberWizardStep.excluded
                    "WizardSheetFreeSleep" -> MemberWizardStep.fineTune
                    else -> MemberWizardStep.sleep
                }
            assertEquals(expectedStep, state.step)
            assertTrue(state.entered)
            assertFalse(state.fromLastTrip)
            assertTrue(state.profile.keys.containsAll(listOf("habitual_bed", "habitual_wake", "chronotype")))
            assertEquals(
                "2026-10-12T14:40",
                draft.form.legs
                    .single()
                    .departLocal,
            )
            assertEquals(
                "2026-10-13T09:15",
                draft.form.legs
                    .single()
                    .arriveLocal,
            )
            if (expectedStep == MemberWizardStep.question) {
                assertEquals(if (base == "WizardQShift") 1 else 0, state.questionIndex)
                assertEquals(
                    if (base == "WizardQShift") {
                        listOf(PromptKind.AgeBand, PromptKind.Screening)
                    } else {
                        listOf(PromptKind.AgeBand)
                    },
                    state.questions?.map { it.kind },
                )
                assertEquals(
                    if (base == "WizardQShift") "18_64" else null,
                    (state.profile["age_band"] as? kotlinx.serialization.json.JsonPrimitive)?.content,
                )
            }
            assertEquals(
                if (base == "WizardSleepError") setOf("profile.habitual_bed") else emptySet<String>(),
                draft.fieldErrors.keys,
            )
            assertEquals(if (base == "WizardSheetFreeSleep") "free_day_times::" else null, state.promptSheetID)
            when (base) {
                "WizardExcludedShift" -> {
                    assertFalse("shift_work" in state.profile)
                    assertEquals(kotlinx.serialization.json.JsonPrimitive(true), state.savedProfile?.get("shiftWork"))
                    assertEquals(PlanStatus.NoPlan, state.review?.status)
                    assertEquals(
                        listOf(
                            SHIFT_REASON,
                        ),
                        state.review?.reasons?.map { it.key },
                    )
                }

                "WizardExcluded" -> {
                    assertEquals(PlanStatus.NoPlan, state.review?.status)
                    assertEquals(
                        listOf("This plan is not available for travellers under 18."),
                        state.review?.reasons?.map { it.key },
                    )
                }

                "WizardSheetFreeSleep" -> {
                    assertEquals(PlanStatus.Ok, state.review?.status)
                    assertEquals(
                        PromptKind.FreeDayTimes,
                        state.review
                            ?.prompts
                            ?.single()
                            ?.kind,
                    )
                    assertEquals(setOf("freeDayStatus", "freeDayBed", "freeDayWake"), state.savedProfile?.keys)
                }

                else -> {
                    assertEquals(null, state.review)
                }
            }
            val model =
                MemberWizardModel(
                    draft,
                    WizardMemoryDraftStore(),
                    backgroundScope,
                    { WizardBoardCaptureSeed.now },
                )
            try {
                assertEquals(base == "WizardExcludedShift", model.exclusionWasSaved)
                val progress =
                    when (base) {
                        "WizardSleep", "PickerTime" -> .30
                        "WizardSleepMember", "WizardSleepError" -> .50
                        "WizardQAge" -> .54
                        "WizardQShift" -> .64
                        "WizardSheetFreeSleep" -> .80
                        else -> .60
                    }
                assertEquals(progress, model.progress, .00001)
            } finally {
                model.close()
            }
            assertEquals(
                if (name == "PickerTime") "habitual_bed" else null,
                WizardBoardCaptureSeed.presentation(name).timeSheet,
            )
        }

    @Test fun retiredBoardsAreNeverKnown() {
        assertFalse(WizardBoardCaptureSeed.knows("WizardQBipolar"))
        assertFalse(WizardBoardCaptureSeed.knows("WizardQCaffeine"))
        assertFalse(WizardBoardCaptureSeed.knows("WizardQClot"))
        assertFalse(WizardBoardCaptureSeed.knows("WizardQEye"))
        assertFalse(WizardBoardCaptureSeed.knows("WizardQPregnant"))
    }

    companion object {
        private val SHIFT_REASON =
            "This plan is not available if, in the past 14 days, you have worked " +
                "night or rotating shifts or had a very irregular sleep schedule."
        private val bases =
            setOf(
                "WizardSleep",
                "WizardSleepMember",
                "WizardSleepError",
                "PickerTime",
                "WizardQAge",
                "WizardQAgeMember",
                "WizardQShift",
                "WizardExcluded",
                "WizardExcludedShift",
                "WizardSheetFreeSleep",
            )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases() = WizardBoardCaptureSeed.names.filter { WizardBoardCaptureSeed.baseName(it) in bases }
    }
}
