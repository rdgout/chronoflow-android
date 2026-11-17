package net.chronoflow.core.plan

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.Lane
import net.chronoflow.core.model.store.CheckMarkRecord
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.model.store.TripPlanSnapshot
import net.chronoflow.core.testing.Fixtures
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class PlanDecoderTests {
    private fun TestScope.decoder() = PlanDecoder(StandardTestDispatcher(testScheduler))

    @Test fun everyPlanFixtureDecodes() {
        val failures = mutableListOf<String>()
        for (name in listOf("S1", "S2", "S3", "S4", "S5", "S6", "S6-RETURN-10N")) {
            val f = fixture(name)
            if (f.plan.blocks.isEmpty() || f.plan.days.isEmpty()) failures += name
        }
        assertThat(failures).isEqualTo(emptyList<String>())
    }

    @Test fun tripDetailDecodesThroughThePlanDecoder() =
        runTest(timeout = 60.seconds) {
            val record = recordOf(tripDetail())
            val value = decoder().detail(record)
            assertThat(value.detailVersion).isEqualTo("v1")
            assertThat(value).isEqualTo(tripDetail())
        }

    @Test fun unknownTypeAndLaneDecodeToUnknown() =
        runTest(timeout = 60.seconds) {
            val root = ApiJson.response.parseToJsonElement(Fixtures.text("trips/detail.json")).jsonObject
            val plan = root.getValue("plan").jsonObject
            val blocks = plan.getValue("blocks").jsonArray
            val first =
                JsonObject(
                    blocks[0].jsonObject +
                        mapOf("type" to JsonPrimitive("future_type"), "lane" to JsonPrimitive("future_lane")),
                )
            val edited =
                JsonObject(
                    root + ("plan" to JsonObject(plan + ("blocks" to JsonArray(listOf(first) + blocks.drop(1))))),
                )
            val bytes = edited.toString().encodeToByteArray()
            val value = decoder().detail(PlanVersionRecord("t", "h", bytes, "now", "v1"))
            assertThat(value.plan.blocks[0].type).isEqualTo(BlockType.Unknown("future_type"))
            assertThat(value.plan.blocks[0].lane).isEqualTo(Lane.Unknown("future_lane"))
        }

    @Test fun decodingRunsOnTheInjectedWorker() =
        runTest(timeout = 60.seconds) {
            val worker = CountingDispatcher(StandardTestDispatcher(testScheduler))
            PlanDecoder(worker).detail(recordOf(tripDetail()))
            assertThat(worker.dispatches).isGreaterThan(0)
        }

    @Test fun evictForcesTheNextCallToDecodeAgain() =
        runTest(timeout = 60.seconds) {
            val decoder = decoder()
            val record = recordOf(tripDetail())
            decoder.detail(record)
            decoder.detail(record)
            assertThat(decoder.decodeCount).isEqualTo(1)
            decoder.evict(record.tripId)
            decoder.detail(record)
            assertThat(decoder.decodeCount).isEqualTo(2)
        }

    @Test fun decodeErrorsPropagate() =
        runTest(timeout = 60.seconds) {
            val record = PlanVersionRecord("t", "h", "{".encodeToByteArray(), "now")
            assertFailure { decoder().detail(record) }.isInstanceOf<SerializationException>()
        }

    @Test fun snapshotMergesMarks() =
        runTest(timeout = 60.seconds) {
            val decoder = decoder()
            assertThat(decoder.detail(TripPlanSnapshot(plan = null))).isNull()
            val value = tripDetail()
            val record = recordOf(value)
            val block = value.plan.blocks.first { it.actionable && it.checkKey != null }
            val key = block.checkKey!!
            val row =
                CheckMarkRecord(record.tripId, record.inputHash, key.type.raw, key.localDate, key.ordinal, "done", "t")
            val merged = decoder.detail(TripPlanSnapshot(record, listOf(row)))!!
            assertThat(merged.checkState.marks.size).isEqualTo(1)
            assertThat(decoder.detail(record).checkState.marks).isEmpty()
            assertThat(decoder.decodeCount).isEqualTo(1)
        }
}
