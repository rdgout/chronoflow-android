package net.chronoflow.core.model.config

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ThemeChoiceResolveTest(
    private val choice: ThemeChoice,
    private val systemDark: Boolean,
    private val expected: Boolean,
) {
    @Test fun resolvesDeviceChoice() = assertEquals(expected, choice.resolve(systemDark))

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}-systemDark={1}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf(ThemeChoice.System, false, false),
                arrayOf(ThemeChoice.System, true, true),
                arrayOf(ThemeChoice.Light, false, false),
                arrayOf(ThemeChoice.Light, true, false),
                arrayOf(ThemeChoice.Dark, false, true),
                arrayOf(ThemeChoice.Dark, true, true),
            )
    }
}
