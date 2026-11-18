package net.chronoflow.core.datamock

import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.ApiTripInput
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.testing.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.BeforeClass
import org.junit.Test

/** Android generation-helper coverage (new). */
class MockTripGenerationHelpersTest {
    companion object {
        @JvmStatic @BeforeClass
        fun warmFixtures() {
            warmMockFixtures()
        }
    }

    @Test fun airportsIncludeEveryFixtureLeg() {
        assertEquals(
            listOf("AMS", "DXB", "HND", "JFK", "LAX", "LHR", "SIN", "SYD"),
            MockTripGeneration.airports().map {
                it.iata
            },
        )
    }

    @Test fun instantRejectsTheDstGap() {
        val airport = MockTripGeneration.airports().first { it.iata == "AMS" }
        val failure =
            assertThrows(DataFailure.Validation::class.java) { MockTripGeneration.instant("2026-03-29T02:30", airport) }
        assertEquals(
            DataFailure.Validation(mapOf("review" to FieldMessage("Check the departure and arrival times.")), 422),
            failure,
        )
    }

    @Test fun savedHashIsDeterministic() {
        val input = ApiJson.response.decodeFromString(ApiTripInput.serializer(), Fixtures.text("wizard/input.json"))
        val review = ApiJson.response.decodeFromString(ApiTripReview.serializer(), Fixtures.text("wizard/review.json"))
        val draft =
            MockWizardDraft(
                "owner",
                null,
                input,
                review,
                codecState()
                    .accounts.values
                    .single()
                    .snapshot.settings.bodyClock,
                0.0,
            )
        val first = MockTripGeneration.saved(draft, "trip")
        val second = MockTripGeneration.saved(draft, "trip")
        assertEquals(first.second.plan.meta.inputHash, second.second.plan.meta.inputHash)
        assertEquals(first, second)
    }
}
