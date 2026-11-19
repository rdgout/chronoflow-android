package net.chronoflow.core.model.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class StoreTimeTests {
    private val date = Instant.ofEpochSecond(1_800_000_000)

    @Test fun exactUTCForms() {
        assertEquals("2027-01-15T08:00:00.000Z", StoreTime.millis(date))
        assertEquals("2027-01-15T08:00:00Z", StoreTime.seconds(date))
        assertEquals("2027-01-15T08:00:00.001Z", StoreTime.millis(date.plusMillis(1)))
        assertEquals("2027-01-15T08:00:00.010Z", StoreTime.millis(date.plusMillis(10)))
    }

    @Test fun stringOrderEqualsInstantOrder() {
        val values = (0L until 1000L).map { StoreTime.millis(date.plusMillis(it)) }
        assertEquals(values, values.sorted())
        assertEquals(1000, values.toSet().size)
        assertTrue(values.all { it.length == 24 })
    }
}
