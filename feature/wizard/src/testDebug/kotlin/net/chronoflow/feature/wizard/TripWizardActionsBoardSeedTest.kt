@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.wizard.model.MemberWizardModel
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardCredits
import net.chronoflow.core.wizard.model.WizardPresentation
import net.chronoflow.core.wizard.model.hasChanges
import net.chronoflow.core.wizard.model.isEditing
import net.chronoflow.core.wizard.model.progress
import net.chronoflow.feature.wizard.debug.TripWizardActionsBoardSeed
import net.chronoflow.feature.wizard.debug.WizardBoardCaptureSeed
import net.chronoflow.feature.wizard.debug.WizardMemoryDraftStore
import net.chronoflow.feature.wizard.ready.ReadyFooterUi
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
class TripWizardActionsBoardSeedTest(
    private val board: String,
) {
    @Test fun draftMatchesEditOrDuplicateBoard() =
        runTest(timeout = 60.seconds) {
            val name = WizardBoardCaptureSeed.baseName(board)
            val dutch = board.endsWith("NL")
            val locale = if (dutch) Locale.forLanguageTag("nl") else Locale.ENGLISH
            val draft = WizardBoardCaptureSeed.draft(board, locale)
            val model =
                MemberWizardModel(draft, WizardMemoryDraftStore(), backgroundScope, { WizardBoardCaptureSeed.now })
            try {
                val step =
                    when (name) {
                        "WizardDuplicate" -> MemberWizardStep.outbound
                        "WizardReturnWeb" -> MemberWizardStep.returnChoice
                        "WizardCheckingEdit", "WizardCheckOfflineEdit", "WizardCheckServerEdit" -> MemberWizardStep.checking
                        "WizardExcludedEdit" -> MemberWizardStep.excluded
                        else -> MemberWizardStep.ready
                    }
                assertEquals(step, draft.state.step)
                assertEquals(name != "WizardDuplicate", model.isEditing)
                if (name == "WizardDuplicate") {
                    assertTrue(draft.form.legs.all { it.departLocal.isEmpty() && it.arriveLocal.isEmpty() })
                    assertNull(draft.state.review)
                    assertEquals(if (dutch) "Amsterdam → Tokio" else "Amsterdam → Tokyo", draft.state.copiedRoute)
                    assertEquals(.22, model.progress, 0.0)
                    return@runTest
                }
                assertEquals("capture-source", draft.state.edit?.sourceRevision)
                assertEquals(
                    name in
                        setOf(
                            "WizardEditChanged",
                            "WizardEditNoReturn",
                            "WizardEditReturn",
                            "WizardEditReturnNoCredits",
                            "WizardEditWeb",
                            "WizardDiscardEdit",
                            "WizardEditSaveOffline",
                            "WizardEditSaveServer",
                        ),
                    draft.hasChanges,
                )
                val returning =
                    name in
                        setOf(
                            "WizardEditChanged",
                            "WizardEditReturn",
                            "WizardEditReturnNoCredits",
                            "WizardDiscardEdit",
                            "WizardEditSaveOffline",
                            "WizardEditSaveServer",
                        )
                assertEquals(
                    when {
                        returning -> "return_legs"
                        name in setOf("WizardEditWeb", "WizardReturnWeb") -> "nights"
                        else -> "one_way"
                    },
                    draft.form.end.kind,
                )
                assertEquals(
                    if (name == "WizardEditChanged") {
                        listOf(WizardPresentation.SourceChange.Field.returnFlight)
                    } else {
                        null
                    },
                    draft.state.sourceChange?.fields,
                )
                if (name == "WizardReturnWeb") {
                    assertEquals(9, draft.form.end.nights)
                    assertNull(draft.state.progressFloor)
                    assertEquals(.45, model.progress, 0.0)
                    assertNull(WizardBoardCaptureSeed.uiOverride(board))
                    return@runTest
                }
                assertNotNull(WizardBoardCaptureSeed.uiOverride(board))
                val review = checkNotNull(draft.state.review)
                assertEquals(
                    when {
                        name == "WizardEditReturnNoCredits" || name == "WizardExcludedEdit" -> WizardCredits.unavailable
                        returning -> WizardCredits.credits
                        else -> WizardCredits.free
                    },
                    WizardCredits.of(review),
                )
                if (name == "WizardExcludedEdit") {
                    assertEquals(PlanStatus.NoPlan, review.status)
                    assertEquals(
                        "This plan is not available if, in the past 14 days, you have worked night or rotating shifts or had a very irregular sleep schedule.",
                        review.reasons.single().key,
                    )
                }
                if (name == "WizardEdit") {
                    val ui = boardState(board, locale)
                    assertFalse((ui.ready!!.footer as ReadyFooterUi.Primary).enabled)
                }
            } finally {
                model.close()
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            TripWizardActionsBoardSeed.names.map { arrayOf<Any>(it) }.also { check(it.size == 27) }
    }
}
