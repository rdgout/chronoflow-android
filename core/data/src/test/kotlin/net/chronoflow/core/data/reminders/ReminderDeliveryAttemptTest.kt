package net.chronoflow.core.data.reminders

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.datastore.PermanentSessionKeyFailure
import net.chronoflow.core.model.store.StoreFailure
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStoreException
import java.security.ProviderException
import kotlin.time.Duration.Companion.seconds

class ReminderDeliveryAttemptTest {
    @Test fun ioFailureDoesNotEscapeTheAlarm() = drops(IOException())

    @Test fun keyStoreFailureDoesNotEscapeTheAlarm() = drops(KeyStoreException())

    @Test fun securityFailureDoesNotEscapeTheAlarm() = drops(GeneralSecurityException())

    @Test fun providerFailureDoesNotEscapeTheAlarm() = drops(ProviderException())

    @Test fun permissionFailureDoesNotEscapeTheAlarm() = drops(SecurityException())

    @Test fun ledgerFailureDoesNotEscapeTheAlarm() = drops(StoreFailure.Unavailable())

    @Test fun invalidSessionKeyDoesNotEscapeTheAlarm() = drops(PermanentSessionKeyFailure())

    @Test fun cancellationEscapesTheAlarm() =
        runTest(timeout = 60.seconds) {
            val cancelled = CancellationException()
            try {
                attemptReminderDelivery { throw cancelled }
                fail("alarm swallowed cancellation")
            } catch (actual: CancellationException) {
                assertSame(cancelled, actual)
            }
        }

    private fun drops(error: Throwable) =
        runTest(timeout = 60.seconds) {
            attemptReminderDelivery { throw error }
        }
}
