package net.chronoflow.core.datastore

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.config.LanguageChoice
import net.chronoflow.core.model.config.ThemeChoice
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.device.DeviceIdentityPersistence
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.WeakHashMap

class DevicePreferences(
    context: Context,
    private val dataStore: DataStore<Preferences>,
    private val io: CoroutineDispatcher,
) : DeviceIdentityPersistence {
    private val application = WeakReference(context.applicationContext)

    private fun applicationContext(): Context = checkNotNull(application.get())

    private val data = dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
    val language: Flow<LanguageChoice> =
        data
            .map {
                LanguageChoice.fromRaw(it[languageKey]) ?: LanguageChoice.System
            }.distinctUntilChanged()
    val theme: Flow<ThemeChoice> =
        data
            .map {
                ThemeChoice.fromRaw(it[themeKey]) ?: ThemeChoice.System
            }.distinctUntilChanged()
    val notificationPermissionRequested: Flow<Boolean> = data.map { it[permissionKey] ?: false }.distinctUntilChanged()

    suspend fun setLanguage(
        choice: LanguageChoice,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ) = withContext(io) {
        dataStore.edit { it[languageKey] = choice.raw }
        if (sdkInt < 33) DeviceConfigFile.update(applicationContext(), io) { it.copy(language = choice) }
    }

    suspend fun setTheme(
        choice: ThemeChoice,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ) = withContext(io) {
        dataStore.edit { it[themeKey] = choice.raw }
        if (sdkInt < 31) DeviceConfigFile.update(applicationContext(), io) { it.copy(theme = choice) }
    }

    suspend fun setNotificationPermissionRequested(value: Boolean) =
        withContext(io) {
            dataStore.edit { it[permissionKey] = value }
            Unit
        }

    suspend fun reconcileDeviceConfig(sdkInt: Int = Build.VERSION.SDK_INT) =
        withContext(io) {
            if (sdkInt < 33) {
                val prefs = data.first()
                DeviceConfigFile.update(applicationContext(), io) { old ->
                    old.copy(
                        language = LanguageChoice.fromRaw(prefs[languageKey]) ?: LanguageChoice.System,
                        theme =
                            if (sdkInt < 31) {
                                ThemeChoice.fromRaw(prefs[themeKey]) ?: ThemeChoice.System
                            } else {
                                old.theme
                            },
                    )
                }
            }
        }

    override suspend fun readDeviceID(): String? =
        withContext(io) {
            val value =
                dataStore.data
                    .first()
                    .asMap()
                    .entries
                    .firstOrNull { it.key.name == "api.deviceId" }
                    ?.value
            when (value) {
                null -> null
                is String -> value
                else -> throw DeviceIdentityFailure.InvalidPersistedIdentity
            }
        }

    override suspend fun writeDeviceID(value: String) =
        withContext(io) {
            dataStore.edit { it[deviceKey] = value }
            Unit
        }

    companion object {
        const val FILE_NAME = "device_prefs.preferences_pb"

        fun file(root: File): File = File(root, FILE_NAME)

        private val deviceKey = stringPreferencesKey("api.deviceId")
        private val languageKey = stringPreferencesKey("cf.settings.language")
        private val themeKey = stringPreferencesKey("cf.settings.theme")
        private val permissionKey = booleanPreferencesKey("notificationPermissionRequested")
        private val instances = WeakHashMap<Context, DevicePreferences>()

        fun shared(context: Context): DevicePreferences {
            val app = context.applicationContext
            return synchronized(instances) {
                instances.getOrPut(app) {
                    val application = WeakReference(app)
                    val store =
                        PreferenceDataStoreFactory.create(
                            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                            produceFile = { file(checkNotNull(application.get()).noBackupFilesDir) },
                        )
                    DevicePreferences(app, store, Dispatchers.IO)
                }
            }
        }
    }
}
