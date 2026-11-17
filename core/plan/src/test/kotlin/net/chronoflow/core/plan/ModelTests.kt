package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.store.PlanVersionRecord
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ModelTests {
    private fun reencoded(value: TripDetail) =
        PlanVersionRecord(
            value.trip.id,
            value.plan.meta.inputHash,
            ApiJson.response.encodeToString(TripDetail.serializer(), value).encodeToByteArray(),
            "now",
            value.detailVersion,
        )

    @Test fun versionAndRawPayloadInvalidateCache() =
        runTest(timeout = 60.seconds) {
            val decoder = PlanDecoder(StandardTestDispatcher(testScheduler))
            var value = tripDetail()
            decoder.detail(reencoded(value))
            decoder.detail(reencoded(value))
            assertThat(decoder.decodeCount).isEqualTo(1)
            value = value.copy(detailVersion = "new-version")
            assertThat(decoder.detail(reencoded(value)).detailVersion).isEqualTo("new-version")
            assertThat(decoder.decodeCount).isEqualTo(2)
            value = value.copy(plan = value.plan.copy(blocks = emptyList()))
            assertThat(decoder.detail(reencoded(value)).plan.blocks).isEmpty()
            assertThat(decoder.decodeCount).isEqualTo(3)
        }

    @Test fun localProgressCountsStartsAndKeepsSkippedSeparate() {
        val value = tripDetail()
        val blocks = value.plan.blocks.filter { it.actionable && it.checkKey != null }
        val marks =
            blocks.take(2).mapIndexed { index, block ->
                CheckMark(block.checkKey!!, if (index == 0) CheckStatus.Done else CheckStatus.Skipped, block.startUtc)
            }
        val state = buildCheckState(value.plan, marks)
        assertThat(state.marks.size).isEqualTo(marks.size)
        assertThat(state.days.sumOf { it.done }).isEqualTo(minOf(1, marks.size))
        assertThat(state.days.sumOf { it.skipped }).isEqualTo(maxOf(0, marks.size - 1))
    }
}
