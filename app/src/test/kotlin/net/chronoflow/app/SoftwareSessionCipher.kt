package net.chronoflow.app

import net.chronoflow.core.datastore.PermanentSessionKeyFailure
import net.chronoflow.core.datastore.SessionCipher
import java.security.SecureRandom
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class SoftwareSessionCipher : SessionCipher {
    val keys = mutableMapOf<String, SecretKey>()
    var failNext: Throwable? = null

    private fun failIfRequested() {
        val failure = failNext
        failNext = null
        if (failure != null) throw failure
    }

    override fun encrypt(
        alias: String,
        plaintext: ByteArray,
    ): SessionCipher.Sealed {
        failIfRequested()
        val key = keys.getOrPut(alias) { KeyGenerator.getInstance("AES").apply { init(256) }.generateKey() }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(alias.encodeToByteArray())
        return SessionCipher.Sealed(cipher.iv, cipher.doFinal(plaintext))
    }

    override fun decrypt(
        alias: String,
        sealed: SessionCipher.Sealed,
    ): ByteArray {
        failIfRequested()
        val key = keys[alias] ?: throw PermanentSessionKeyFailure()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, sealed.iv))
        cipher.updateAAD(alias.encodeToByteArray())
        return try {
            cipher.doFinal(sealed.ciphertext)
        } catch (
            e: BadPaddingException,
        ) {
            throw PermanentSessionKeyFailure(e)
        }
    }

    override fun deleteKey(alias: String) {
        keys.remove(alias)
    }
}
