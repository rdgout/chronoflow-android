package net.chronoflow.core.testing.compose

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfiles
import org.junit.Test

class ProfileQualifiersTest {
    @Test
    fun smallDutchLight() {
        assertThat(profileQualifiers(DeviceProfiles.small, AppLanguage.Nl, dark = false))
            .isEqualTo("nl-rNL-w360dp-h640dp-port-notnight-320dpi")
    }

    @Test
    fun pixel8EnglishDark() {
        assertThat(profileQualifiers(DeviceProfiles.pixel8, AppLanguage.En, dark = true))
            .isEqualTo("en-rUS-w411dp-h914dp-port-night-420dpi")
    }

    @Test
    fun landscapeSideNavEnglishLight() {
        assertThat(profileQualifiers(DeviceProfiles.landscapeSideNav, AppLanguage.En, dark = false))
            .isEqualTo("en-rUS-w914dp-h411dp-land-notnight-420dpi")
    }

    @Test
    fun everyProfileLanguageAndThemeHasTheFullShape() {
        val shape = Regex("""(en-rUS|nl-rNL)-w\d+dp-h\d+dp-(port|land)-(night|notnight)-\d+dpi""")
        for (profile in DeviceProfiles.all) {
            for (language in AppLanguage.entries) {
                for (dark in listOf(false, true)) {
                    assertThat(shape.matches(profileQualifiers(profile, language, dark))).isEqualTo(true)
                }
            }
        }
    }

    @Test
    fun layoutMatrixIsTheLightOnlyDefault() {
        assertThat(LayoutMatrix.phone.map { it.toString() }).isEqualTo(listOf("small-nl", "largeFont-nl", "pixel8-en"))
        assertThat(LayoutMatrix.wide.map { it.toString() })
            .isEqualTo(listOf("small-nl", "largeFont-nl", "pixel8-en", "foldInner-en", "landscapeSideNav-en"))
    }
}
