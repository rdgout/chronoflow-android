package net.chronoflow.core.datastore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidKeystoreSessionCipher : SessionCipher {
    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    override fun encrypt(
        alias: String,
        plaintext: ByteArray,
    ): SessionCipher.Sealed = permanentFailures { encryptOnce(alias, plaintext) }

    private fun encryptOnce(
        alias: String,
        plaintext: ByteArray,
    ): SessionCipher.Sealed {
        val key = keyStore().getKey(alias, null) as SecretKey? ?: generateKey(alias)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(alias.encodeToByteArray())
        check(cipher.iv.size == 12)
        return SessionCipher.Sealed(cipher.iv, cipher.doFinal(plaintext))
    }

    override fun decrypt(
        alias: String,
        sealed: SessionCipher.Sealed,
    ): ByteArray =
        permanentFailures {
            val key = keyStore().getKey(alias, null) as SecretKey? ?: throw PermanentSessionKeyFailure()
            if (sealed.iv.size != 12) throw PermanentSessionKeyFailure()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, sealed.iv))
            cipher.updateAAD(alias.encodeToByteArray())
            cipher.doFinal(sealed.ciphertext)
        }

    override fun deleteKey(alias: String) {
        keyStore().deleteEntry(alias)
    }

    private fun generateKey(alias: String): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec
                .Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private inline fun <T> permanentFailures(operation: () -> T): T =
        try {
            operation()
        } catch (e: KeyPermanentlyInvalidatedException) {
            throw PermanentSessionKeyFailure(e)
        } catch (e: UnrecoverableKeyException) {
            throw PermanentSessionKeyFailure(e)
        } catch (e: BadPaddingException) {
            throw PermanentSessionKeyFailure(e)
        }
}
