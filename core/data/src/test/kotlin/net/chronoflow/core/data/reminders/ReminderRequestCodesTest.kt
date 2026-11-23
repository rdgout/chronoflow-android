package net.chronoflow.core.data.reminders

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderRequestCodesTest {
    @Test fun deterministicForOneIdentifier() {
        assertEquals(97, ReminderRequestCodes.allocate("a", emptySet()))
        assertEquals(97, ReminderRequestCodes.allocate("a", emptySet()))
    }

    @Test fun minimumHashIsNonNegative() {
        assertEquals(Int.MIN_VALUE, "polygenelubricants".hashCode())
        assertEquals(0, ReminderRequestCodes.allocate("polygenelubricants", emptySet()))
    }

    @Test fun probingSkipsTakenCodes() {
        assertEquals(100, ReminderRequestCodes.allocate("a", setOf(97, 98, 99)))
    }

    @Test fun probingWrapsToZero() {
        val identifier = "\u0002\u000d\u0000\u0009\u001e\u000c\u0001"
        assertEquals(Int.MAX_VALUE, identifier.hashCode())
        assertEquals(0, ReminderRequestCodes.allocate(identifier, setOf(Int.MAX_VALUE)))
    }
}
