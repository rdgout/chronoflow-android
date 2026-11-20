package net.chronoflow.core.wizard.model

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.contracts.ApiReviewItinerary
import net.chronoflow.core.model.contracts.ApiReviewLayover
import net.chronoflow.core.model.contracts.ApiReviewPromptScope
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.contracts.ReviewPromptTarget
import net.chronoflow.core.model.contracts.SegmentKindCode
import net.chronoflow.core.model.contracts.TripDirection
import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.testing.wizard.wizardAirport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

internal val wizardTranslate: Translate = { key, values ->
    values.entries.fold(
        key,
    ) { text, (name, value) -> text.replace("{{$name}}", value) }
}

class WizardPresentationTests {
    @Test fun planDatesCountAndSourceChangesUseThePlanStartZone() {
        val form = WizardForm.from(generationInput())
        val review = generationReview()
        assertEquals(15, WizardPresentation.planDayCount(review.summary))
        val dates = WizardPresentation.planDateSummary(form, review.summary, Language.En, wizardTranslate)
        assertTrue(dates.contains("Sat 7 Nov") && dates.contains("Sat 21 Nov") && dates.endsWith("one way"))
        assertNull(WizardPresentation.planDayCount(null))
        assertEquals("", WizardPresentation.planDateSummary(form, null, Language.En, wizardTranslate))
        val change =
            WizardPresentation.SourceChange(
                form,
                listOf(
                    WizardPresentation.SourceChange.Field.route,
                    WizardPresentation.SourceChange.Field.flight,
                    WizardPresentation.SourceChange.Field.returnFlight,
                    WizardPresentation.SourceChange.Field.sleep,
                    WizardPresentation.SourceChange.Field.extras,
                ),
                review.price,
                review.price,
            )
        val text = WizardPresentation.sourceChangeSummary(change, null, null, Language.En, wizardTranslate)!!
        assertTrue(text.contains("route changed to AMS → HND"))
        assertTrue(text.contains("trip end changed to One way"))
        assertTrue(text.contains("usual sleep times") && text.contains("3 days prep"))
        assertTrue(text.endsWith("The price is still 1 credits."))
        assertNull(
            WizardPresentation.sourceChangeSummary(change.copy(price = null), null, null, Language.En, wizardTranslate),
        )
        val acrossYear = review.summary!!.copy(planStartUtc = "2026-12-31T12:00Z", planEndDate = "2027-01-02")
        assertEquals(
            "Thu, 31 Dec 2026 – Sat, 2 Jan 2027 · one way",
            WizardPresentation.planDateSummary(form, acrossYear, Language.En, wizardTranslate),
        )
        val localNewYear = acrossYear.copy(planStartUtc = "2026-12-31T23:30Z")
        assertEquals(
            "Fri 1 Jan – Sat 2 Jan · one way",
            WizardPresentation.planDateSummary(form, localNewYear, Language.En, wizardTranslate),
        )
        assertEquals(2, WizardPresentation.planDayCount(localNewYear))
    }

