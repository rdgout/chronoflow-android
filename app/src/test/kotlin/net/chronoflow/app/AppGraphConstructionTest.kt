package net.chronoflow.app

import android.app.Application
import android.os.StrictMode
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class AppGraphConstructionTest {
    @Test fun constructionDoesNotResolveAnyStorageDirectory() =
        runTest(timeout = 60.seconds) {
            var accesses = 0
            val guarded =
                object : Application() {
                    override fun getNoBackupFilesDir(): File {
                        accesses++
                        error("storage resolved during graph construction")
                    }

                    override fun getFilesDir(): File {
                        accesses++
                        error("files resolved during graph construction")
                    }
                }
            val dispatcher = StandardTestDispatcher(testScheduler)
            val old = StrictMode.getThreadPolicy()
            val graph: AppGraph
            try {
                StrictMode.setThreadPolicy(
                    StrictMode.ThreadPolicy
                        .Builder()
                        .detectDiskReads()
                        .detectDiskWrites()
                        .penaltyDeath()
                        .build(),
                )
                graph =
                    AppGraph(
                        guarded,
                        overrides =
                            GraphOverrides(
                                sessionCipher = SoftwareSessionCipher(),
                                dispatchers = AppDispatchers(dispatcher, dispatcher, dispatcher),
                            ),
                    )
            } finally {
                StrictMode.setThreadPolicy(old)
            }
            try {
                assertEquals(0, accesses)
            } finally {
                graph.closeAndJoinForTest()
            }
        }
}
