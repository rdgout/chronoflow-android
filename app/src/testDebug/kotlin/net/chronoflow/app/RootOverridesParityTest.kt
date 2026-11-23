package net.chronoflow.app

import net.chronoflow.debug.TestSeed
import org.junit.Assert.assertEquals
import org.junit.Test

class RootOverridesParityTest {
    @Test fun reduceMotionParserMatchesTheDebugSeed() {
        listOf("1", "true", "0", "false", "yes", "", null).forEach { raw ->
            val values = if (raw == null) emptyMap() else mapOf("cf.reduceMotion" to raw)
            assertEquals(TestSeed.parse(values).reduceMotion, RootOverrides.parseFlag(raw))
        }
    }
}
