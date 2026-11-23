package net.chronoflow.debug

import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.ContextWrapper
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowActivityManager.ApplicationExitInfoBuilder
import org.robolectric.shadows.ShadowLog

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class ExitInfoLoggerTest {
    @Before fun clearLogs() = ShadowLog.clear()

    @Test fun previousCrashIsLoggedOnce() {
        val app = RuntimeEnvironment.getApplication()
        val manager = requireNotNull(app.getSystemService(ActivityManager::class.java))
        shadowOf(manager).addApplicationExitInfo(
            ApplicationExitInfoBuilder
                .newBuilder()
                .setProcessName(app.packageName)
                .setPid(123)
                .setReason(ApplicationExitInfo.REASON_CRASH)
                .setStatus(1)
                .build(),
        )
        ExitInfoLogger.logPrevious(app)
        val logs = ShadowLog.getLogsForTag("CfExit")
        assertEquals(1, logs.size)
        assertTrue(logs.single().msg.contains("reason=4"))
    }

    @Test fun noPreviousExitProducesNoLog() {
        ExitInfoLogger.logPrevious(RuntimeEnvironment.getApplication())
        assertTrue(ShadowLog.getLogsForTag("CfExit").isEmpty())
    }

    @Test
    @Config(sdk = [29])
    fun api29DoesNotReadActivityManager() {
        val context =
            object : ContextWrapper(RuntimeEnvironment.getApplication()) {
                override fun getSystemService(name: String): Any = error("API 29 must not read a service")
            }
        ExitInfoLogger.logPrevious(context)
        assertTrue(ShadowLog.getLogsForTag("CfExit").isEmpty())
    }
}
