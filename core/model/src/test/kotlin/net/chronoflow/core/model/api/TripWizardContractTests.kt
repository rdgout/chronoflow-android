package net.chronoflow.core.model.api

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.contracts.ApiReviewIdentity
import net.chronoflow.core.model.contracts.ApiReviewPromptShape
import net.chronoflow.core.model.contracts.ApiTripInputSnapshot
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.contracts.fixtureObject
import net.chronoflow.core.model.contracts.with
import net.chronoflow.core.testing.Fixtures
import org.junit.Test

class TripWizardContractTests {
    @Test
    fun promptsAndFractionalBounds() {
        val review =
            ApiJson.response.decodeFromString(
                ApiTripReview.serializer(),
                Fixtures.text("wizard/needs-answers.json"),
            )
        assertThat(review.status).isEqualTo(PlanStatus.NeedsAnswers)
        assertThat(review.summary).isNull()
        assertThat(review.canSave).isEqualTo(false)
        assertThat(review.prompts.map { it.kind }).containsExactly(PromptKind.AgeBand, PromptKind.Screening)
        assertThat(review.prompts[0].answer["age_band"]).isEqualTo(kotlinx.serialization.json.JsonNull)
        val shape =
            ApiJson.response.decodeFromString(
                ApiReviewPromptShape.serializer(),
                """{"kind":"options","options":["rated"],"min":0.1,"max":9.9}""",
            )
        assertThat(shape.min).isEqualTo(0.1)
        assertThat(shape.max).isEqualTo(9.9)
    }

    private fun encodeReview(value: ApiTripReviewInput): JsonObject =
        ApiJson.request.parseToJsonElement(
            ApiJson.request.encodeToString(ApiTripReviewInput.serializer(), value),
        ) as JsonObject

    private val reviewIdentity
        get() =
            ApiJson.response
                .decodeFromString(
                    ApiTripReview.serializer(),
                    Fixtures.text("wizard/review.json"),
                ).identity

    @Test
    fun reviewRequestMatchesFixture() {
        val expected = fixtureObject("wizard/review-request.json")
        val request = ApiTripReviewInput(wizardInput(), identity = null, profile = expected["profile"] as JsonObject)
        assertThat(encodeReview(request)).isEqualTo(expected)
    }

    @Test
    fun sourceRevisionOnlyWhenPresent() {
        val profile = fixtureObject("wizard/review-request.json")["profile"] as JsonObject
        val revision = "a".repeat(64)
        val withRevision = ApiReviewIdentity("t", 1, 1, revision, null)
        val encoded = encodeReview(ApiTripReviewInput(wizardInput(), withRevision, profile))
        assertThat(encoded["source_revision"]).isEqualTo(JsonPrimitive(revision))
        assertThat(encoded["draft_token"]).isEqualTo(JsonPrimitive("t"))
        val without = encodeReview(ApiTripReviewInput(wizardInput(), withRevision.copy(sourceRevision = null), null))
        assertThat(without.containsKey("source_revision")).isEqualTo(false)
        assertThat(without.containsKey("profile")).isEqualTo(false)
        listOf("identity", "input", "guest_secret").forEach { assertThat(without.containsKey(it)).isEqualTo(false) }
    }

    @Test
    fun saveRequestMatchesFixture() {
        val save = ApiTripSaveInput.from(reviewIdentity)
        val text = ApiJson.request.encodeToString(ApiTripSaveInput.serializer(), save)
        assertThat(ApiJson.request.parseToJsonElement(text)).isEqualTo(fixtureObject("wizard/save-request.json"))
        listOf(
            reviewIdentity.copy(token = ""),
            reviewIdentity.copy(inputRevision = 0),
            reviewIdentity.copy(reviewRevision = null),
        ).forEach { identity ->
            assertFailure { ApiTripSaveInput.from(identity) }.isInstanceOf<DataFailure.ReviewExpired>()
        }
    }

    @Test
    fun snapshotRoundTripsTheInput() {
        val snapshot =
            ApiJson.response.decodeFromString(
                ApiTripInputSnapshot.serializer(),
                Fixtures.text("wizard/snapshot.json"),
            )
        assertThat(snapshot.input).isEqualTo(wizardInput())
        assertThat(
            snapshot.input.outbound.legs[0]
                .stay.bedAvailableMode,
        ).isNull()
        assertThat(snapshot.airports["AMS"]?.timezone).isEqualTo("Europe/Amsterdam")
    }

    @Test
    fun reviewInputIsEncodeOnly() {
        val text =
            ApiJson.request.encodeToString(
                JsonElement.serializer(),
                fixtureObject("wizard/review-request.json").with("x", null),
            )
        assertFailure { ApiJson.response.decodeFromString(ApiTripReviewInput.serializer(), text) }
    }
}
