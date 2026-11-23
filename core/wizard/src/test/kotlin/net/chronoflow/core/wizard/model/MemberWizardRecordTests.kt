package net.chronoflow.core.wizard.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.ProfilePreferences
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.testing.wizard.wizardAirport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.Instant

@RunWith(Enclosed::class)
class MemberWizardRecordTests {
    class Codec {
        @Test fun fullStateRoundTrips() {
            val review = generationReview().copy(prompts = listOf(wizardPrompt()))
            val form = WizardForm.from(generationInput()).copy(identity = review.identity, itinerary = review.itinerary)
            val at = Instant.ofEpochMilli(1_800_000_000_123)
            val state =
                MemberWizardState(
                    review = review,
                    airports =
                        mapOf("AMS" to wizardAirport("AMS", "Amsterdam", "Europe/Amsterdam")),
                    sourceChange =
                        WizardPresentation.SourceChange(
                            form,
                            listOf(WizardPresentation.SourceChange.Field.route),
                            review.price,
                        ),
                    saveFailure = WizardSaveFailure(WizardSaveFailure.Kind.throttle, at.plusSeconds(42)),
                    reviewedAt = at,
                    profile = JsonObject(mapOf("age_band" to JsonNull)),
                    questions = review.prompts,
                )
            val draft =
                MemberWizardDraft(
                    form,
                    state,
                    mapOf("review" to FieldMessage("error", mapOf("count" to JsonPrimitive(2)))),
                )
            assertEquals(draft, MemberWizardRecord.decode(MemberWizardRecord.encode(draft)))
        }

        @Test fun retiredValuesAndSecretsNeverReachThePayload() {
            val identity = generationReview().identity.copy(guestSecret = "secret-must-disappear")
            val draft =
                MemberWizardDraft(
                    WizardForm.from(generationInput()).copy(identity = identity),
                    MemberWizardState(
                        profile = JsonObject(mapOf("pregnancy" to JsonPrimitive(true), "age_band" to JsonNull)),
                        savedProfile = JsonObject(mapOf("bipolar" to JsonPrimitive(true))),
                        review = generationReview().copy(identity = identity),
                        originalForm = WizardForm.from(generationInput()).copy(identity = identity),
                        sourceChange =
                            WizardPresentation.SourceChange(
                                WizardForm.from(generationInput()).copy(identity = identity),
                                emptyList(),
                                null,
                            ),
                    ),
                )
            val payload = MemberWizardRecord.encode(draft)
            assertFalse(payload.form.contains("secret-must-disappear"))
            assertFalse(payload.state.contains("secret-must-disappear"))
            assertFalse(payload.form.contains("guestSecret"))
            assertFalse(payload.state.contains("guestSecret"))
            assertFalse(ProfilePreferences.containsRetiredValues(payload.state))
            val decoded = MemberWizardRecord.decode(payload)!!
            assertEquals(JsonNull, decoded.state.profile["age_band"])
            val legacy = payload.copy(state = ApiJson.response.encodeToString(draft.state))
            assertFalse(
                MemberWizardRecord
                    .decode(legacy)!!
                    .state.profile
                    .containsKey("pregnancy"),
            )
        }

        @Test fun unknownPromptKindUsesThePinnedSupportFilter() {
            val prompt = wizardPrompt(kind = PromptKind.Unknown("future"))
            val draft =
                MemberWizardDraft(state = MemberWizardState(review = generationReview().copy(prompts = listOf(prompt))))
            assertEquals(
                listOf(prompt),
                MemberWizardRecord
                    .decode(MemberWizardRecord.encode(draft))!!
                    .state.review!!
                    .prompts,
            )
            val unsupported = wizardPrompt(kind = PromptKind.Unknown("future"), fields = listOf("retired_field"))
            assertTrue(
                MemberWizardRecord
                    .decode(
                        MemberWizardRecord.encode(
                            draft.copy(
                                state =
                                    draft.state.copy(
                                        review = generationReview().copy(prompts = listOf(unsupported)),
                                    ),
                            ),
                        ),
                    )!!
                    .state.review!!
                    .prompts
                    .isEmpty(),
            )
        }
    }

    @RunWith(Parameterized::class)
    class Expiry(
        private val age: Long,
        private val expired: Boolean,
    ) {
        @Test fun expiresAtExactlyThirtyMinutes() {
            assertEquals(expired, MemberWizardRecord.isExpired(1000, 1000 + age))
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "age={0}")
            fun cases() =
                listOf(
                    arrayOf<Any>(0L, false),
                    arrayOf<Any>(1_799_000L, false),
                    arrayOf<Any>(1_799_999L, false),
                    arrayOf<Any>(1_800_000L, true),
                    arrayOf<Any>(1_801_000L, true),
                )
        }
    }

    @RunWith(Parameterized::class)
    class Shape(
        private val kind: String,
    ) {
        @Test fun invalidShapesDecodeAsNull() {
            val base = MemberWizardDraft()
            val form =
                when (kind) {
                    "zero" -> base.form.copy(legs = emptyList(), stays = emptyList())
                    "seventeen" -> base.form.copy(legs = List(17) { WizardLeg() }, stays = List(17) { WizardStay() })
                    "stays" -> base.form.copy(stays = emptyList())
                    "boundary" -> base.form.copy(end = WizardEnd(kind = "return_legs", returnFromLeg = 0))
                    else -> base.form
                }
            var payload = MemberWizardRecord.encode(base.copy(form = form))
            if (kind == "malformed") payload = payload.copy(form = "{")
            assertNull(MemberWizardRecord.decode(payload))
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "shape={0}")
            fun cases() = listOf("zero", "seventeen", "stays", "boundary", "malformed")
        }
    }

    @RunWith(Parameterized::class)
    class Continuation(
        private val optional: Boolean,
    ) {
        @Test fun continuationBecomesFineTuneOrReady() {
            val review =
                generationReview().copy(
                    prompts = if (optional) listOf(wizardPrompt(blocking = false)) else emptyList(),
                )
            val draft =
                MemberWizardDraft(state = MemberWizardState(step = MemberWizardStep.continuation, review = review))
            assertEquals(
                if (optional) MemberWizardStep.fineTune else MemberWizardStep.ready,
                MemberWizardRecord.decode(MemberWizardRecord.encode(draft))!!.state.step,
            )
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "optional={0}")
            fun cases() = listOf(false, true)
        }
    }
}
