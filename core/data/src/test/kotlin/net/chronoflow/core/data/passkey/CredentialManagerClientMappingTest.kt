package net.chronoflow.core.data.passkey

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.domerrors.NotAllowedError
import androidx.credentials.exceptions.publickeycredential.GetPublicKeyCredentialDomException
import net.chronoflow.core.model.passkey.PasskeyFailure
import org.junit.Assert.assertSame
import org.junit.Test

class CredentialManagerClientMappingTest {
    @Test fun userCancellationIsSilent() {
        assertSame(PasskeyFailure.Canceled, GetCredentialCancellationException().toPasskeyFailure())
    }

    @Test fun noCredentialIsSilent() {
        assertSame(PasskeyFailure.Canceled, NoCredentialException().toPasskeyFailure())
    }

    @Test fun domFailureIsUnavailable() {
        assertSame(PasskeyFailure.Unavailable, GetPublicKeyCredentialDomException(NotAllowedError()).toPasskeyFailure())
    }

    @Test fun interruptionIsUnavailable() {
        assertSame(PasskeyFailure.Unavailable, GetCredentialInterruptedException().toPasskeyFailure())
    }

    @Test fun providerConfigurationIsUnavailable() {
        assertSame(PasskeyFailure.Unavailable, GetCredentialProviderConfigurationException().toPasskeyFailure())
    }

    @Test fun unsupportedIsUnavailable() {
        assertSame(PasskeyFailure.Unavailable, GetCredentialUnsupportedException().toPasskeyFailure())
    }

    @Test fun unknownIsUnavailable() {
        assertSame(PasskeyFailure.Unavailable, GetCredentialUnknownException().toPasskeyFailure())
    }

    @Test fun futureCredentialFailureIsUnavailable() {
        assertSame(PasskeyFailure.Unavailable, GetCredentialCustomException("future").toPasskeyFailure())
    }
}
