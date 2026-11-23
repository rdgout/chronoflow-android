package net.chronoflow.core.wizard.generation

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.contracts.ApiReviewPromptScope
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.contracts.ReviewPromptTarget
import net.chronoflow.core.model.contracts.TripDirection
import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.wizard.model.wizardPrompt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TripGenerationAnswersTest {
    private val input = ApiTripReviewInput(generationInput())

    @Test fun profileAnswersUseTheRequestFilterAndReviewIdentity() {
        val review = generationReview().copy(prompts = listOf(wizardPrompt()))
        val answered =
            TripGenerationAnswers.applyingAnswers(
                input.copy(
                    profile =
                        JsonObject(
                            mapOf("pregnancy" to JsonPrimitive(true)),
                        ),
                ),
                review,
                mapOf("age_band" to mapOf("age_band" to JsonPrimitive("18_64"))),
            )
        assertEquals(mapOf("age_band" to JsonPrimitive("18_64")), answered.profile)
        assertEquals(review.identity, answered.identity)
    }

    @Test fun scopedTripAnswerUpdatesTheStay() {
        val prompt =
            wizardPrompt(
                kind = PromptKind.Room,
                target = ReviewPromptTarget.Trip,
                fields = listOf("stay.bed_available_mode"),
                scope = ApiReviewPromptScope(TripDirection.Outbound, 0, null),
            )
        val review = generationReview().copy(prompts = listOf(prompt))
        val answered =
            TripGenerationAnswers.applyingAnswers(
                input,
                review,
                mapOf(prompt.id to mapOf("stay.bed_available_mode" to JsonPrimitive("arrival"))),
            )
        assertEquals(
            "arrival",
            answered.input.outbound.legs[0]
                .stay.bedAvailableMode,
        )
    }

    @Test fun foreignFieldIsAReviewValidation() {
        val review = generationReview().copy(prompts = listOf(wizardPrompt()))
        val failure =
            assertThrows(DataFailure.Validation::class.java) {
                TripGenerationAnswers.applyingAnswers(
                    input,
                    review,
                    mapOf("age_band" to mapOf("foreign" to JsonPrimitive(true))),
                )
            }
        assertEquals(setOf("review"), failure.fields.keys)
    }

    @Test fun missingLegIndexExpiresTheReview() {
        val prompt =
            wizardPrompt(
                kind = PromptKind.Room,
                target = ReviewPromptTarget.Trip,
                fields = listOf("stay.bed_available_mode"),
                scope = ApiReviewPromptScope(TripDirection.Outbound, 8, null),
            )
        assertThrows(DataFailure.ReviewExpired::class.java) {
            TripGenerationAnswers.applyingAnswers(
                input,
                generationReview().copy(prompts = listOf(prompt)),
                mapOf(prompt.id to mapOf("stay.bed_available_mode" to JsonPrimitive("arrival"))),
            )
        }
    }

    @Test fun unknownPromptOrTargetIsAValidation() {
        assertThrows(DataFailure.Validation::class.java) {
            TripGenerationAnswers.applyingAnswers(
                input,
                generationReview(),
                mapOf("unknown" to emptyMap()),
            )
        }
        val prompt = wizardPrompt(target = ReviewPromptTarget.Unknown("future"))
        assertThrows(DataFailure.Validation::class.java) {
            TripGenerationAnswers.applyingAnswers(
                input,
                generationReview().copy(prompts = listOf(prompt)),
                mapOf(prompt.id to emptyMap()),
            )
        }
    }
}
