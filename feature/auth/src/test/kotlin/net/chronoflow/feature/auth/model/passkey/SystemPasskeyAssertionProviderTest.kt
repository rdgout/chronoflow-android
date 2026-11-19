package net.chronoflow.feature.auth.model.passkey

import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.datamock.MockMethod
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiPasskeyChallenge
import net.chronoflow.core.model.api.ApiPasskeyLoginInput
import net.chronoflow.core.model.api.UserVerification
import net.chronoflow.core.model.passkey.Base64URL
import net.chronoflow.core.model.passkey.CredentialClient
import net.chronoflow.core.model.passkey.PasskeyFailure
import net.chronoflow.core.model.passkey.PasskeyJson
import net.chronoflow.core.navigation.Route
import net.chronoflow.feature.auth.model.AuthBannerKind
import net.chronoflow.feature.auth.model.AuthNavigation
import net.chronoflow.feature.auth.testing.AUTH_DEVICE_ID
import net.chronoflow.feature.auth.testing.FakeSecretField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SystemPasskeyAssertionProviderTest {
    @Test fun requestIsTheOptionsJsonAndResponseIsParsed() =
        runTest(timeout = 60.seconds) {
            var request: String? = null
            val expected = testAssertion()
            val client =
                CredentialClient {
                    request = it
                    net.chronoflow.core.model.api.ApiJson.request.encodeToString(
                        net.chronoflow.core.model.api.ApiPasskeyAssertion
                            .serializer(),
                        expected,
                    )
                }
            val challenge = fixtureChallenge()
            assertEquals(expected, SystemPasskeyAssertionProvider(client).assertion(challenge))
            assertEquals(PasskeyJson.requestJson(challenge.options), request)
        }

    @Test fun cancelIsSilentThroughSignIn() =
        runTest(timeout = 60.seconds) {
            val f =
                PasskeyTestFixture(
                    StandardTestDispatcher(testScheduler),
                    SystemPasskeyAssertionProvider(CredentialClient { throw PasskeyFailure.Canceled }),
                )
            f.model.submitPasskey(FakeSecretField())
            assertNull(f.model.form.state.value.banner)
            assertNull(f.model.form.state.value.failure)
            assertFalse(f.model.form.busy)
            assertNull(f.model.form.takeAnnouncement { it.key })
        }

    private suspend fun failure(
        failure: PasskeyFailure,
        io: kotlinx.coroutines.CoroutineDispatcher,
    ) {
        val f = PasskeyTestFixture(io, SystemPasskeyAssertionProvider(CredentialClient { throw failure }))
        f.model.submitPasskey(FakeSecretField())
        assertEquals(AuthBannerKind.Server, f.model.form.state.value.banner)
    }

    @Test fun otherFailuresShowTheServerBannerUnavailable() =
        runTest(timeout = 60.seconds) {
            failure(PasskeyFailure.Unavailable, StandardTestDispatcher(testScheduler))
        }

    @Test fun otherFailuresShowTheServerBannerInvalidEncoding() =
        runTest(timeout = 60.seconds) {
            failure(PasskeyFailure.InvalidEncoding, StandardTestDispatcher(testScheduler))
        }

    @Test fun otherFailuresShowTheServerBannerBusy() =
        runTest(timeout = 60.seconds) {
            failure(PasskeyFailure.Busy, StandardTestDispatcher(testScheduler))
        }
}
