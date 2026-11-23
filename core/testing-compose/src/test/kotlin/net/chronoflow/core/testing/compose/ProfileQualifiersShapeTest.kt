package net.chronoflow.core.testing.compose

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ProfileQualifiersShapeTest(
    private val profile: DeviceProfile,
    private val language: AppLanguage,
    private val dark: Boolean,
) {
    @Test
    fun hasTheFullShape() {
        val shape = Regex("""(en-rUS|nl-rNL)-w\d+dp-h\d+dp-(port|land)-(night|notnight)-\d+dpi""")
        assertThat(shape.matches(profileQualifiers(profile, language, dark))).isEqualTo(true)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}-{1}-dark={2}")
        fun cases(): List<Array<Any>> =
            DeviceProfiles.all.flatMap { profile ->
                AppLanguage.entries.flatMap { language ->
                    listOf(false, true).map { arrayOf<Any>(profile, language, it) }
                }
            }
    }
}
