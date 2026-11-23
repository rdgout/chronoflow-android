package net.chronoflow.app

import android.os.StrictMode
import androidx.test.platform.app.InstrumentationRegistry
import net.chronoflow.core.datastore.DeviceConfigFile
import net.chronoflow.core.model.config.DeviceConfig
import net.chronoflow.core.model.config.LanguageChoice
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

@Smoke
class DeviceConfigStrictModeTest {
    @Test
    fun launchReadSurvivesAStrictDiskPolicyOnTheMainThread() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val file = File(context.noBackupFilesDir, DeviceConfigFile.FILE_NAME)
        file.writeBytes("language=nl\n".toByteArray())
        var result: DeviceConfig? = null
        try {
            instrumentation.runOnMainSync {
                val previous = StrictMode.getThreadPolicy()
                StrictMode.setThreadPolicy(
                    StrictMode.ThreadPolicy
                        .Builder()
                        .detectDiskReads()
                        .detectDiskWrites()
                        .penaltyDeath()
                        .build(),
                )
                try {
                    result = DeviceConfigFile.readForLaunch(context, sdkInt = 29)
                } finally {
                    StrictMode.setThreadPolicy(previous)
                }
            }
        } finally {
            file.delete()
        }
        assertEquals(LanguageChoice.Nl, result?.language)
    }
}
