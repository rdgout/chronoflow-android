package net.chronoflow.core.model.contracts

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.api.ApiJson
import org.junit.Test

class ReviewPriceContractTests {
    private fun decode(
        name: String,
        patch: (JsonObject) -> JsonObject = { it },
    ): ApiTripReview {
        val raw = patch(fixtureObject("wizard/$name.json"))
        return ApiJson.response.decodeFromString(
            ApiTripReview.serializer(),
            ApiJson.response.encodeToString(JsonElement.serializer(), raw),
        )
    }

    @Test
    fun needsAnswersHasNoPriceAndNoSummary() {
        val review = decode("needs-answers")
        assertThat(review.status).isEqualTo(PlanStatus.NeedsAnswers)
        assertThat(review.price).isNull()
        assertThat(review.summary).isNull()
        assertThat(review.canSave).isFalse()
        assertThat(review.prompts.all { it.blocking }).isTrue()
    }

    @Test
    fun priceKeyMayBeAbsent() {
        assertThat(decode("needs-answers") { it.with("price", null) }.price).isNull()
    }

    @Test
    fun noPlanHasReasonsAndNoPrompts() {
        val review =
            decode("needs-answers") {
                it
                    .with("status", JsonPrimitive("no_plan"))
                    .with("prompts", JsonArray(emptyList()))
                    .with(
                        "reasons",
                        JsonArray(
                            listOf(
                                JsonObject(
                                    mapOf(
                                        "key" to JsonPrimitive("This plan is not available for travellers under 18."),
                                        "args" to JsonArray(emptyList()),
                                    ),
                                ),
                            ),
                        ),
                    )
            }
        assertThat(review.status).isEqualTo(PlanStatus.NoPlan)
        assertThat(review.price).isNull()
        assertThat(review.summary).isNull()
        assertThat(review.canSave).isFalse()
        assertThat(review.prompts).isEmpty()
        assertThat(review.reasons).hasSize(1)
    }

    @Test
    fun notEnoughCreditsKeepsThePrice() {
        val review =
            decode("review") {
                it.with("canSave", JsonPrimitive(false)).with("saveBlockedReason", JsonPrimitive("Not enough credits"))
            }
        assertThat(review.price?.cost).isEqualTo(1)
        assertThat(review.price?.balance).isEqualTo(1)
        assertThat(review.canSave).isFalse()
        assertThat(review.saveBlockedReason).isEqualTo("Not enough credits")
    }

    @Test
    fun okReviewKeepsThePrice() {
        val review = decode("review")
        assertThat(review.status).isEqualTo(PlanStatus.Ok)
        assertThat(review.canSave).isTrue()
        assertThat(review.price?.state).isEqualTo(ReviewCreditState.Create)
    }
}
