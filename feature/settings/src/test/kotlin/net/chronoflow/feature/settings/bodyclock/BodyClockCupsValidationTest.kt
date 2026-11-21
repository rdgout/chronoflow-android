package net.chronoflow.feature.settings.bodyclock

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class BodyClockCupsValidationTest(
    private val value: String,
    private val valid: Boolean,
) {
    @Test fun cupsMustBeJsonWholeNumbersInRange() {
        val answers = json("""{"caffeineHabit":"cups","cupsFilterCoffee":$value}""")
        assertEquals(valid, SettingsBodyClockModel.validate(BodyClockGroup.Caffeine, answers).isEmpty())
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "cups={0}-valid={1}")
        fun cases() =
            listOf(
                arrayOf<Any>("true", false),
                arrayOf<Any>("\"2\"", false),
                arrayOf<Any>("2.5", false),
                arrayOf<Any>("-1", false),
                arrayOf<Any>("21", false),
                arrayOf<Any>("2", true),
            )
    }
}
