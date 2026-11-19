package net.chronoflow.core.datastore

interface SessionCipher {
    fun encrypt(
        alias: String,
        plaintext: ByteArray,
    ): Sealed

    fun decrypt(
        alias: String,
        sealed: Sealed,
    ): ByteArray

    fun deleteKey(alias: String)

    class Sealed(
        val iv: ByteArray,
        val ciphertext: ByteArray,
    )
}

class PermanentSessionKeyFailure(
    cause: Throwable? = null,
) : Exception(null, cause)
