package net.chronoflow.core.data.sync

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.sync.support.SyncStore
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.AdaptationDirection
import net.chronoflow.core.model.contracts.AdaptationGoal
import net.chronoflow.core.model.contracts.AdaptationView
import net.chronoflow.core.testing.sync.card
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TripRepositoryJsonTests {
    @Test fun generatedTripJSONUsesCanonicalKeyOrder() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val adaptation = AdaptationView(AdaptationGoal.Full, AdaptationDirection.Earlier, 60)
                val item = card().copy(adaptation = adaptation)
                store.database.write { TripRepository.applyCards(listOf(item), "then", it) }
                val stored = TripRepository(store.database).trips().single()
                val objectValue = ApiJson.response.parseToJsonElement(stored.card) as JsonObject
                assertEquals(
                    listOf(
                        "arrivalUtc",
                        "arriveLocal",
                        "canComplete",
                        "canDelete",
                        "canDuplicate",
                        "canEdit",
                        "canReopen",
                        "connections",
                        "departLocal",
                        "departureUtc",
                        "from",
                        "id",
                        "offsetDifferenceMinutes",
                        "progress",
                        "to",
                    ),
                    objectValue.keys.toList(),
                )
                assertEquals("""{"direction":"earlier","goal":"full","shiftMin":60}""", stored.adaptation)
            } finally {
                store.close()
            }
        }
}
