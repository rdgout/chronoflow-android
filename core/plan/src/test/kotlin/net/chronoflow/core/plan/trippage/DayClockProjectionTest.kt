package net.chronoflow.core.plan.trippage

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.contracts.DayView
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.plan.CopyFormat
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.DayLayout
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.plan.preparer
import net.chronoflow.core.plan.testCatalog
import net.chronoflow.core.testing.LargeTimelineFixtures
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

/** (new) The clock projection the page writes once per tick: offset and text together, one write per changed day. */
class DayClockProjectionTest {
    private val detail = LargeTimelineFixtures.large60.detail
    private val days: List<DayView> = detail.plan.days
    private val formatter = CopyLineFormatter(testCatalog)
    private val format = CopyFormat(Language.En, TimeFormat.Value24)
    private val layouts: Map<String, DayLayout> by lazy {
        var result: Map<String, DayLayout> = emptyMap()
        runTest(timeout = 60.seconds) { result = preparer().layouts(detail, emptyList()) }
        result
    }

    private fun start(day: DayView) = checkNotNull(planDate(day.startUtc))

    private fun end(day: DayView) = checkNotNull(planDate(day.endUtc))

    private fun project(now: Instant) = projectDayClocks(layouts, now, formatter, format)

    @Test fun theStartOfADayProjectsOnlyThatDayAtOffsetZero() {
        val result = project(start(days[0]))
        assertThat(result.keys).isEqualTo(setOf(days[0].id))
        assertThat(result.getValue(days[0].id).offset).isEqualTo(0.0)
        assertThat(result.getValue(days[0].id).nowMs).isEqualTo(start(days[0]).toEpochMilli())
    }

    @Test fun aMinuteLaterChangesOnlyThatDay() {
        val before = project(start(days[0]))
        val after = project(start(days[0]).plusSeconds(60))
        assertThat(changedDayIds(before, after)).isEqualTo(setOf(days[0].id))
        assertThat(after.getValue(days[0].id).offset).isCloseTo(2.0 / 3.0, 1e-9)
    }

    @Test fun aDayBoundaryChangesTheLeavingAndTheEnteringDay() {
        val before = project(start(days[0]).plusSeconds(60))
        val after = project(start(days[1]))
        assertThat(changedDayIds(before, after)).isEqualTo(setOf(days[0].id, days[1].id))
    }

    @Test fun theSameInstantTwiceChangesNothing() {
        val now = start(days[5]).plusSeconds(3_600)
        assertThat(changedDayIds(project(now), project(now))).isEmpty()
    }

    @Test fun beforeThePlanAndAfterItsLastEndProjectNothing() {
        assertThat(project(start(days.first()).minusSeconds(1))).isEmpty()
        assertThat(project(end(days.last()))).isEmpty()
        assertThat(project(end(days.last()).plusSeconds(86_400))).isEmpty()
    }

    @Test fun nowAtADayEndBelongsToTheNextDay() {
        assertThat(project(end(days[0])).keys).isEqualTo(setOf(days[1].id))
    }

    @Test fun longAndShortDaysUseUtcMinutes() {
        for ((index, height) in listOf(2 to 540.0, 3 to 1020.0)) {
            val day = days[index]
            assertThat(layouts.getValue(day.id).height).isEqualTo(height)
            val value = project(end(day).minusSeconds(60)).getValue(day.id)
            assertThat(value.offset).isCloseTo(height - 2.0 / 3.0, 1e-6)
        }
    }

    @Test fun aMinuteTickChangesOneDayAndEveryInnerBoundaryChangesTwo() {
        for (index in days.indices) {
            val day = days[index]
            val mid = project(start(day).plusSeconds(60))
            val tick = project(start(day).plusSeconds(120))
            assertThat(changedDayIds(mid, tick)).isEqualTo(setOf(day.id))
            val last = project(end(day).minusSeconds(60))
            val boundary = project(end(day))
            val expected = if (index == days.lastIndex) setOf(day.id) else setOf(day.id, days[index + 1].id)
            assertThat(changedDayIds(last, boundary)).isEqualTo(expected)
        }
    }

    @Test fun theTextEqualsThePreparersNowText() =
        runTest(timeout = 60.seconds) {
            val preparer = preparer()
            val now = start(days[7]).plusSeconds(5 * 3_600 + 1_800)
            val texts = preparer.nowTexts(layouts, now, Language.En, TimeFormat.Value24)
            val projected = project(now)
            assertThat(projected.mapValues { it.value.text }).isEqualTo(texts)
            assertThat(projected.size).isEqualTo(1)
        }
}
