package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.contracts.AdaptationDirection
import net.chronoflow.core.model.contracts.AdaptationGoal
import net.chronoflow.core.model.contracts.AdaptationView
import net.chronoflow.core.model.contracts.AssumedReturnBasis
import net.chronoflow.core.model.contracts.AssumedReturnView
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.EndState
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.contracts.TripDetailLegView
import net.chronoflow.core.testing.MockTripTemplates
import org.junit.Test

/** (new) Android tests of the prepared model. */
class PreparedPlanTests {
    private val en = PageCopy(Language.En, testPageCopy)
    private val nl = PageCopy(Language.Nl, testPageCopy)
    private val s3: TripDetail get() = MockTripTemplates.templates()[2].detail
    private val sleepLine = CopyLine("Sleep", emptyList())

    private fun withSummary(
        detail: TripDetail,
        change: (net.chronoflow.core.model.contracts.SummaryView) -> net.chronoflow.core.model.contracts.SummaryView,
        returnLeg: Boolean = false,
    ): TripDetail {
        val plan = detail.plan
        val legs =
            if (returnLeg) {
                listOf(TripDetailLegView(0, null, detail.trip.to, detail.trip.from, returnLeg = true))
            } else {
                emptyList()
            }
        return detail.copy(
            trip = detail.trip.copy(legs = legs),
            plan = plan.copy(summary = change(plan.summary!!), days = plan.days.map { it.copy(notes = emptyList()) }),
        )
    }

    @Test fun priorityKeyTable() {
        assertThat(priorityKey(Priority.Core)).isEqualTo("Important")
        assertThat(priorityKey(Priority.Recommended)).isEqualTo("Recommended")
        assertThat(priorityKey(Priority.Optional)).isEqualTo("Optional")
        assertThat(priorityKey(Priority.Info)).isEqualTo("Information")
        assertThat(priorityKey(Priority.Unknown("x"))).isEqualTo("Information")
    }

    @Test fun clockShiftTextCases() {
        val options = CopyFormat(Language.En)

        fun shift(
            direction: AdaptationDirection,
            minutes: Int,
            copy: PageCopy = en,
        ) = clockShiftText(AdaptationView(AdaptationGoal.Full, direction, minutes), testFormatter, options, copy)
        assertThat(shift(AdaptationDirection.None, 0)).isEqualTo("No clock shift planned")
        assertThat(shift(AdaptationDirection.Earlier, 120)).isEqualTo("We advance your clock 2 hours")
        assertThat(shift(AdaptationDirection.Later, 90)).isEqualTo("We delay your clock 1 hr 30 min")
        assertThat(shift(AdaptationDirection.Earlier, 120, nl)).isEqualTo("We zetten je klok 2 uur vooruit")
        assertThat(shift(AdaptationDirection.Unknown("x"), 60)).isEqualTo("We delay your clock 1 hours")
    }

    @Test fun endTextRules() {
        val returnTrip =
            withSummary(s3, { it.copy(endState = EndState.Hold, endCopy = listOf(sleepLine)) }, returnLeg = true)
        assertThat(prepare(returnTrip).endText).isEqualTo("There are no more steps in this plan.")
        val noExpected =
            withSummary(s3, { it.copy(endState = EndState.Adapted, expectedAdaptedText = null, endCopy = emptyList()) })
        assertThat(prepare(noExpected).endText).isEqualTo("Expected adapted")
        val expected =
            withSummary(
                s3,
                { it.copy(endState = EndState.Adapted, expectedAdaptedText = sleepLine, endCopy = emptyList()) },
            )
        assertThat(prepare(expected).endText).isEqualTo("Sleep")
        val duplicates =
            withSummary(
                s3,
                {
                    it.copy(
                        endState = EndState.Hold,
                        expectedAdaptedText = null,
                        endCopy = listOf(sleepLine, sleepLine, CopyLine("How this plan ends", emptyList())),
                    )
                },
            )
        assertThat(prepare(duplicates).endText).isEqualTo("Holding this shift · Sleep")
        val empty =
            withSummary(
                s3,
                { it.copy(endState = EndState.Unknown("x"), expectedAdaptedText = null, endCopy = emptyList()) },
            )
        assertThat(prepare(empty).endText).isEqualTo("There are no more steps in this plan.")
    }

    @Test fun assumedEndForCheckOutAndNights() {
        val plan = s3.plan
        val checkOut =
            s3.copy(
                plan =
                    plan.copy(
                        meta =
                            plan.meta.copy(
                                assumedReturn = AssumedReturnView("2026-10-30T10:00Z", AssumedReturnBasis.CheckOut, 14),
                            ),
                    ),
            )
        assertThat(prepare(checkOut).assumedEnd)
            .isEqualTo("Assumed trip end · 30 Oct 2026 · 21:00 · Sydney · Based on your check-out date")
        val nights =
            s3.copy(
                plan =
                    plan.copy(
                        meta =
                            plan.meta.copy(
                                assumedReturn = AssumedReturnView("2026-10-30T10:00Z", AssumedReturnBasis.Nights, 10),
                            ),
                    ),
            )
        assertThat(prepare(nights).assumedEnd)
            .isEqualTo("Assumed trip end · 30 Oct 2026 · 21:00 · Sydney · Based on 10 nights")
    }

    @Test fun checkStatusesFollowMarksAndPendingChanges() {
        val detail = tripDetail()
        val block = detail.plan.blocks.first { it.actionable && it.checkKey != null && !isOptionalStep(it) }
        val key = checkNotNull(block.checkKey)
        val stored =
            detail.copy(
                checkState =
                    detail.checkState.copy(
                        marks = listOf(CheckMark(key, CheckStatus.Done, detail.trip.departureUtc)),
                    ),
            )

        fun status(plan: net.chronoflow.core.plan.PreparedPlan) =
            plan.days.flatMap { it.blocks }.first { it.block.checkKey == key }
        assertThat(status(prepare(stored)).status).isEqualTo(CheckStatus.Done)
        val clear =
            CheckOffChange(detail.trip.id, detail.plan.meta.inputHash, key.type, key.localDate, key.ordinal, null)
        assertThat(status(prepare(stored, pendingChecks = listOf(clear))).status).isNull()
        val unknown =
            stored.copy(
                checkState =
                    stored.checkState.copy(
                        marks = listOf(CheckMark(key, CheckStatus.Unknown("x"), detail.trip.departureUtc)),
                    ),
            )
        val label = status(prepare(unknown)).accessibilityLabel
        assertThat(label.contains(", Skipped")).isEqualTo(true)
    }

    @Test fun zoneChangeTextsOnTheFirstS3ZoneChange() {
        val prepared = prepare(s3)
        val change = prepared.days.flatMap { it.blocks }.first { it.zoneInstruction != null }
        assertThat(
            change.zoneInstruction,
        ).isEqualTo("From here on all times are Singapore time. Set your watch to 15:30.")
        assertThat(change.zoneComparison).isEqualTo("Amsterdam 09:30 → Singapore 15:30 · +6 h")
    }

    @Test fun summaryKeepsTheTrailingSpaceWithoutSleep() {
        val detail = s3
        val day = sampleDay().copy(noLightText = null)
        val plan = detail.plan.copy(days = listOf(day), blocks = emptyList())
        val prepared = prepare(detail.copy(plan = plan))
        assertThat(prepared.days.single().summary).isEqualTo("Sleep  · Light ")
    }
}
