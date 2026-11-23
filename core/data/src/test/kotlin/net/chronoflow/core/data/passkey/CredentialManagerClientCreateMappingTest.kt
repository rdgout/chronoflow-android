package net.chronoflow.core.data.passkey

import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialCustomException
import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.CreateCredentialNoCreateOptionException
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.CreateCredentialUnsupportedException
import androidx.credentials.exceptions.domerrors.InvalidStateError
import androidx.credentials.exceptions.domerrors.NotAllowedError
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import net.chronoflow.core.model.passkey.PasskeyFailure
import org.junit.Assert.assertSame
import org.junit.Test

class CredentialManagerClientCreateMappingTest {
    @Test fun cancellationIsCanceled() {
        assertSame(PasskeyFailure.Canceled, CreateCredentialCancellationException().toPasskeyFailure())
    }

    @Test fun invalidStateDomErrorIsAlreadyRegistered() {
        assertSame(
            PasskeyFailure.AlreadyRegistered,
            CreatePublicKeyCredentialDomException(InvalidStateError()).toPasskeyFailure(),
        )
    }

    @Test fun otherDomErrorIsUnavailable() {
        assertSame(
            PasskeyFailure.Unavailable,
            CreatePublicKeyCredentialDomException(NotAllowedError()).toPasskeyFailure(),
        )
    }

    @Test fun interruptedIsUnavailable() {
        assertSame(PasskeyFailure.Unavailable, CreateCredentialInterruptedException().toPasskeyFailure())
    }

    @Test fun noCreateOptionIsUnavailable() {
        assertSame(PasskeyFailure.Unavailable, CreateCredentialNoCreateOptionException().toPasskeyFailure())
    }

    @Test fun providerConfigurationIsUnavailable() {
        assertSame(PasskeyFailure.Unavailable, CreateCredentialProviderConfigurationException().toPasskeyFailure())
    }

    @Test fun unsupportedIsUnavailable() {
        assertSame(PasskeyFailure.Unavailable, CreateCredentialUnsupportedException().toPasskeyFailure())
    }

    @Test fun unknownIsUnavailable() {
        assertSame(PasskeyFailure.Unavailable, CreateCredentialUnknownException().toPasskeyFailure())
    }

    @Test fun futureCredentialFailureIsUnavailable() {
        assertSame(PasskeyFailure.Unavailable, CreateCredentialCustomException("future").toPasskeyFailure())
    }
}
