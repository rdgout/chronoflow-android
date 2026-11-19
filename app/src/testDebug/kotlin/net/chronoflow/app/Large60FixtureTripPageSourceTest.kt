package net.chronoflow.app

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.app.perf.FixtureTripPageSource
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.plan.CopyCatalogProvider
import net.chronoflow.core.plan.PageCopy
import net.chronoflow.core.plan.PageCopyTable
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.plan.PlanPreparer
import net.chronoflow.core.plan.trippage.ListRows
import net.chronoflow.core.plan.trippage.openingDay
import net.chronoflow.core.testing.LargeTimelineFixtures
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class Large60FixtureTripPageSourceTest {
    @Test fun large60DecodesTo60Days() =
        runTest(timeout = 60.seconds) {
            val input = FixtureTripPageSource("large60").snapshots("fixture-large60").first()
            val detail =
                PlanDecoder(
                    Dispatchers.Default,
                ).detail(input)!!
            assertThat(detail.plan.days.size).isEqualTo(60)
        }

    @Test fun large60ListBenchmarkOpeningTagMatchesTheFixture() =
        runTest(timeout = 60.seconds) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val decoder = PlanDecoder(dispatcher)
            val detail = decoder.detail(FixtureTripPageSource("large60").snapshots("fixture-large60").first())!!
            val preparer = PlanPreparer(decoder, CopyCatalogProvider(dispatcher), dispatcher)
            val plan = preparer.prepare(detail, emptyList(), Language.En, TimeFormat.Value24, emptyList())
            val day =
                openingDay(
                    detail.plan.days,
                    detail.plan.blocks,
                    null,
                    null,
                    null,
                    Instant.parse("2026-10-13T08:00:00Z"),
                )!!
            val rows = ListRows.build(plan, PageCopy(Language.En, PageCopyTable.load()))
            val opening = rows.first { it.dayId == day.id }.rows.first()
            assertThat(day.id).isEqualTo(
                detail.plan.days
                    .first()
                    .id,
            )
            assertThat(opening.blockId).isEqualTo("caffeine-no_caffeine-29866935-caffeine-day-0")
        }
}
