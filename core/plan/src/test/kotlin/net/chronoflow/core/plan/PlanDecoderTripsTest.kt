package net.chronoflow.core.plan

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.store.TripRecord
import net.chronoflow.core.testing.sync.card
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class PlanDecoderTripsTest {
    private fun record(id: String = "good") =
        TripRecord(
            id,
            ApiJson.response.encodeToString(TripCardView.serializer(), card(id).card),
            departureUtc = card(id).card.departureUtc,
        )

    @Test fun tombstonedAndLocallyDeletedRecordsAreSkipped() =
        runTest(timeout = 60.seconds) {
            val decoder = PlanDecoder(StandardTestDispatcher(testScheduler))
            val result =
                decoder.trips(
                    listOf(record().copy(deletedAt = "now"), record().copy(deletedLocally = true)),
                    Instant.EPOCH,
                )
            assertTrue(result.values.isEmpty())
            assertNull(result.error)
        }

    @Test fun badCardsAreSkippedAndTheFirstErrorIsKept() =
        runTest(timeout = 60.seconds) {
            val decoder = PlanDecoder(StandardTestDispatcher(testScheduler))
            val result =
                decoder.trips(
                    listOf(record().copy(card = "{first"), record(), record().copy(card = "{second")),
                    Instant.EPOCH,
                )
            assertEquals(listOf("good"), result.values.map { it.card.id })
            assertNotNull(result.error)
            assertTrue(result.error!!.message!!.contains("first"))
        }

    @Test fun decodingRunsOnTheInjectedWorker() =
        runTest(timeout = 60.seconds) {
            val worker = CountingDispatcher(StandardTestDispatcher(testScheduler))
            val reads = mutableListOf<Boolean>()
            val records =
                object : AbstractList<TripRecord>() {
                    override val size: Int get() = 1

                    override fun get(index: Int): TripRecord {
                        reads += worker.running
                        return record()
                    }
                }
            val decoder = PlanDecoder(worker)
            decoder.trips(records, Instant.EPOCH)
            assertTrue(worker.dispatches > 0)
            assertTrue(reads.isNotEmpty() && reads.all { it })
            assertEquals(card().card.copy(id = "good"), decoder.card(record()))
            assertNull(decoder.adaptation(record()))
        }
}
