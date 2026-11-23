package net.chronoflow.core.datastore

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.api.ApiJson
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

class CorruptWipeMarkerException : Exception(null, null)

class WipeMarker(
    private val namespaceRoot: File,
    private val io: CoroutineDispatcher,
) {
    val file: File = File(namespaceRoot, "pending-wipe.json")

    suspend fun write(id: AccountID) =
        withContext(io) {
            namespaceRoot.mkdirs()
            val tmp = File(namespaceRoot, "pending-wipe.json.tmp")
            FileOutputStream(tmp).use { output ->
                output.write(ApiJson.response.encodeToString(String.serializer(), id.rawValue).encodeToByteArray())
                output.fd.sync()
            }
            Files.move(tmp.toPath(), file.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
            Unit
        }

    suspend fun read(): AccountID? =
        withContext(io) {
            val text =
                try {
                    Files.readAllBytes(file.toPath()).decodeToString()
                } catch (
                    _: NoSuchFileException,
                ) {
                    return@withContext null
                }
            try {
                val element =
                    ApiJson.response.parseToJsonElement(text) as? JsonPrimitive ?: throw CorruptWipeMarkerException()
                if (!element.isString) throw CorruptWipeMarkerException()
                AccountID.of(element.content)
            } catch (
                _: SerializationException,
            ) {
                throw CorruptWipeMarkerException()
            } catch (_: AccountID.InvalidUUID) {
                throw CorruptWipeMarkerException()
            }
        }

    suspend fun clear() =
        withContext(io) {
            Files.deleteIfExists(file.toPath())
            Unit
        }
}
