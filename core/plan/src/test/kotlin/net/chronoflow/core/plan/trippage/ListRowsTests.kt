package net.chronoflow.core.plan.trippage

import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.plan.PageCopy
import net.chronoflow.core.plan.PreparedBlock
import net.chronoflow.core.plan.PreparedPlan
import net.chronoflow.core.plan.prepare
import net.chronoflow.core.plan.testPageCopy
import net.chronoflow.core.testing.LargeTimelineFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListRowsTests {
    private val plan by lazy { prepare(LargeTimelineFixtures.fixture("S3").detail) }
    private val en = PageCopy(Language.En, testPageCopy)

    private fun rows(
        plan: PreparedPlan = this.plan,
        copy: PageCopy = en,
    ) = ListRows.build(plan, copy).flatMap { it.rows }

    private fun one(block: PreparedBlock) = plan.copy(days = listOf(plan.days[0].copy(blocks = listOf(block))))

    @Test fun large60Counts() {
        val days = ListRows.build(prepare(LargeTimelineFixtures.large60.detail), en)
        val rows = days.flatMap { it.rows }
        assertEquals(60, days.size)
        assertEquals(740, rows.size)
        assertEquals(20, rows.count { it.zone })
        assertEquals(rows.size, rows.map { it.key }.toSet().size)
    }

    @Test fun labelsMatchIos() {
        val block = plan.days.flatMap { it.blocks }.first { it.block.priority == Priority.Core }
        val core = rows(one(block)).single()
        assertEquals("${core.title}, ${core.interval}, Important", core.label)
        assertTrue(rows(one(block.copy(status = CheckStatus.Done))).single().label.endsWith(", Marked done"))
        val recommended = rows(one(block.copy(block = block.block.copy(priority = Priority.Recommended)))).single()
        assertEquals("${recommended.title}, ${recommended.interval}", recommended.label)
        val zone = rows().first { it.zone }
        assertEquals(
            listOfNotNull(zone.title, zone.zoneInstruction, zone.zoneComparison).joinToString(". "),
            zone.label,
        )
        assertEquals(zone.label, zone.labelNow)
    }

    @Test fun nowVariants() {
        val block =
            plan.days[0].blocks.first {
                it.block.type !=
                    net.chronoflow.core.model.contracts.BlockType.ZoneChange
            }
        val english = rows(one(block)).single()
        val dutch = rows(one(block), PageCopy(Language.Nl, testPageCopy)).single()
        assertEquals("${english.interval} · now", english.intervalNow)
        assertEquals("${dutch.interval} · nu", dutch.intervalNow)
        assertEquals(english.intervalNow, english.labelNow.split(", ")[1])
        assertEquals(dutch.intervalNow, dutch.labelNow.split(", ")[1])
    }

    @Test fun crossMidnightBlockHasTwoKeys() {
        val block = plan.days[0].blocks[0]
        val input =
            plan.copy(
                days =
                    listOf(
                        plan.days[0].copy(blocks = listOf(block.copy(continuesAfter = true))),
                        plan.days[1].copy(blocks = listOf(block.copy(continuesBefore = true))),
                    ),
            )
        val rows = rows(input)
        assertEquals("r:${plan.days[0].id}:${block.id}", rows[0].key)
        assertEquals("r:${plan.days[1].id}:${block.id}", rows[1].key)
    }

    @Test fun unparsableTimesNeverRun() {
        val block = plan.days[0].blocks[0]
        val invalid = rows(one(block.copy(block = block.block.copy(startUtc = "invalid")))).single()
        assertEquals(Long.MAX_VALUE, invalid.startMs)
        assertEquals(Long.MIN_VALUE, invalid.endMs)
    }
}
