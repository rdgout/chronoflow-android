package net.chronoflow.core.model.api

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.testing.Fixtures
import org.junit.Test

class AuthContractsTests {
    private fun signIn(path: String) = ApiJson.response.decodeFromString(SignInResult.serializer(), Fixtures.text(path))

    @Test
    fun sessionFixtureDecodesVerified() {
        val result = (signIn("auth/session.json") as SignInResult.Session).result
        assertThat(result.account.hasVerifiedEmail).isEqualTo(true)
        assertThat(result.tripId).isNull()
    }

    @Test
    fun unverifiedFixtureDecodesUnverified() {
        val result = (signIn("auth/unverified.json") as SignInResult.Session).result
        assertThat(result.account.hasVerifiedEmail).isEqualTo(false)
        assertThat(result.tripId).isNull()
    }

    @Test
    fun twoFactorFixtureDecodes() {
        assertThat(signIn("auth/two-factor.json")).isEqualTo(SignInResult.TwoFactor("artificial-two-factor-challenge"))
    }

    @Test
    fun verificationFixtureDecodes() {
        val status =
            ApiJson.response.decodeFromString(
                EmailVerificationStatus.serializer(),
                Fixtures.text("auth/verification.json"),
            )
        assertThat(status).isEqualTo(EmailVerificationStatus("demo@example.com", true, 0))
    }

    @Test
    fun identityFixtureDecodes() {
        val account =
            ApiJson.response.decodeFromString(
                SessionAccount.serializer(),
                Fixtures.text("account/identity.json"),
            )
        assertThat(account.id).isEqualTo("0199a3c0-0000-7000-8000-000000000000")
        assertThat(account.policies.acceptedTermsVersion).isNull()
    }

    @Test
    fun sessionWithANonUuidAccountIdThrows() {
        val raw = ApiJson.response.parseToJsonElement(Fixtures.text("auth/session.json")) as JsonObject
        val account = (raw["account"] as JsonObject) + ("id" to JsonPrimitive("not-a-uuid"))
        val text =
            ApiJson.response.encodeToString(
                JsonElement.serializer(),
                JsonObject(raw + ("account" to JsonObject(account))),
            )
        assertFailure {
            ApiJson.response.decodeFromString(SessionResult.serializer(), text)
        }.isInstanceOf<SerializationException>()
    }

    @Test
    fun sessionEncodesTripIdNullUnderBothConfigurations() {
        val result = (signIn("auth/session.json") as SignInResult.Session).result
        listOf(ApiJson.response, ApiJson.request).forEach { json ->
            val obj = json.parseToJsonElement(json.encodeToString(SessionResult.serializer(), result)) as JsonObject
            assertThat(obj.containsKey("tripId")).isEqualTo(true)
            assertThat(obj["kind"]).isEqualTo(JsonPrimitive("session"))
        }
    }

    @Test
    fun signInWithoutDraftAndGuestEncodesExactlyFourKeys() {
        val input = PasswordSignInInput("a@b.c", "pw", "dev", "Pixel")
        val obj =
            ApiJson.response.parseToJsonElement(
                ApiJson.request.encodeToString(PasswordSignInInput.serializer(), input),
            ) as JsonObject
        assertThat(obj.keys).isEqualTo(setOf("email", "password", "device_id", "device_name"))
    }

    @Test
    fun signedVerificationExpiresIsALong() {
        val text = """{"id":"1","hash":"h","expires":4102444800,"signature":"s"}"""
        assertThat(
            ApiJson.response.decodeFromString(SignedVerificationInput.serializer(), text).expires,
        ).isEqualTo(4_102_444_800L)
    }
}
