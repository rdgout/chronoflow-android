package net.chronoflow.core.model.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class JSONDocumentTests {
    @Test fun requireValidKeepsRawText() {
        assertEquals("""{"n":1.000}""", JSONDocument.requireValid("""{"n":1.000}"""))
        assertEquals("null", JSONDocument.requireValid("null"))
        assertEquals("1", JSONDocument.requireValid("1"))
        assertEquals("\"x\"", JSONDocument.requireValid("\"x\""))
    }

    @Test fun invalidJsonMessageHidesTheInput() {
        val error = assertThrows(IllegalArgumentException::class.java) { JSONDocument.requireValid("{secret") }
        assertEquals("invalid JSON", error.message)
        assertNull(error.cause)
    }
}
