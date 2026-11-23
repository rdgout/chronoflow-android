package net.chronoflow.core.datamock

import org.junit.Assert.assertEquals
import org.junit.Test

/** Exhaustiveness of the independent method-control table (new). */
class MockMethodCoverageTest {
    @Test fun tableCoversEveryMethod() {
        assertEquals(MockMethod.entries.toSet(), MOCK_METHOD_CALLS.keys)
    }
}
