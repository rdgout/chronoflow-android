package net.chronoflow.core.testing

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.plan.planDate
import org.junit.Test

class ProfileFixtureCoverageTest {
    @Test fun firstSixDayTourCoversEveryLarge60RowStyle() {
        val plan = LargeTimelineFixtures.large60.detail.plan
        val start = checkNotNull(planDate(plan.days.first().startUtc))
        val end = checkNotNull(planDate(plan.days[5].endUtc))
        val firstTour =
            plan.blocks.filter {
                checkNotNull(planDate(it.startUtc)) < end && checkNotNull(planDate(it.endUtc)) >= start
            }
        assertThat(firstTour.map { it.type to it.priority }.toSet())
            .isEqualTo(plan.blocks.map { it.type to it.priority }.toSet())
    }
}