    @Test fun localFactsBeforeCheckingUseZonesAndCompleteNights() {
        var form =
            WizardForm.from(generationInput()).updateLeg(0) {
                it.copy(departLocal = "2026-10-12T14:40", arriveLocal = "2026-10-13T09:15")
            }
        val airports =
            mapOf(
                "AMS" to wizardAirport("AMS", "Amsterdam", "Europe/Amsterdam"),
                "HND" to wizardAirport("HND", "Tokyo", "Asia/Tokyo"),
                "DXB" to wizardAirport("DXB", "Dubai", "Asia/Dubai"),
            )
        assertNull(form.itinerary)
        assertEquals(695, WizardOutboundRules.flightMinutes(form, 0, airports))
        form = form.updateLeg(0) { it.copy(arriveLocal = "2026-10-11T09:15") }
        assertNull(WizardOutboundRules.flightMinutes(form, 0, airports))
        form =
            form
                .insertStop(airports.getValue("DXB"), 0)
                .updateLeg(0) { it.copy(arriveLocal = "2026-10-12T23:25") }
                .updateLeg(1) { it.copy(departLocal = "2026-10-13T02:05") }
        // 06 §11.9: the Overnight illustration is not a complete 23:00–07:00 night.
        assertEquals(
            JsonPrimitive("160"),
            WizardOutboundRules
                .connection(form, 1, airports) {
                    it.toString()
                }?.arguments
                ?.get("duration"),
        )
        form =
            form
                .updateLeg(0) {
                    it.copy(arriveLocal = "2026-10-12T22:25")
                }.updateLeg(1) { it.copy(departLocal = "2026-10-14T02:05") }
        assertEquals(
            JsonPrimitive(1),
            WizardOutboundRules
                .connection(form, 1, airports) {
                    it.toString()
                }?.arguments
                ?.get("count"),
        )
        form =
            form
                .updateLeg(0) {
                    it.copy(toIata = "AMS", arriveLocal = "2026-10-24T23:00")
                }.updateLeg(1) { it.copy(fromIata = "AMS", departLocal = "2026-10-25T07:00") }
        assertEquals(
            JsonPrimitive(1),
            WizardOutboundRules
                .connection(form, 1, airports) {
                    it.toString()
                }?.arguments
                ?.get("count"),
        )
        form = form.updateLeg(0) { it.copy(arriveLocal = "2026-10-24T23:01") }
        assertEquals(
            "Connection in {{airport}} · {{duration}}",
            WizardOutboundRules
                .connection(form, 1, airports) {
                    it.toString()
                }?.key,
        )
    }

    @Test fun errorsPastConnectionsAndOvernightUseAirportClock() {
        var form = WizardForm.from(generationInput()).updateLeg(0) { it.copy(arriveLocal = "2026-11-09T09:00") }
        val airports =
            mapOf(
                "AMS" to wizardAirport("AMS", "Amsterdam", "Europe/Amsterdam"),
                "HND" to wizardAirport("HND", "Tokyo", "Asia/Tokyo"),
            )
        val after = Instant.parse("2026-11-12T12:00:00Z")
        assertEquals(
            "Check the departure and arrival times.",
            WizardOutboundRules.errors(form, airports, after, false)["legs.0.arriveLocal"]?.key,
        )
        form = form.updateLeg(0) { it.copy(arriveLocal = "2026-11-11T09:00") }
        assertTrue(WizardOutboundRules.errors(form, airports, after, false).isEmpty())
        assertEquals(
            "A new plan needs an upcoming arrival. Check your flight dates or add your return flight.",
            WizardOutboundRules.errors(form, airports, after, true)["review"]?.key,
        )
        val home =
            form.addReturn().updateLeg(1) {
                it.copy(toIata = "AMS", departLocal = "2026-11-11T10:25", arriveLocal = "2026-11-11T15:30")
            }
        assertEquals(
            "A new plan needs an upcoming arrival. Check your flight dates.",
            WizardFlightRules.errors(home, airports, after, true)["review"]?.key,
        )
        form = form.insertStop(wizardAirport("DXB", "Dubai", "Asia/Dubai"), 0)
        assertEquals("Stop in {{airport}}", WizardOutboundRules.connection(form, 1) { it.toString() }?.key)
        form =
            form.copy(
                itinerary =
                    ApiReviewItinerary(
                        emptyList(),
                        listOf(ApiReviewLayover(TripDirection.Outbound, 0, SegmentKindCode.Connection, 160, 0)),
                    ),
            )
        assertEquals(
            "Connection in {{airport}} · {{duration}}",
            WizardOutboundRules.connection(form, 1) { it.toString() }?.key,
        )
        form =
            form.copy(
                itinerary =
                    ApiReviewItinerary(
                        emptyList(),
                        listOf(ApiReviewLayover(TripDirection.Outbound, 0, SegmentKindCode.Stopover, 1600, 2)),
                    ),
            )
        assertEquals(
            JsonPrimitive(2),
            WizardOutboundRules.connection(form, 1) { it.toString() }?.arguments?.get("count"),
        )
    }

