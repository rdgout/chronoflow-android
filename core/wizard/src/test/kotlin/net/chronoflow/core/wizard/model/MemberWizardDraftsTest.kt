package net.chronoflow.core.wizard.model

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.AirportRef
import net.chronoflow.core.model.contracts.ApiTripInputSnapshot
import net.chronoflow.core.testing.Fixtures
import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.wizard.generation.TripGenerationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemberWizardDraftsTest {
    private val body =
        JsonObject(
            mapOf(
                "habitualBed" to JsonPrimitive("22:30"),
                "habitualWake" to JsonPrimitive("06:30"),
                "chronotype" to JsonPrimitive("early"),
            ),
        )

    @Test fun memberInitialPrefillsOriginAndSleep() {
        val draft =
            MemberWizardDrafts.memberInitial(
                true,
                AirportRef("AMS", "Amsterdam", "Europe/Amsterdam"),
                body,
                false,
            )
        assertFalse(draft.state.firstTrip)
        assertTrue(draft.state.fromLastTrip)
        assertTrue(draft.state.sleepNeeded)
        assertEquals("AMS", draft.form.legs[0].fromIata)
        assertEquals("Amsterdam", draft.form.legs[0].fromCity)
        assertEquals("Europe/Amsterdam", draft.airports["AMS"]?.timezone)
        assertEquals(body, draft.state.savedProfile)
        assertEquals(
            mapOf(
                "habitual_bed" to JsonPrimitive("22:30"),
                "habitual_wake" to JsonPrimitive("06:30"),
                "chronotype" to JsonPrimitive("early"),
            ),
            draft.state.profile,
        )
        val undecodableCard = MemberWizardDrafts.memberInitial(true, null, null, null)
        assertFalse(undecodableCard.state.firstTrip)
        assertFalse(undecodableCard.state.fromLastTrip)
    }

    @Test fun sleepAnsweredSkipsPrefill() {
        val draft = MemberWizardDrafts.memberInitial(false, null, body, true)
        assertFalse(draft.state.sleepNeeded)
        assertTrue(draft.state.firstTrip)
        assertTrue(draft.state.profile.isEmpty())
    }

    @Test fun guestDefaults() {
        val draft = MemberWizardDrafts.guest()
        assertEquals(WizardMode.guest, draft.state.mode)
        assertEquals(MemberWizardStep.sleep, draft.state.lastInputStep)
        assertFalse(draft.state.firstTrip)
        assertEquals(
            mapOf("habitual_bed" to JsonPrimitive("23:00"), "habitual_wake" to JsonPrimitive("07:00")),
            draft.state.profile,
        )
    }

    @Test fun seededStartsCheckingWithoutIdentity() {
        val seed =
            MemberWizardDrafts.guest().copy(
                form =
                    WizardForm
                        .from(
                            generationInput(),
                        ).copy(identity = generationReview().identity, override = WizardSleepOverride(enabled = true)),
            )
        val initial = MemberWizardDrafts.memberInitial(false, null, null, null)
        val draft = MemberWizardDrafts.seeded(seed, initial)
        assertNull(draft.form.identity)
        assertEquals(WizardSleepOverride(), draft.form.override)
        assertEquals(seed.airports, draft.airports)
        assertEquals(initial.state.instance, draft.state.instance)
        assertEquals(MemberWizardStep.checking, draft.state.step)
        assertEquals(true, draft.state.returnToReady)
        assertTrue(draft.state.entered)
        assertFalse(draft.state.fromLastTrip)
        assertTrue(draft.state.profile.isEmpty())
    }

    @Test fun editLoadStartsCheckingWithFullProgress() {
        val snapshot = ApiJson.response.decodeFromString<ApiTripInputSnapshot>(Fixtures.text("wizard/snapshot.json"))
        val draft = MemberWizardDraft().loadTrip(snapshot, "trip", TripWizardAction.edit)
        assertEquals(MemberWizardStep.checking, draft.state.step)
        assertEquals(1.0, draft.state.progressFloor)
        assertEquals(true, draft.state.returnToReady)
        assertEquals(draft.inputFingerprint, draft.state.originalInput)
        assertFalse(draft.hasChanges)
    }

    @Test fun duplicateLoadStartsOnTimes() {
        val snapshot = ApiJson.response.decodeFromString<ApiTripInputSnapshot>(Fixtures.text("wizard/snapshot.json"))
        val draft = MemberWizardDraft().loadTrip(snapshot, "trip", TripWizardAction.duplicate)
        assertEquals(MemberWizardStep.outbound, draft.state.step)
        assertTrue(draft.state.entered)
        assertEquals("Amsterdam → HND", draft.state.copiedRoute)
    }

    @Test fun generationStateUsesReviewIdentity() {
        val review = generationReview()
        val input = WizardForm.from(generationInput()).reviewInput(null)
        val state = TripGenerationState("instance", input, review = review)
        assertEquals(review.identity, state.input.identity)
        assertEquals(review.identity, state.identity)
        assertEquals(input, TripGenerationState("other", input).input)
    }

    @Test fun fingerprintsSortNestedObjectsAndIgnoreIdentity() {
        val form = WizardForm.from(generationInput())
        val state =
            MemberWizardState(
                profile =
                    JsonObject(
                        linkedMapOf(
                            "age_band" to JsonPrimitive("18_64"),
                            "shift_work" to JsonPrimitive(false),
                        ),
                    ),
            )
        val first = MemberWizardDraft(form, state)
        val second =
            first.copy(
                form = form.copy(identity = generationReview().identity),
                state = state.copy(profile = JsonObject(state.profile.toSortedMap(reverseOrder()))),
            )
        assertEquals(first.inputFingerprint, second.inputFingerprint)
        assertFalse(first.hasChanges)
        assertTrue(first.copy(state = state.copy(entered = true)).hasChanges)
    }
}
