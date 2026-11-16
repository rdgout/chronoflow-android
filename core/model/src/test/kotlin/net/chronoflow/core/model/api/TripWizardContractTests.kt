package net.chronoflow.core.model.api

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import net.chronoflow.core.model.contracts.ApiReviewPromptShape
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.model.contracts.PromptKind
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
}
