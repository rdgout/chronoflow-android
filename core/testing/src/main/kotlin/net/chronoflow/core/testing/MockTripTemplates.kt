package net.chronoflow.core.testing

import kotlinx.serialization.Serializable
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripDetail

@Serializable
data class Template(
    val card: TripCardView,
    val detail: TripDetail,
)

@Serializable
private class Collection(
    val trips: List<Template>,
)

/** The seven trips of the iOS mock seed (`MockTripFixtures.templates()`); templates 0 and 2 are S1 and S3. */
object MockTripTemplates {
    private val decoded: List<Template> by lazy {
        ApiJson.response.decodeFromString(Collection.serializer(), Fixtures.text("mock-seed.json")).trips
    }

    fun templates(): List<Template> = decoded
}
