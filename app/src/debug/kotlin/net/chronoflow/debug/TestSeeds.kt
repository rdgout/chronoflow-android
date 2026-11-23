package net.chronoflow.debug

import android.annotation.SuppressLint
import android.app.LocaleManager
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.LocaleList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.chronoflow.app.MainActivity
import net.chronoflow.core.datastore.DevicePreferences
import net.chronoflow.core.model.config.LanguageChoice
import net.chronoflow.core.model.config.ThemeChoice
import java.io.File

object TestSeeds {
    @SuppressLint("NewApi")
    suspend fun apply(
        context: Context,
        seed: TestSeed,
        io: CoroutineDispatcher,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ) = withContext(io) {
        UITestTimeline.record("seed-begin", seed)
        if (seed.testId != null) {
            val dir = File(context.filesDir, "uitests/${seed.testId}")
            dir.deleteRecursively()
            dir.mkdirs()
        }
        seed.language?.let { language ->
            DevicePreferences.shared(context).setLanguage(language, sdkInt)
            if (sdkInt >= 33) {
                val tags = if (language == LanguageChoice.System) "" else language.raw
                context.getSystemService(LocaleManager::class.java).applicationLocales =
                    LocaleList.forLanguageTags(tags)
            }
        }
        seed.theme?.let { theme ->
            DevicePreferences.shared(context).setTheme(theme, sdkInt)
            if (sdkInt >= 31) {
                val mode =
                    when (theme) {
                        ThemeChoice.System -> UiModeManager.MODE_NIGHT_AUTO
                        ThemeChoice.Light -> UiModeManager.MODE_NIGHT_NO
                        ThemeChoice.Dark -> UiModeManager.MODE_NIGHT_YES
                    }
                context.getSystemService(UiModeManager::class.java).setApplicationNightMode(mode)
            }
        }
        UITestTimeline.record("seed-applied", seed, "keys=${seed.raw.keys.sorted().joinToString(",")}")
        if (seed.ignoredKeys.isNotEmpty()) {
            UITestTimeline.record("seed-ignored", seed, "keys=${seed.ignoredKeys.sorted().joinToString(",")}")
        }
    }

    /** An explicit intent that restarts [MainActivity], so attachBaseContext applies the seeded configuration. */
    fun forwardIntent(
        context: Context,
        seed: TestSeed,
    ): Intent =
        Intent(context, MainActivity::class.java).apply {
            seed.raw.forEach { (key, value) -> putExtra(key, value) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
}
