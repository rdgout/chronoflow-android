package net.chronoflow.core.model.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionValueTypesTest {
    private fun noMessage(failure: SessionFailure) = assertNull(failure.message)

    @Test fun sessionFailureCarriesNoMessageCredentialsUnavailable() = noMessage(SessionFailure.CredentialsUnavailable)

    @Test fun sessionFailureCarriesNoMessageDatabaseUnavailable() = noMessage(SessionFailure.DatabaseUnavailable)

    @Test fun sessionFailureCarriesNoMessageCredentialWrite() = noMessage(SessionFailure.CredentialWrite)

    @Test fun sessionFailureCarriesNoMessageInvalidResult() = noMessage(SessionFailure.InvalidResult)

    @Test fun sessionFailureCarriesNoMessageSuperseded() = noMessage(SessionFailure.Superseded)

    @Test fun snapshotToStringRedactsTheToken() {
        val snapshot = SessionSnapshot(StoredSession("s3cr3t-value", "019a562b-8000-7000-8000-000000000001"), 1)
        assertFalse(snapshot.toString().contains("s3cr3t-value"))
    }

    @Test fun needsConfirmationKeepsTheCount() {
        assertEquals(SignOutResult.NeedsConfirmation(2), SignOutResult.NeedsConfirmation(2))
        assertNotEquals(SignOutResult.NeedsConfirmation(2), SignOutResult.NeedsConfirmation(3))
    }
}
