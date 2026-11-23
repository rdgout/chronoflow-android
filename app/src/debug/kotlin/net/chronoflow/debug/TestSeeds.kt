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
import net.chronoflow.app.AppDispatchers
import net.chronoflow.app.ChronoFlowApp
import net.chronoflow.app.GraphOverrides
import net.chronoflow.app.MainActivity
import net.chronoflow.app.MockSeedFlags
import net.chronoflow.app.perf.PerfMockSources
import net.chronoflow.core.database.AndroidAccountDriverFactory
import net.chronoflow.core.datastore.AndroidKeystoreSessionCipher
import net.chronoflow.core.datastore.DevicePreferences
import net.chronoflow.core.datastore.KeystoreSessionStore
import net.chronoflow.core.datastore.SessionCipher
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.config.LanguageChoice
import net.chronoflow.core.model.config.ThemeChoice
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.model.session.readStoredSession
import java.io.File
import kotlin.time.Duration

object TestSeeds {
    @SuppressLint("NewApi")
    suspend fun apply(
        context: Context,
        seed: TestSeed,
        io: CoroutineDispatcher,
        sdkInt: Int = Build.VERSION.SDK_INT,
        sessionCipher: SessionCipher? = null,
        dispatchers: AppDispatchers? = null,
    ) = withContext(io) {
        DebugShellStart.reset()
        DebugRefreshProbe.reset()
        net.chronoflow.feature.settings.SettingsDebugCounters.languageSaved
            .set(0)
        UITestTimeline.record("seed-begin", seed)
        if (seed.testId != null) {
            val app = context.applicationContext as ChronoFlowApp
            val dir = File(context.filesDir, "uitests/${seed.testId}")
            val alias = "ChronoFlowMock.uitest.${seed.testId}"
            val cipher = sessionCipher ?: AndroidKeystoreSessionCipher()
            val overrides =
                GraphOverrides(
                    dir,
                    seed.mockDelay,
                    alias,
                    MockSeedFlags(seed.unverified, seed.walkthroughSeen, seed.termsPending),
                    cipher,
                    dispatchers,
                    mutationsOffline = seed.mutationsOffline,
                )
            DebugAppHooks.resetGraph(app, overrides)
            val credentials = KeystoreSessionStore(File(dir, "ChronoFlowMock/session.bin"), alias, cipher, io)
            val source = sourceForSeed(dir, overrides, io) { credentials.readStoredSession()?.token }
            SeedRunner(
                dir,
                credentials,
                AndroidAccountDriverFactory(context),
                source,
                io,
                beforeSignIn = { SettingsBoardCaptureFixture.prepare(dir, seed, app.graph.planClock.now, io) },
            ).reseed(seed.signedIn, seed.testId, seed.termsChangesPending, seed.settingsPending)
            seed.authTestLink?.let { link ->
                withContext(app.graph.dispatchers.main) { app.graph.pendingLaunchIntents.captureLink(link) }
            }
            DebugSeedRouting.current = seed
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

    internal fun sourceForSeed(
        root: File,
        overrides: GraphOverrides,
        io: CoroutineDispatcher,
        bearer: suspend () -> String?,
    ): DataSource = PerfMockSources(io).dataSource(root, overrides.copy(mockDelay = Duration.ZERO), bearer)

    /** An explicit intent that restarts [MainActivity], so attachBaseContext applies the seeded configuration. */
    fun forwardIntent(
        context: Context,
        seed: TestSeed,
    ): Intent =
        Intent(context, MainActivity::class.java).apply {
            seed.raw.filterKeys { it != "CF_AUTH_TEST_LINK" }.forEach { (key, value) -> putExtra(key, value) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
}
