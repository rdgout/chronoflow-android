package net.chronoflow.feature.trip.list

import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.PageCopy
import net.chronoflow.core.plan.PageCopyTable
import net.chronoflow.core.plan.trippage.ListRows
import net.chronoflow.feature.trip.timeline.TimelineTestPlans
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListModeIndexTest {
    @Test fun headersRowsAndNonDayItems() {
        val rows = ListTestRows.days
        val index = ListModeIndex(rows.map { it.dayId }, rows, true)
        val first = rows[0]
        val second = rows[1]
        assertEquals(2, index.headerIndex(first.dayId))
        assertEquals(3 + first.rows.size, index.headerIndex(second.dayId))
        assertEquals(first.dayId, index.dayAt(index.rowIndex(first.rows[0].key)))
        assertEquals(second.dayId, index.dayAt(index.headerIndex(second.dayId)))
        assertNull(index.dayAt(index.summary))
        assertNull(index.dayAt(index.days))
        assertNull(index.dayAt(index.end))
        assertNull(index.dayAt(index.hidden!!))
    }

    @Test fun large60Has803ItemsWithoutHiddenSteps() {
        val plan = TimelineTestPlans.prepared("large60").plan
        val rows = ListRows.build(plan, PageCopy(Language.En, PageCopyTable.load()))
        assertEquals(803, ListModeIndex(rows.map { it.dayId }, rows, false).itemCount)
    }

    @Test fun emptyDayHasOnlyItsHeader() {
        val rows = ListTestRows.days.take(1).map { it.copy(rows = emptyList()) }
        val index = ListModeIndex(rows.map { it.dayId }, rows, false)
        assertEquals(4, index.itemCount)
        assertEquals(rows[0].dayId, index.dayAt(2))
        assertNull(index.dayAt(3))
    }
}
