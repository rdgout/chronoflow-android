@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.OutboxKind
import net.chronoflow.core.model.ProfileChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.api.ResetPasswordInput
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.model.api.SignedVerificationInput
import net.chronoflow.core.model.api.TwoFactorInput
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.Lane
import net.chronoflow.core.model.contracts.renderableBlocks
import net.chronoflow.core.network.support.MockApi
import net.chronoflow.core.network.support.RecordingDeadline
import net.chronoflow.core.network.support.failure
import net.chronoflow.core.network.support.json
import net.chronoflow.core.network.support.requestJson
import net.chronoflow.core.network.support.response
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ApiDataSourceTests {
    @get:Rule val api = MockApi()
    private val id = "0199a3c0-0000-7000-8000-000000000010"

    @Test fun nativeLinkInputsReachExactPublicResetAndAuthenticatedSignedVerificationBodies() =
        runTest(timeout = 60.seconds) {
            api.reply("POST", "/v1/auth/reset-password", 204)
            api.reply("POST", "/v1/email/verify", 200, json("auth/verification.json"))
            val source = testSource(api.client())
            source.resetPassword(ResetPasswordInput("a%2Fb", "a+b@example.com", "NewPassword123!", "NewPassword123!"))
            val signed =
                SignedVerificationInput(
                    "0199a3c0-0000-7000-8000-000000000001",
                    "a".repeat(40),
                    1800000000L,
                    "b".repeat(64),
                )
            source.verifyEmail(signed)
            assertThat(
                requestJson(api.requests[0]),
            ).isEqualTo(
                buildJsonObject {
                    put("token", "a%2Fb")
                    put("email", "a+b@example.com")
                    put("password", "NewPassword123!")
                    put("password_confirmation", "NewPassword123!")
                },
            )
            assertThat(
                requestJson(api.requests[1]),
            ).isEqualTo(
                buildJsonObject {
                    put("id", "0199a3c0-0000-7000-8000-000000000001")
                    put("hash", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                    put("expires", 1800000000L)
                    put("signature", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                },
            )
            assertThat(api.requests[0].headers["Authorization"]).isEqualTo(null)
            assertThat(api.requests[1].headers["Authorization"]).isEqualTo("Bearer artificial-old-token")
            assertThat(api.server.requestCount).isEqualTo(2)
        }

    @Test fun publicAuthRoutesAndSignedVerification() =
        runTest(timeout = 60.seconds) {
            val deadline = RecordingDeadline()
            val source = testSource(api.client(deadline = deadline))
            api.reply("POST", "/v1/auth/login", 200, json("auth/two-factor.json"))
            api.reply("POST", "/v1/auth/two-factor", 200, json("auth/session.json"))
            api.reply("POST", "/v1/auth/register", 200, json("auth/session.json"))
            api.reply("POST", "/v1/auth/passkey/options", 200, json("auth/passkey-options.json"))
            api.reply("POST", "/v1/auth/passkey/login", 200, json("auth/session.json"))
            api.reply("POST", "/v1/auth/forgot-password", 204)
            api.reply("POST", "/v1/auth/reset-password", 204)
            api.reply("POST", "/v1/email/verification-notification", 200, json("auth/verification.json"))
            api.reply("POST", "/v1/email/verify", 200, json("auth/verification.json"))
            api.reply("POST", "/v1/auth/logout", 204)
            assertThat(source.signIn(PasswordSignInInput("test@example.com", "fake", "test-device", "Phone")))
                .isEqualTo(SignInResult.TwoFactor("artificial-two-factor-challenge"))
            source.completeTwoFactor(TwoFactorInput("fake", TwoFactorInput.Proof.Code("123456")))
            source.signUp(registration())
            source.passkeyOptions()
            source.passkeyLogin(passkeyInput())
            source.requestPasswordReset("test@example.com")
            source.resetPassword(ResetPasswordInput("fake", "test@example.com", "fake", "fake"))
            source.resendVerificationEmail()
            source.verifyEmail(SignedVerificationInput("fake", "fake", 1, "fake"))
            source.signOut()
            assertThat(api.server.requestCount).isEqualTo(10)
            assertTrue(
                api.requests.all {
                    it.method == "POST" && it.headers["Accept"] == "application/json" &&
                        it.headers["Cookie"] == null
                },
            )
            assertTrue(api.requests.take(7).all { it.headers["Authorization"] == null })
            assertTrue(api.requests.drop(7).all { it.headers["Authorization"] == "Bearer artificial-old-token" })
            assertThat(requestJson(api.requests[1])).isEqualTo(
                buildJsonObject {
                    put("challengeId", "fake")
                    put("code", "123456")
                },
            )
            assertThat(requestJson(api.requests[2])?.get("language")).isEqualTo(JsonPrimitive("nl"))
            assertThat(api.requests[3].bodySize).isEqualTo(0L)
            assertThat(api.requests[3].headers["Content-Type"]).isEqualTo(null)
            assertThat(
                requestJson(api.requests[4])?.get("credential")?.jsonObject?.get("rawId"),
            ).isEqualTo(JsonPrimitive("AA=="))
            assertThat(
                deadline.durations,
            ).isEqualTo(
                listOf(
                    15.seconds,
                    15.seconds,
                    90.seconds,
                    15.seconds,
                    15.seconds,
                    15.seconds,
                    15.seconds,
                    15.seconds,
                    15.seconds,
                    3.seconds,
                ),
            )
        }

    @Test fun protectedReadsMutationsAndPathEncoding() =
        runTest(timeout = 60.seconds) {
            val source = testSource(api.client())
            api.reply("GET", "/v1/users/me", 200, json("account/identity.json"))
            api.reply("GET", "/v1/account", 200, json("account/snapshot.json"))
            api.reply("GET", "/v1/trips", 200, json("trips/list.json"))
            api.reply("GET", "/v1/trips/$id", 200, json("trips/detail.json"))
            api.reply("PUT", "/v1/trips/test/checks", 200, json("trips/checks.json"))
            api.reply("PATCH", "/v1/account/body-clock", 200, json("account/snapshot.json"))
            api.reply("PATCH", "/v1/account/preferences", 200, json("account/snapshot.json"))
            api.reply("PATCH", "/v1/account/reminders", 200, json("account/snapshot.json"))
            api.reply("PATCH", "/v1/users/me", 200, json("account/identity.json"))
            api.reply("POST", "/v1/terms/acceptance", 204)
            api.reply("POST", "/v1/walkthrough", 204)
            source.fetchIdentity()
            assertThat(source.verificationStatus().resendAfterSeconds).isEqualTo(0)
            source.fetchAccount()
            assertThat(source.fetchTrips().size).isEqualTo(1)
            source.fetchTripDetail(id)
            source.send(OutboxChange.CheckOff(CheckOffChange("test", "fake", BlockType.Sleep, "2026-11-10", 1, null)))
            source.send(
                OutboxChange.Settings(SettingsSection.BodyClock, buildJsonObject { put("habitual_bed", "test") }),
            )
            source.send(
                OutboxChange.Settings(SettingsSection.Preferences, buildJsonObject { put("time_format", "test") }),
            )
            source.send(OutboxChange.Settings(SettingsSection.Reminders, buildJsonObject { put("lead_min", "test") }))
            source.send(OutboxChange.Profile(ProfileChange("Test", "demo@example.com")))
            source.send(OutboxChange.Policies("1.0"))
            source.send(OutboxChange.Walkthrough)
            assertThat(failure { source.send(OutboxChange.Unsupported(OutboxKind.Security, JsonObject(emptyMap()))) })
                .isEqualTo(DataFailure.Unsupported(OutboxKind.Security))
            assertThat(requestJson(api.requests[5])?.get("status")).isEqualTo(JsonNull)
            assertFalse(requestJson(api.requests[5])!!.containsKey("tripID"))
            assertThat(requestJson(api.requests[10])).isEqualTo(
                buildJsonObject {
                    put("accepted", true)
                    put("version", "1.0")
                },
            )
            assertTrue(api.requests.all { it.headers["Authorization"] == "Bearer artificial-old-token" })
            assertThat(api.server.requestCount).isEqualTo(12)
        }

    @Test fun passkeyErrorsAndUnverified() =
        runTest(timeout = 60.seconds) {
            val source = testSource(api.client())
            api.reply("POST", "/v1/auth/passkey/login", 200, json("auth/unverified.json"))
            assertThat(source.passkeyLogin(passkeyInput()).account.hasVerifiedEmail).isEqualTo(false)
            passkeyError(source, "credential", "We couldn't sign you in with that passkey. Please try again.")
            passkeyError(source, "challengeId", "The passkey request expired. Please try again.")
        }

    private suspend fun passkeyError(
        source: ApiDataSource,
        field: String,
        key: String,
    ) {
        api.reply(
            "POST",
            "/v1/auth/passkey/login",
            422,
            """{"code":"validation_failed","message":"Invalid","errors":{"$field":"$key"}}""",
        )
        assertThat(failure { source.passkeyLogin(passkeyInput()) }).isEqualTo(
            DataFailure.Validation(
                mapOf(field to FieldMessage(key)),
                422,
            ),
        )
    }

    @Test fun rejectsInvalidPasskeyOptionsAndSessionDiscriminator() =
        runTest(timeout = 60.seconds) {
            invalidOptions("app.chronoflow.net", "foreign.invalid")
            invalidOptions("60000", "120000")
            invalidOptions("A".repeat(43), "+".repeat(43))
            invalidOptions("required", "preferred")
            api.reply("POST", "/v1/auth/passkey/login", 200, json("auth/two-factor.json"))
            assertThat(
                failure { testSource(api.client()).passkeyLogin(passkeyInput()) },
            ).isEqualTo(DataFailure.Server(200))
        }

    private suspend fun invalidOptions(
        from: String,
        to: String,
    ) {
        api.reply("POST", "/v1/auth/passkey/options", 200, json("auth/passkey-options.json").replace(from, to))
        assertThat(failure { testSource(api.client()).passkeyOptions() }).isEqualTo(DataFailure.Server(200))
    }

    @Test fun fetchTripDetailKeepsTheExactServerBytes() =
        runTest(timeout = 60.seconds) {
            val raw = fixtureObject("trips/detail.json")
            val plan = raw.getValue("plan").jsonObject
            val block =
                plan
                    .getValue("blocks")
                    .jsonArray
                    .first()
                    .jsonObject
            val future =
                JsonObject(
                    block +
                        mapOf(
                            "id" to JsonPrimitive("future"),
                            "type" to JsonPrimitive("future_step"),
                            "lane" to JsonPrimitive("future_lane"),
                            "params" to
                                buildJsonObject {
                                    put(
                                        "future",
                                        JsonArray(listOf(JsonPrimitive(1), JsonNull, JsonPrimitive("kept"))),
                                    )
                                },
                        ),
                )
            val patched =
                JsonObject(
                    raw +
                        (
                            "plan" to
                                JsonObject(plan + ("blocks" to JsonArray(plan.getValue("blocks").jsonArray + future)))
                        ),
                )
            val text = " \n {\"futureEnvelope\":{\"preserve\":true}," + patched.toString().drop(1) + " \n"
            api.reply("GET", "/v1/trips/$id", 200, text)
            val result = testSource(api.client()).fetchTripDetail(id)
            assertArrayEquals(text.encodeToByteArray(), result.data)
            assertThat(
                result.detail.plan.blocks
                    .last()
                    .type,
            ).isEqualTo(BlockType.Unknown("future_step"))
            assertThat(
                result.detail.plan.blocks
                    .last()
                    .lane,
            ).isEqualTo(Lane.Unknown("future_lane"))
            assertFalse(
                result.detail.plan.renderableBlocks
                    .any { it.id == "future" },
            )
        }

    @Test fun fetchTripDetailRejectsAnotherTrip() =
        runTest(timeout = 60.seconds) {
            api.reply("GET", "/v1/trips/other", 200, json("trips/detail.json"))
            assertThat(failure { testSource(api.client()).fetchTripDetail("other") }).isEqualTo(DataFailure.Server(200))
        }

    @Test fun fetchTripsRejectsEmptyIds() =
        runTest(timeout = 60.seconds) {
            api.reply("GET", "/v1/trips", 200, json("trips/list.json").replace(id, ""))
            assertThat(failure { testSource(api.client()).fetchTrips() }).isEqualTo(DataFailure.Server(200))
        }
}
