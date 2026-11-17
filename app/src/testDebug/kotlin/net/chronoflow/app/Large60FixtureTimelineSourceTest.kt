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
import kotlin.time.Duration.Companion.seconds

class Large60FixtureTimelineSourceTest {
    @Test fun large60DecodesTo60Days() =
        runTest(timeout = 60.seconds) {
            val input = FixtureTimelineSource(LargeTimelineFixtures.detailBytes("large60")).inputs().first()
            val detail =
                PlanDecoder(
                    Dispatchers.Default,
                ).detail(PlanVersionRecord("fixture", "large60", input.detailBytes, ""))
            assertThat(detail.plan.days.size).isEqualTo(60)
        }
}
