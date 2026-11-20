package net.chronoflow.app

import android.content.Context
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.datastore.DeviceConfigFile
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@Layout
@RunWith(AndroidJUnit4::class)
@Config(sdk = [29, 32, 33, 36], application = android.app.Application::class)
class ConfigurationPolicyTest {
    private val base: Context get() = RuntimeEnvironment.getApplication()

    private fun applied(): android.content.res.Configuration {
        val wrapper = ContextThemeWrapper(base, 0)
        wrapper.applyOverrideConfiguration(ConfigurationPolicy.overrideConfiguration(base))
        return wrapper.resources.configuration
    }

    private fun writeDeviceConfig(bytes: ByteArray) =
        File(base.noBackupFilesDir, DeviceConfigFile.FILE_NAME).writeBytes(bytes)

    @Test
    @Config(qualifiers = "ar-rEG")
    fun arabicPhoneGetsEnglishLeftToRight() {
        val configuration = applied()
        assertThat(configuration.locales[0].toLanguageTag()).isEqualTo("en")
        assertThat(configuration.layoutDirection).isEqualTo(View.LAYOUT_DIRECTION_LTR)
    }

    @Test
    @Config(qualifiers = "de-rDE")
    fun germanPhoneGetsEnglish() {
        assertThat(applied().locales[0].toLanguageTag()).isEqualTo("en")
    }

    @Test
    @Config(qualifiers = "nl-rBE")
    fun belgianDutchKeepsItsRegion() {
        assertThat(applied().locales[0].toLanguageTag()).isEqualTo("nl-BE")
    }

    @Test
    fun fontScaleTwoIsCappedAtOnePointThree() {
        RuntimeEnvironment.setFontScale(2.0f)
        assertThat(applied().fontScale).isEqualTo(1.3f)
    }

    @Test
    fun fontScaleOneStaysOne() {
        RuntimeEnvironment.setFontScale(1.0f)
        assertThat(applied().fontScale).isEqualTo(1.0f)
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun deviceConfigLanguageAppliesOnlyBelowApi33() {
        writeDeviceConfig("language=nl\n".toByteArray())
        val expected = if (Build.VERSION.SDK_INT >= 33) "en-US" else "nl"
        assertThat(applied().locales[0].toLanguageTag()).isEqualTo(expected)
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun oversizedDeviceConfigIsIgnored() {
        writeDeviceConfig(("language=nl\n" + " ".repeat(53)).toByteArray())
        assertThat(applied().locales[0].toLanguageTag()).isEqualTo("en-US")
    }
}