    @Test fun summariesAndPromptAnswersRetainScopeAndFallback() {
        var form = WizardForm.from(generationInput())
        assertTrue(
            WizardPresentation
                .flightSummary(
                    form,
                    language = Language.En,
                    t = wizardTranslate,
                ).contains("11:00 → Wed 06:00"),
        )
        assertEquals("One way", WizardPresentation.returnSummary(form, null, Language.En, wizardTranslate))
        form = form.copy(end = WizardEnd(kind = "nights", nights = 9))
        assertEquals("9 nights", WizardPresentation.returnSummary(form, null, Language.En, wizardTranslate))
        form = form.copy(end = WizardEnd(kind = "check_out", checkOutDate = "2026-11-21"))
        assertTrue(WizardPresentation.returnSummary(form, null, Language.En, wizardTranslate).startsWith("Check-out"))
        var prompt =
            wizardPrompt(
                kind = PromptKind.Room,
                blocking = false,
                target = ReviewPromptTarget.Trip,
                fields = listOf("stay.bed_available_mode"),
                scope = ApiReviewPromptScope(TripDirection.Outbound, null, 0),
                answer =
                    mapOf("stay.bed_available_mode" to JsonPrimitive("arrival")),
            )
        assertEquals(
            "When I land",
            WizardPresentation.promptAnswerText(prompt, form.reviewInput(null), Language.En, wizardTranslate),
        )
        assertTrue(WizardPresentation.promptKey(prompt).contains("room:question:0:"))
        prompt =
            wizardPrompt(
                kind = PromptKind.Cups,
                fields = listOf("caffeine_habit"),
                answer =
                    mapOf("caffeine_habit" to JsonPrimitive("non_user")),
            )
        assertEquals(
            "None",
            WizardPresentation.promptAnswerText(prompt, form.reviewInput(null), Language.En, wizardTranslate),
        )
        val input =
            form.reviewInput(
                null,
                JsonObject(
                    mapOf(
                        "caffeine_habit" to JsonPrimitive("cups"),
                        "cups_filter_coffee" to JsonPrimitive(2),
                        "cups_tea" to JsonPrimitive(1),
                    ),
                ),
            )
        prompt =
            wizardPrompt(kind = PromptKind.Cups, fields = listOf("caffeine_habit", "cups_filter_coffee", "cups_tea"))
        assertEquals("3 a day", WizardPresentation.promptAnswerText(prompt, input, Language.En, wizardTranslate))
        assertEquals(
            "error",
            WizardPresentation
                .promptFieldError(
                    prompt,
                    form,
                    mapOf("profile.cupsTea" to FieldMessage("error")),
                )?.key,
        )
        assertEquals("3 days prep", WizardPresentation.extrasSummary(form, null, input, Language.En, wizardTranslate))
    }

    @Test fun ratingsRetainThePinnedIntegerFormatter() {
        val prompt = wizardPrompt(kind = PromptKind.BlueBlockers, fields = listOf("blue_blockers", "blue_blocker_mdfd"))
        val form = WizardForm.from(generationInput())
        val input =
            form.reviewInput(
                null,
                JsonObject(
                    mapOf(
                        "blue_blockers" to JsonPrimitive("rated"),
                        "blue_blocker_mdfd" to JsonPrimitive("1,5"),
                    ),
                ),
            )
        assertEquals("Rated 2", WizardPresentation.promptAnswerText(prompt, input, Language.En, wizardTranslate))
        val tie = input.copy(profile = JsonObject(input.profile!! + ("blue_blocker_mdfd" to JsonPrimitive(2.5))))
        assertEquals("Rated 2", WizardPresentation.promptAnswerText(prompt, tie, Language.Nl, wizardTranslate))
        val upper = input.copy(profile = JsonObject(input.profile!! + ("blue_blocker_mdfd" to JsonPrimitive(9.9))))
        assertEquals("Rated 10", WizardPresentation.promptAnswerText(prompt, upper, Language.En, wizardTranslate))
    }
}
