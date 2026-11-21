package net.chronoflow.core.datastore

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.config.ThemeChoice

fun interface NightModeSink {
    fun set(mode: Int)
}

class UiModeNightModeSink(
    context: Context,
) : NightModeSink {
    private val application = context.applicationContext

    @RequiresApi(31)
    override fun set(mode: Int) {
        application.getSystemService(UiModeManager::class.java).setApplicationNightMode(mode)
    }
}

class DeviceAppearance(
    private val prefs: DevicePreferences,
    private val sink: NightModeSink,
    private val io: CoroutineDispatcher,
    private val sdkInt: Int = Build.VERSION.SDK_INT,
) {
    val theme = prefs.theme

    suspend fun setTheme(choice: ThemeChoice) {
        prefs.setTheme(choice, sdkInt)
        if (sdkInt >= 31) {
            withContext(io) {
                sink.set(
                    when (choice) {
                        ThemeChoice.System -> UiModeManager.MODE_NIGHT_AUTO
                        ThemeChoice.Light -> UiModeManager.MODE_NIGHT_NO
                        ThemeChoice.Dark -> UiModeManager.MODE_NIGHT_YES
                    },
                )
            }
        }
    }
}
