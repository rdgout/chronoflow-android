package net.chronoflow.core.plan

import net.chronoflow.core.testing.LargeTimelineFixtures
import net.chronoflow.core.testing.tiers.TimingGate
import org.junit.Assert.assertTrue
import org.junit.Test

@TimingGate
class TodayViewBudgetTest {
    @Test fun todayViewOnLarge60StaysWithinTwentyMillisecondsPerSample() {
        val detail = LargeTimelineFixtures.large60.detail
        val now =
            planDate(
                detail.plan.days
                    .first()
                    .startUtc,
            )!!
        todayView(detail, detail.checkState.marks, now)
        repeat(10) { sample ->
            val start = System.nanoTime()
            val view = todayView(detail, detail.checkState.marks, now)
            val elapsed = (System.nanoTime() - start) / 1_000_000.0
            assertTrue("sample $sample produced no plan", view.currentDay != null)
            println("todayView large60 sample=$sample elapsedMs=$elapsed")
            assertTrue("sample $sample exceeded 20 ms: $elapsed", elapsed <= 20.0)
        }
    }
}
