package net.chronoflow.app

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.chronoflow.app.perf.FixtureTimelineSource
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.plan.PlanDecoder
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class FixtureTimelineSourceTest {
    private val names = listOf("S1", "S2", "S3", "S4", "S5", "S6", "large30", "large60")

    @Test fun everyFixtureEmitsBytesThePlanDecoderDecodes() =
        runTest(timeout = 60.seconds) {
            val decoder = PlanDecoder(Dispatchers.Default)
            for (name in names) {
                val input = FixtureTimelineSource(name, Dispatchers.Default).inputs().first()
                val detail = decoder.detail(PlanVersionRecord("fixture", name, input.detailBytes, ""))
                assertThat(detail.plan.days.isNotEmpty()).isEqualTo(true)
            }
        }

    @Test fun large60DecodesTo60Days() =
        runTest(timeout = 60.seconds) {
            val input = FixtureTimelineSource("large60", Dispatchers.Default).inputs().first()
            val detail =
                PlanDecoder(
                    Dispatchers.Default,
                ).detail(PlanVersionRecord("fixture", "large60", input.detailBytes, ""))
            assertThat(detail.plan.days.size).isEqualTo(60)
        }
}
