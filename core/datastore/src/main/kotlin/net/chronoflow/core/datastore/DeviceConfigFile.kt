package net.chronoflow.core.datastore

import android.content.Context
import android.os.Build
import android.os.StrictMode
import android.util.AtomicFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.config.DeviceConfig
import java.io.File
import java.io.IOException

/** The 64-byte `device-config` file in `noBackupFilesDir`, read once before the first frame below API 33. */
object DeviceConfigFile {
    const val FILE_NAME = "device-config"

    private val mutex = Mutex()

    private const val READ_LIMIT = DeviceConfig.MAX_BYTES + 1

    fun readForLaunch(
        context: Context,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): DeviceConfig {
        if (sdkInt >= 33) return DeviceConfig()
        // The launch path may read this one tiny file on the main thread; allowing writes also covers the mkdir.
        val previous = StrictMode.allowThreadDiskWrites()
        try {
            val bytes =
                try {
                    File(context.noBackupFilesDir, FILE_NAME).inputStream().use { input ->
                        val buffer = ByteArray(READ_LIMIT)
                        var length = 0
                        while (length < READ_LIMIT) {
                            val read = input.read(buffer, length, READ_LIMIT - length)
                            if (read < 0) break
                            length += read
                        }
                        buffer.copyOf(length)
                    }
                } catch (_: IOException) {
                    return DeviceConfig()
                }
            return DeviceConfig.decode(bytes)
        } finally {
            StrictMode.setThreadPolicy(previous)
        }
    }

    suspend fun write(
        context: Context,
        config: DeviceConfig,
        io: CoroutineDispatcher,
    ) = withContext(io) {
        mutex.withLock { writeFile(context, config) }
    }

    suspend fun update(
        context: Context,
        io: CoroutineDispatcher,
        transform: (DeviceConfig) -> DeviceConfig,
    ) = withContext(io) {
        mutex.withLock { writeFile(context, transform(readOnIo(context))) }
    }

    private fun readOnIo(context: Context): DeviceConfig {
        val bytes =
            try {
                File(context.noBackupFilesDir, FILE_NAME).inputStream().use { input ->
                    val buffer = ByteArray(READ_LIMIT)
                    var length = 0
                    while (length < READ_LIMIT) {
                        val read = input.read(buffer, length, READ_LIMIT - length)
                        if (read < 0) break
                        length += read
                    }
                    buffer.copyOf(length)
                }
            } catch (_: IOException) {
                null
            }
        return DeviceConfig.decode(bytes)
    }

    private fun writeFile(
        context: Context,
        config: DeviceConfig,
    ) {
        val file = AtomicFile(File(context.noBackupFilesDir, FILE_NAME))
        val out = file.startWrite()
        try {
            out.write(config.encode())
            file.finishWrite(out)
        } catch (e: IOException) {
            file.failWrite(out)
            throw e
        }
    }
}
