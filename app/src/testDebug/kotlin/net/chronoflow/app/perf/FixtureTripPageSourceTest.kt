package net.chronoflow.app.perf

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.testing.LargeTimelineFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class FixtureTripPageSourceTest {
    @Test fun profileCheckAndUndoReachTheRealDecoder() =
        runTest(timeout = 60.seconds) {
            val detail = LargeTimelineFixtures.fixture("large60").detail
            val source = FixtureTripPageSource("large60", LargeTimelineFixtures.detailBytes("large60"))
            val block = detail.plan.blocks.first { it.actionable && it.checkKey != null }
            val key = checkNotNull(block.checkKey)
            val change =
                CheckOffChange(
                    "fixture-large60",
                    detail.plan.meta.inputHash,
                    key.type,
                    key.localDate,
                    key.ordinal,
                    CheckStatus.Done,
                )
            val decoder = PlanDecoder(StandardTestDispatcher(testScheduler))
            source.enqueueCheck(change)
            val done = checkNotNull(decoder.detail(source.snapshots("fixture-large60").first()))
            assertEquals(
                CheckStatus.Done,
                done.checkState.marks
                    .first { it.key == key }
                    .status,
            )
            source.enqueueCheck(change.copy(status = null))
            val undone = checkNotNull(decoder.detail(source.snapshots("fixture-large60").first()))
            assertTrue(undone.checkState.marks.none { it.key == key })
        }
}
