package net.chronoflow.feature.wizard

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.datamock.MockWizardPrompts
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.wizard.model.MemberWizardModel
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.inputFingerprint
import net.chronoflow.core.wizard.model.progress
import net.chronoflow.core.wizard.model.reviewInput
import net.chronoflow.feature.wizard.debug.WizardBoardCaptureSeed
import net.chronoflow.feature.wizard.debug.WizardCompletionBoardSeed
import net.chronoflow.feature.wizard.debug.WizardCompletionPrompts
import net.chronoflow.feature.wizard.debug.WizardMemoryDraftStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class WizardCompletionBoardSeedTest(
    private val board: String,
) {
    @Test fun draftMatchesTheCompletionBoard() =
        runTest(timeout = 60.seconds) {
            val draft = WizardBoardCaptureSeed.draft(board, Locale.ENGLISH)
            val override = checkNotNull(WizardBoardCaptureSeed.uiOverride(board))
            val checking = board.startsWith("WizardCheck")
            assertEquals(
                if (checking) MemberWizardStep.checking else MemberWizardStep.fineTune,
                draft.state.step,
            )
            assertEquals(MemberWizardStep.returnChoice, draft.state.lastInputStep)
            assertEquals(board == "WizardCheckingAgain", draft.state.checkingAnswers == true)
            assertEquals(
                if (board == "WizardFineTuneMulti" || board == "WizardSheetCheckout") 2 else 1,
                draft.form.legs.size,
            )
            assertEquals(
                if (board == "WizardSheetCheckout") 1 else draft.form.legs.size,
                draft.form.returnBoundary,
            )
            assertEquals(
                if (board == "WizardFineTuneNone") .95 else null,
                draft.state.progressFloor,
            )
            val model =
                MemberWizardModel(
                    draft,
                    WizardMemoryDraftStore(),
                    backgroundScope,
                    { WizardBoardCaptureSeed.now },
                )
            try {
                val progress =
                    when {
                        board == "WizardCheckingAgain" -> .70
                        checking -> .60
                        board == "WizardFineTuneNone" -> .95
                        else -> .80
                    }
                assertEquals(progress, model.progress, 0.0)
            } finally {
                model.close()
            }
            if (checking) {
                assertEquals(2, override.checking?.ticks)
                assertEquals(board == "WizardCheckingSlow", override.checking?.slow)
                assertEquals(
                    if (board == "WizardCheckThrottle") 42 else 0,
                    override.retrySeconds,
                )
                assertEquals(
                    when (board) {
                        "WizardCheckOffline" -> DataFailure.Offline
                        "WizardCheckServer" -> DataFailure.Server(500)
                        "WizardCheckThrottle" -> DataFailure.Throttled(42.0, 429)
                        else -> null
                    },
                    override.checking?.failure,
                )
                assertNull(draft.state.review)
            } else {
                assertEquals("08:30", draft.form.preparation.latestWake)
                assertEquals(draft.inputFingerprint, draft.state.checkedInput)
                assertEquals(WizardBoardCaptureSeed.now, draft.state.reviewedAt)
                assertEquals(draft.form.identity, draft.state.review!!.identity)
                assertEquals(board == "WizardFineTuneBusy", override.reviewing)
                assertEquals(
                    if (board == "WizardSheetBlueError") JsonPrimitive(12) else JsonPrimitive(1.5),
                    draft.state.savedProfile?.get("blueBlockerMdfd"),
                )
                assertEquals(
                    JsonPrimitive(if (board == "WizardSheetDrinksError") 0 else 2),
                    draft.state.savedProfile?.get("cupsFilterCoffee"),
                )
                val kind =
                    when (board) {
                        "WizardSheet", "WizardFineTuneSheet" -> PromptKind.SeatSide
                        "WizardSheetRoom" -> PromptKind.Room
                        "WizardSheetCheckout" -> PromptKind.LateCheckOut
                        "WizardSheetWake" -> PromptKind.LatestWake
                        "WizardSheetBusy" -> PromptKind.PreDepartureBusyHours
                        "WizardSheetDrinks", "WizardSheetDrinksError" -> PromptKind.Cups
                        "WizardSheetFreeSleep" -> PromptKind.FreeDayTimes
                        "WizardSheetLight" -> PromptKind.LightDevice
                        "WizardSheetBlue", "WizardSheetBlueError" -> PromptKind.BlueBlockers
                        else -> null
                    }
                assertEquals(
                    kind,
                    draft.state.review!!
                        .prompts
                        .firstOrNull { it.id == draft.state.promptSheetID }
                        ?.kind,
                )
                if (board == "WizardFineTune" || board == "WizardFineTuneBusy") {
                    assertEquals(
                        listOf(
                            PromptKind.Room,
                            PromptKind.LatestWake,
                            PromptKind.SeatSide,
                            PromptKind.LateCheckOut,
                            PromptKind.PreDepartureBusyHours,
                            PromptKind.Cups,
                        ),
                        draft.state.review!!
                            .prompts
                            .map { it.kind },
                    )
                    assertEquals(
                        listOf(true, true, false, false, false, false),
                        draft.state.review!!
                            .prompts
                            .map { it.required },
                    )
                }
                if (board == "WizardFineTuneMulti") {
                    assertEquals(
                        listOf(
                            PromptKind.Room,
                            PromptKind.Room,
                            PromptKind.SeatSide,
                            PromptKind.SeatSide,
                            PromptKind.LateCheckOut,
                            PromptKind.LatestWake,
                        ),
                        draft.state.review!!
                            .prompts
                            .map { it.kind },
                    )
                    assertEquals(
                        listOf(true, true, false, false, false, false),
                        draft.state.review!!
                            .prompts
                            .map { it.required },
                    )
                    assertEquals("2026-10-12T23:25", draft.form.legs[0].arriveLocal)
                    assertEquals("2026-10-13T02:05", draft.form.legs[1].departLocal)
                    assertEquals("2026-10-13T16:35", draft.form.legs[1].arriveLocal)
                }
                if (board == "WizardFineTuneNone") {
                    assertTrue(
                        draft.state.review!!
                            .prompts
                            .isEmpty(),
                    )
                }
                assertEquals(board == "WizardFineTuneSleep", draft.form.override.enabled)
                if (board == "WizardFineTuneSleep") {
                    assertEquals("00:30", draft.form.override.habitualBed)
                    assertEquals("08:30", draft.form.override.habitualWake)
                    assertEquals(
                        JsonPrimitive("same_as_workdays"),
                        draft.state.savedProfile?.get("freeDayStatus"),
                    )
                    assertEquals(
                        setOf(PromptKind.Room, PromptKind.LatestWake, PromptKind.FreeDayTimes),
                        draft.state.review!!
                            .prompts
                            .map { it.kind }
                            .toSet(),
                    )
                }
                if (board == "WizardSheetRoom" || board == "WizardFineTuneSleep") {
                    assertEquals("declared", draft.form.stays[0].bedAvailableMode)
                    assertEquals("2026-10-13T15:00", draft.form.stays[0].bedAvailableFromLocal)
                }
                if (board == "WizardSheetCheckout") {
                    assertTrue(draft.state.returnRouteConfirmed == true)
                    assertEquals("2026-10-22T10:25", draft.form.legs[1].departLocal)
                    assertEquals("2026-10-22T15:30", draft.form.legs[1].arriveLocal)
                }
                assertEquals(board == "WizardSheetBusy", draft.form.preparation.busyAnswered)
                if (board == "WizardSheetBusy") {
                    assertEquals(
                        listOf("2026-10-09T09:00", "2026-10-12T08:00"),
                        draft.form.preparation.busyIntervals
                            .map { it.startLocal },
                    )
                    assertEquals(
                        listOf("2026-10-09T17:30", "2026-10-12T11:00"),
                        draft.form.preparation.busyIntervals
                            .map { it.endLocal },
                    )
                }
                assertEquals(
                    if (board in
                        setOf(
                            "WizardSheet",
                            "WizardFineTuneSheet",
                            "WizardFineTune",
                            "WizardFineTuneBusy",
                            "WizardFineTuneMulti",
                        )
                    ) {
                        "left"
                    } else {
                        null
                    },
                    draft.form.legs[0].seatWindow,
                )
                assertEquals(
                    when (board) {
                        "WizardSheetBlueError" -> "Enter a rating between 0.1 and 9.9."
                        "WizardSheetDrinksError" -> "Add at least one drink to save this answer."
                        else -> null
                    },
                    override.answerError,
                )
                if (board == "WizardSheet") {
                    val input = draft.form.reviewInput(null).input
                    val profile = JsonObject(draft.state.savedProfile.orEmpty() + draft.state.profile)
                    assertEquals(
                        MockWizardPrompts.make(input, profile),
                        WizardCompletionPrompts.make(input, profile),
                    )
                }
            }
            assertEquals(
                draft.inputFingerprint,
                WizardBoardCaptureSeed.draft(board + "Dark", Locale.ENGLISH).inputFingerprint,
            )
            assertEquals(
                draft.inputFingerprint,
                WizardBoardCaptureSeed.draft(board + "NL", Locale.ENGLISH).inputFingerprint,
            )
            assertEquals(
                draft.inputFingerprint,
                WizardBoardCaptureSeed.draft(board + "DarkNL", Locale.ENGLISH).inputFingerprint,
            )
            assertNotNull(WizardBoardCaptureSeed.uiOverride(board + "DarkNL"))
            assertFalse(draft.state.sleepNeeded)
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            (WizardCompletionBoardSeed.baseNames + "WizardSheetFreeSleep")
                .map { arrayOf<Any>(it) }
    }
}
