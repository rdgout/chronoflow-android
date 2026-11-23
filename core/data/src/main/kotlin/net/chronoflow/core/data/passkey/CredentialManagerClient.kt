package net.chronoflow.core.data.passkey

import android.content.Context
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.domerrors.InvalidStateError
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import net.chronoflow.core.model.passkey.CredentialClient
import net.chronoflow.core.model.passkey.PasskeyFailure
import java.util.concurrent.atomic.AtomicBoolean

class CredentialManagerClient(
    appContext: Context,
    private val anchor: PasskeyAnchor,
) : CredentialClient {
    private val credentialManager by lazy { CredentialManager.create(appContext.applicationContext) }
    private val pending = AtomicBoolean(false)

    override suspend fun createPasskey(requestJson: String): String {
        val activity = anchor.current() ?: throw PasskeyFailure.Unavailable
        if (!pending.compareAndSet(false, true)) throw PasskeyFailure.Busy
        try {
            val response = credentialManager.createCredential(activity, CreatePublicKeyCredentialRequest(requestJson))
            val credential = response as? CreatePublicKeyCredentialResponse ?: throw PasskeyFailure.Unavailable
            return credential.registrationResponseJson
        } catch (failure: CreateCredentialException) {
            throw failure.toPasskeyFailure()
        } finally {
            pending.set(false)
        }
    }

    override suspend fun getPasskey(requestJson: String): String {
        val activity = anchor.current() ?: throw PasskeyFailure.Unavailable
        if (!pending.compareAndSet(false, true)) throw PasskeyFailure.Busy
        try {
            val request =
                GetCredentialRequest(
                    credentialOptions = listOf(GetPublicKeyCredentialOption(requestJson)),
                    preferImmediatelyAvailableCredentials = false,
                )
            val response = credentialManager.getCredential(activity, request)
            val credential = response.credential as? PublicKeyCredential ?: throw PasskeyFailure.Unavailable
            return credential.authenticationResponseJson
        } catch (failure: GetCredentialException) {
            throw failure.toPasskeyFailure()
        } finally {
            pending.set(false)
        }
    }
}

internal fun CreateCredentialException.toPasskeyFailure(): PasskeyFailure =
    when {
        this is CreateCredentialCancellationException -> {
            PasskeyFailure.Canceled
        }

        this is CreatePublicKeyCredentialDomException && domError is InvalidStateError -> {
            PasskeyFailure.AlreadyRegistered
        }

        else -> {
            PasskeyFailure.Unavailable
        }
    }

internal fun GetCredentialException.toPasskeyFailure(): PasskeyFailure =
    when (this) {
        is GetCredentialCancellationException, is NoCredentialException -> PasskeyFailure.Canceled
        else -> PasskeyFailure.Unavailable
    }
