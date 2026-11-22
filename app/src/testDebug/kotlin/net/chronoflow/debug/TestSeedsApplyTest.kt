package net.chronoflow.debug

import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.app.AppDispatchers
import net.chronoflow.app.ChronoFlowApp
import net.chronoflow.app.SessionTestApplication
import net.chronoflow.app.SoftwareSessionCipher
import net.chronoflow.app.closeAndJoinForTest
import net.chronoflow.core.datastore.DeviceConfigFile
import net.chronoflow.core.datastore.SessionCipher
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = SessionTestApplication::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TestSeedsApplyTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val id = "0a1b2c3d-4e5f-6789-abcd-ef0123456789"

    private fun deviceConfig() = File(context.noBackupFilesDir, DeviceConfigFile.FILE_NAME).readText()

    @Test
    fun applyRecreatesTheTestDirectory() =
        runTest(timeout = 60.seconds) {
            val dir = File(context.filesDir, "uitests/$id")
            dir.mkdirs()
            File(dir, "stale").writeText("old")
            val dispatcher = StandardTestDispatcher(testScheduler)
            try {
                TestSeeds.apply(
                    context,
                    TestSeed.parse(mapOf(TestSeed.TEST_ID_KEY to id)),
                    dispatcher,
                    sessionCipher = SoftwareSessionCipher(),
                    dispatchers = AppDispatchers(dispatcher, dispatcher, dispatcher),
                )
                assertThat(dir.isDirectory).isTrue()
                assertThat(File(dir, "stale").exists()).isFalse()
            } finally {
                (context as ChronoFlowApp).graph.closeAndJoinForTest()
            }
        }

    @Test
    fun signedInGraphIsPublishedAfterCredentialsAreWritten() =
        runTest(timeout = 60.seconds) {
            val app = context as ChronoFlowApp
            val dir = File(context.filesDir, "uitests/$id")
            val delegate = SoftwareSessionCipher()
            var encrypted = false
            var publishedDuringWrite = false
            val cipher =
                object : SessionCipher by delegate {
                    override fun encrypt(
                        alias: String,
                        plaintext: ByteArray,
                    ): SessionCipher.Sealed {
                        encrypted = true
                        publishedDuringWrite = app.graph.overrides.storageDir == dir
                        return delegate.encrypt(alias, plaintext)
                    }
                }
            val dispatcher = StandardTestDispatcher(testScheduler)
            try {
                TestSeeds.apply(
                    context,
                    TestSeed.parse(mapOf(TestSeed.TEST_ID_KEY to id, "CF_AUTH_TEST_SIGNED_IN" to "demo")),
                    dispatcher,
                    sessionCipher = cipher,
                    dispatchers = AppDispatchers(dispatcher, dispatcher, dispatcher),
                )
                assertThat(encrypted).isTrue()
                assertThat(publishedDuringWrite).isFalse()
                assertThat(app.graph.overrides.storageDir).isEqualTo(dir)
            } finally {
                app.graph.closeAndJoinForTest()
            }
        }

    @Test
    fun languageAndThemeBelowTheirApiLevelsGoToTheDeviceConfig() =
        runTest(timeout = 60.seconds) {
            val io = StandardTestDispatcher(testScheduler)
            TestSeeds.apply(context, TestSeed.parse(mapOf("cf.settings.language" to "nl")), io, sdkInt = 32)
            assertThat(deviceConfig()).isEqualTo("language=nl\ntheme=system\n")
            TestSeeds.apply(context, TestSeed.parse(mapOf("cf.settings.theme" to "dark")), io, sdkInt = 30)
            assertThat(deviceConfig()).isEqualTo("language=nl\ntheme=dark\n")
        }

    @Test
    fun forwardIntentCarriesEveryRawExtraAndBothFlags() {
        val seed = TestSeed.parse(mapOf(TestSeed.TEST_ID_KEY to id, "cf.settings.language" to "nl", "CF_FOO" to "bar"))
        val intent = TestSeeds.forwardIntent(context, seed)
        assertThat(intent.getStringExtra("cf.settings.language")).isEqualTo("nl")
        assertThat(intent.getStringExtra("CF_FOO")).isEqualTo("bar")
        assertThat(intent.getStringExtra(TestSeed.TEST_ID_KEY)).isEqualTo(id)
        val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        assertThat(intent.flags and flags).isEqualTo(flags)
    }
}
