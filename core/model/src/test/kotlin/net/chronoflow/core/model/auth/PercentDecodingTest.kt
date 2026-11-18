package net.chronoflow.core.model.auth

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class PercentDecodingTest(
    private val input: String,
    private val expected: String?,
) {
    @Test fun decodesStrictUtf8Once() {
        assertEquals(expected, percentDecodeOrNull(input))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "input={0}")
        fun cases(): List<Array<String?>> =
            listOf(
                arrayOf("%2B", "+"),
                arrayOf("+", "+"),
                arrayOf("%252F", "%2F"),
                arrayOf("%C3%A9", "é"),
                arrayOf("%", null),
                arrayOf("%4", null),
                arrayOf("%ZZ", null),
                arrayOf("%FF", null),
            )
    }
}
