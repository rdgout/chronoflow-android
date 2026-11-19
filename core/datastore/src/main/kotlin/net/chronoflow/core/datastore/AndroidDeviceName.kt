package net.chronoflow.core.datastore

import android.content.Context
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

suspend fun androidDeviceName(
    context: Context,
    io: CoroutineDispatcher,
): String =
    withContext(io) {
        DeviceIdentityStore.deviceName(
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME) ?: "",
            Build.MODEL ?: "",
        )
    }
