package net.chronoflow.app

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.chronoflow.app.perf.FixtureTimelineSource
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.testing.LargeTimelineFixtures
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class FixtureTimelineSourceTest(
    private val name: String,
) {
    @Test fun everyFixtureEmitsBytesThePlanDecoderDecodes() =
        runTest(timeout = 60.seconds) {
            val decoder = PlanDecoder(Dispatchers.Default)
            val input = FixtureTimelineSource(LargeTimelineFixtures.detailBytes(name)).inputs().first()
            val detail = decoder.detail(PlanVersionRecord("fixture", name, input.detailBytes, ""))
            assertThat(detail.plan.days.isNotEmpty()).isEqualTo(true)
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun fixtures(): Collection<Array<String>> =
            listOf("S1", "S2", "S3", "S4", "S5", "S6", "large30", "large60").map { arrayOf(it) }
    }
}
