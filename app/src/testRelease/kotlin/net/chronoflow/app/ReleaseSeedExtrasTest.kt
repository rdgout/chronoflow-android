package net.chronoflow.app

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import java.io.File
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ReleaseSeedExtrasTest {
    @Test fun releaseIgnoresSeedExtras() =
        runTest(timeout = 60.seconds) {
            val app = ApplicationProvider.getApplicationContext<ChronoFlowApp>()
            val intent =
                Intent(app, MainActivity::class.java)
                    .putExtra("CF_AUTH_TEST_ID", "019a562b-8000-7000-8000-000000000001")
                    .putExtra("CF_AUTH_TEST_SIGNED_IN", "demo")
            val activity = Robolectric.buildActivity(MainActivity::class.java, intent).setup()
            try {
                withContext(Dispatchers.IO) {
                    assertFalse(File(app.noBackupFilesDir, "ChronoFlowMock").exists())
                    assertFalse(File(app.filesDir, "uitests").exists())
                }
            } finally {
                activity.pause().stop().destroy()
            }
        }
}
