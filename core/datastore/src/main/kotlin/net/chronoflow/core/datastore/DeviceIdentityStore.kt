package net.chronoflow.core.datastore

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.device.DeviceIdentityPersistence
import net.chronoflow.core.model.device.UUIDv7
import java.security.SecureRandom
import java.text.BreakIterator
import java.util.Locale
import kotlin.math.floor

class DeviceIdentityStore(
    private val persistence: DeviceIdentityPersistence,
    private val clock: () -> Double = { System.currentTimeMillis() / 1000.0 },
    private val randomBytes: () -> ByteArray = { ByteArray(10).also(SecureRandom()::nextBytes) },
) {
    @Volatile private var cached: String? = null
    private val mutex = Mutex()

    suspend fun identity(): String {
        currentCoroutineContext().ensureActive()
        cached?.let { return it }
        return mutex.withLock { cached ?: load().also { cached = it } }
    }

    private suspend fun load(): String {
        persistence.readDeviceID()?.let { value ->
            if (value.length != 36 || !uuid.matches(value) || value[14] != '7' || value[19] !in "89abAB") {
                throw DeviceIdentityFailure.InvalidPersistedIdentity
            }
            return value.lowercase()
        }
        val ms = floor(clock() * 1000)
        if (!ms.isFinite() || ms < 0 || ms >= (1L shl 48).toDouble()) throw DeviceIdentityFailure.InvalidTimestamp
        val value = UUIDv7.generate(ms.toLong(), randomBytes()).toString()
        persistence.writeDeviceID(value)
        return value
    }

    companion object {
        private val uuid = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

        fun deviceName(
            name: String,
            model: String,
        ): String {
            val value = name.trim().ifEmpty { model.trim() }
            val iterator = BreakIterator.getCharacterInstance(Locale.ROOT).apply { setText(value) }
            var end = 0
            var count = 0
            var next = iterator.next()
            while (next != BreakIterator.DONE) {
                count += value.codePointCount(end, next)
                if (count > 255) break
                end = next
                next = iterator.next()
            }
            return value.substring(0, end)
        }
    }
}
