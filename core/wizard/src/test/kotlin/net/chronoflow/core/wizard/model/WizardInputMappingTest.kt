package net.chronoflow.core.wizard.model

import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.ApiTripInputSnapshot
import net.chronoflow.core.testing.Fixtures
import net.chronoflow.core.wizard.generationInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WizardInputMappingTest {
    @Test
    fun reviewInputMatchesTheRequestFixture() {
        val expected = ApiJson.response.parseToJsonElement(Fixtures.text("wizard/review-request.json")) as JsonObject
        val input = WizardForm.from(generationInput()).reviewInput(null, expected["profile"] as JsonObject)
        assertEquals(
            expected,
            ApiJson.request.encodeToJsonElement(
                net.chronoflow.core.model.api.ApiTripReviewInput
                    .serializer(),
                input,
            ),
        )
    }

    @Test
    fun snapshotFormHasCities() {
        val snapshot = ApiJson.response.decodeFromString<ApiTripInputSnapshot>(Fixtures.text("wizard/snapshot.json"))
        assertEquals("Amsterdam", WizardForm.from(snapshot).legs[0].fromCity)
    }

    @Test
    fun unknownEnumValueIsAServerFailure() {
        assertThrows(
            DataFailure.Server::class.java,
        ) { WizardForm.from(generationInput().copy(tripEndKind = "forever")) }
    }
}
