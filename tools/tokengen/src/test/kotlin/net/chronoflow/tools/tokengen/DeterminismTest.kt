package net.chronoflow.tools.tokengen

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class DeterminismTest {
    @Test
    fun twoRunsAreByteIdenticalInAnyLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("nl-NL"))
            val dutch = tokenOutputs(repoRoot)
            Locale.setDefault(Locale.forLanguageTag("en-US"))
            val english = tokenOutputs(repoRoot)
            assertEquals(dutch.keys, english.keys)
            for (path in dutch.keys) assertArrayEquals(path, dutch.getValue(path), english.getValue(path))
        } finally {
            Locale.setDefault(original)
        }
    }
}
