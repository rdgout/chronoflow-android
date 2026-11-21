package net.chronoflow.core.model.passkey

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class DeterministicRegistrationProviderTest {
    @Test fun defaultBuildsTheIOSAttestation() =
        runTest(timeout = 60.seconds) {
            val actual = DeterministicRegistrationProvider().registration(RegistrationFixture.challenge)
            assertEquals(
                Base64URL.attestation(
                    "credential:Y2g".encodeToByteArray(),
                    "client-data:Y2g".encodeToByteArray(),
                    "attestation:Y2g".encodeToByteArray(),
                    listOf("internal"),
                ),
                actual,
            )
        }
}
