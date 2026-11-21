package net.chronoflow.app.settings

import android.app.LocaleManager
import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.chronoflow.core.datastore.DevicePreferences
import net.chronoflow.core.model.config.ConfigurationRules
import net.chronoflow.core.model.config.LanguageChoice
import net.chronoflow.feature.settings.language.LanguageDependencies
import java.util.Locale

class LanguageFeedback {
    val pending = MutableStateFlow<LanguageChoice?>(null)
}

class AppLanguageApplier(
    context: Context,
    private val preferences: DevicePreferences,
    private val io: CoroutineDispatcher,
    feedback: LanguageFeedback,
) : LanguageDependencies {
    private val application = context.applicationContext
    private val mutex = Mutex()

    @Volatile private var phone = "en"
    override val feedback = feedback.pending
    override val current =
        callbackFlow {
            val callbacks =
                object : ComponentCallbacks {
                    override fun onConfigurationChanged(newConfig: Configuration) {
                        trySend(Unit)
                    }

                    @Suppress("OVERRIDE_DEPRECATION")
                    override fun onLowMemory() = Unit
                }
            application.registerComponentCallbacks(callbacks)
            val subscription = launch { preferences.language.collect { trySend(Unit) } }
            trySend(Unit)
            awaitClose {
                subscription.cancel()
                application.unregisterComponentCallbacks(callbacks)
            }
        }.map {
            mutex.withLock {
                val stored = preferences.language.first()
                val choice =
                    if (Build.VERSION.SDK_INT >= 33) {
                        val manager = checkNotNull(application.getSystemService(LocaleManager::class.java))
                        val locales = manager.applicationLocales
                        if (locales.isEmpty) {
                            LanguageChoice.System
                        } else {
                            LanguageChoice.fromRaw(locales[0].language) ?: LanguageChoice.System
                        }
                    } else {
                        stored
                    }
                phone = readPhoneLanguage()
                if (stored != choice) preferences.setLanguage(choice, Build.VERSION.SDK_INT)
                choice
            }
        }.flowOn(io)

    override fun phoneLanguage(): String = phone

    override suspend fun apply(choice: LanguageChoice): Boolean =
        withContext(io) {
            mutex.withLock {
                preferences.setLanguage(choice, Build.VERSION.SDK_INT)
                if (Build.VERSION.SDK_INT >= 33) {
                    checkNotNull(application.getSystemService(LocaleManager::class.java)).applicationLocales =
                        if (choice == LanguageChoice.System) {
                            LocaleList.getEmptyLocaleList()
                        } else {
                            LocaleList.forLanguageTags(choice.raw)
                        }
                }
                Build.VERSION.SDK_INT < 33
            }
        }

    private fun readPhoneLanguage(): String {
        val locales =
            if (Build.VERSION.SDK_INT >= 33) {
                checkNotNull(application.getSystemService(LocaleManager::class.java)).systemLocales
            } else {
                Resources.getSystem().configuration.locales
            }
        val locale = if (locales.isEmpty) Locale.ENGLISH else locales[0]
        return ConfigurationRules.resolveLanguage(LanguageChoice.System, locale).tag
    }
}
