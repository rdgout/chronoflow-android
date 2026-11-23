package net.chronoflow.feature.wizard

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.contracts.ReviewCreditState
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.l10n.generated.CopyIndex
import net.chronoflow.core.ui.l10n.generated.CopyTable
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.feature.wizard.ready.PriceLine
import net.chronoflow.feature.wizard.ready.ReadyRow
import net.chronoflow.feature.wizard.ready.ReadyText
import net.chronoflow.feature.wizard.ready.ReadyUiMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GuestReadyUiMapperTest {
    private fun guest(): TripWizardUiState.Active {
        val active = readyActive()
        val draft = active.wizard.draft
        val guestDraft = draft.copy(state = draft.state.copy(mode = WizardMode.guest))
        return active.copy(wizard = active.wizard.copy(draft = guestDraft))
    }

    @Test fun rowsAreRouteFlightSleepQuestionsExtras() {
        val expected =
            listOf(
                ReadyRow.Route,
                ReadyRow.Flight,
                ReadyRow.Sleep,
                ReadyRow.Questions,
                ReadyRow.Extras,
            )
        assertEquals(expected, guest().ready().rows.map { it.kind })
    }

    @Test fun sleepSummaryNamesTheChronotype() {
        val chronotypes =
            listOf(
                "early" to "Early bird",
                "intermediate" to "In between",
                "late" to "Night owl",
                "unknown" to "Not sure",
            )
        for ((value, label) in chronotypes) {
            val base = guest().wizard.draft
            val profile =
                JsonObject(
                    mapOf(
                        "habitual_bed" to JsonPrimitive("23:30"),
                        "habitual_wake" to JsonPrimitive("07:15"),
                        "chronotype" to JsonPrimitive(value),
                    ),
                )
            val text = ReadyUiMapper.sleepSummary(base.copy(state = base.state.copy(profile = profile)))
            assertEquals("chronotype=$value", "23:30–07:15 · ${ReadyText.marker(0)}", text.pattern)
            assertEquals("chronotype=$value", label, text.copies.single().key)
        }
        val base = guest().wizard.draft
        val missing = base.copy(state = base.state.copy(profile = JsonObject(emptyMap()), savedProfile = null))
        assertEquals(
            "Not sure",
            ReadyUiMapper
                .sleepSummary(missing)
                .copies
                .single()
                .key,
        )
    }

    @Test fun questionsCountsOnlyAnsweredScreeningFields() {
        for (count in 0..2) {
            val active = guest()
            val draft = active.wizard.draft
            val profile =
                JsonObject(
                    mapOf(
                        "age_band" to if (count > 0) JsonPrimitive("18_64") else JsonNull,
                        "shift_work" to if (count > 1) JsonPrimitive(false) else JsonNull,
                        "chronotype" to JsonPrimitive("early"),
                    ),
                )
            val ui =
                active
                    .copy(
                        wizard = active.wizard.copy(draft = draft.copy(state = draft.state.copy(profile = profile))),
                    ).ready()
            val copy =
                ui.rows
                    .single { it.kind == ReadyRow.Questions }
                    .value.copies
                    .single()
            assertEquals("count=$count", if (count == 0) "None needed" else "{{count}} answered", copy.key)
            assertEquals("count=$count", count.toString(), copy.arguments["count"])
        }
    }

    @Test fun priceLinesForPaidAndFree() {
        val initial = guest()
        val draft = initial.wizard.draft
        val review = checkNotNull(draft.state.review)
        val basePrice = checkNotNull(review.price)
        val paid = review.copy(price = basePrice.copy(balance = 3))
        val active =
            initial.copy(
                wizard = initial.wizard.copy(draft = draft.copy(state = draft.state.copy(review = paid))),
            )
        assertEquals(listOf(PriceLine.GuestPaid(1, 3)), active.ready().price!!.lines)
        val free = review.copy(price = basePrice.copy(cost = 0, state = ReviewCreditState.Free))
        val ui =
            active
                .copy(
                    wizard = active.wizard.copy(draft = draft.copy(state = draft.state.copy(review = free))),
                ).ready()
        val price = checkNotNull(ui.price)
        assertEquals(listOf(PriceLine.GuestFree), price.lines)
        assertFalse(price.lines.any { it is PriceLine.Balance || it == PriceLine.FreeTrip })
    }

    @Test fun balanceOnePicksTheSingularString() {
        val copy = PriceLine.GuestPaid(1, 1).copy
        val entry = checkNotNull(CopyIndex.find(CopyTable.Android, copy.key))
        assertEquals(R.string.android_wizard_guest_price_paid_one, entry.resId)
        assertEquals(mapOf("cost" to "1", "balance" to "1"), copy.arguments)
    }

    @Test fun latestWakeRowStillFollowsTheMemberRule() {
        val active = guest()
        val draft = active.wizard.draft
        val prompt = completionPrompt(PromptKind.LatestWake)
        val review = checkNotNull(draft.state.review).copy(status = PlanStatus.Modified, prompts = listOf(prompt))
        val modified =
            active.copy(
                wizard = active.wizard.copy(draft = draft.copy(state = draft.state.copy(review = review))),
                optionalPrompts = listOf(prompt),
            )
        assertEquals(
            ReadyRow.LatestWake,
            modified
                .ready()
                .rows
                .last()
                .kind,
        )
        assertEquals(
            ReadyRow.Extras,
            modified
                .copy(optionalPrompts = emptyList())
                .ready()
                .rows
                .last()
                .kind,
        )
    }
}
