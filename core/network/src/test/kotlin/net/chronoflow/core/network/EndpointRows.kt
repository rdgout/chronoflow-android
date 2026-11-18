@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.MutationReceipt
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.OutboxKind
import net.chronoflow.core.model.ProfileChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.api.AccountSnapshot
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiPasskey
import net.chronoflow.core.model.api.ApiPasskeyChallenge
import net.chronoflow.core.model.api.ApiPasskeyList
import net.chronoflow.core.model.api.ApiPasskeyRegistrationChallenge
import net.chronoflow.core.model.api.ApiRecoveryCodes
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.api.ApiTwoFactorSetup
import net.chronoflow.core.model.api.EmailVerificationStatus
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.api.ResetPasswordInput
import net.chronoflow.core.model.api.SessionAccount
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.model.api.SignedVerificationInput
import net.chronoflow.core.model.api.TwoFactorInput
import net.chronoflow.core.model.contracts.ApiAirportSearchResults
import net.chronoflow.core.model.contracts.ApiTripInputSnapshot
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.model.contracts.ApiTripSaveResult
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.TripCheckResult
import net.chronoflow.core.model.contracts.TripDetailResponse
import net.chronoflow.core.model.contracts.TripListResponse
import net.chronoflow.core.network.support.MockApi
import net.chronoflow.core.network.support.RecordingDeadline
import net.chronoflow.core.network.support.failure
import net.chronoflow.core.network.support.json
import net.chronoflow.core.network.support.requestJson
import net.chronoflow.core.network.support.response
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class EndpointRow(
    val label: String,
    val method: String,
    val verb: String,
    val path: String,
    val call: suspend (DataSource) -> Any?,
    val expected: () -> Any?,
    val reply: String = "",
    val status: Int = 200,
    val requestBody: JsonObject? = null,
    val public: Boolean = false,
    val seconds: Int = 15,
    val query: String? = null,
)

private inline fun <reified T> decode(text: String): T = ApiJson.response.decodeFromString(text)

private fun obj(text: String): JsonObject = ApiJson.response.parseToJsonElement(text) as JsonObject

internal val ENDPOINT_ROWS: List<EndpointRow> =
    listOf(
        EndpointRow(
            "1 POST auth/passkey/options",
            "passkeyOptions",
            "POST",
            "auth/passkey/options",
            call = { it.passkeyOptions() },
            expected = { decode<ApiPasskeyChallenge>(json("auth/passkey-options.json")) },
            reply = json("auth/passkey-options.json"),
            public = true,
        ),
        EndpointRow(
            "2 POST auth/passkey/login",
            "passkeyLogin",
            "POST",
            "auth/passkey/login",
            call = { it.passkeyLogin(passkeyInput()) },
            expected = { decode<SessionResult>(json("auth/session.json")) },
            reply = json("auth/session.json"),
            requestBody =
                obj(
                    """{"challengeId":"fake","credential":{"id":"AA","rawId":"AA==","type":"public-key","response":{"clientDataJSON":"AA","authenticatorData":"AA","signature":"AA"}},"device_id":"test-device","device_name":"Phone"}""",
                ),
            public = true,
        ),
        EndpointRow(
            "3 POST auth/login",
            "signIn",
            "POST",
            "auth/login",
            call = { it.signIn(PasswordSignInInput("test@example.com", "fake", "test-device", "Phone")) },
            expected = { decode<SignInResult>(json("auth/two-factor.json")) },
            reply = json("auth/two-factor.json"),
            requestBody =
                obj(
                    """{"email":"test@example.com","password":"fake","device_id":"test-device","device_name":"Phone"}""",
                ),
            public = true,
        ),
        EndpointRow(
            "4 POST auth/two-factor",
            "completeTwoFactor",
            "POST",
            "auth/two-factor",
            call = { it.completeTwoFactor(TwoFactorInput("fake", TwoFactorInput.Proof.Code("123456"))) },
            expected = { decode<SessionResult>(json("auth/session.json")) },
            reply = json("auth/session.json"),
            requestBody = obj("""{"challengeId":"fake","code":"123456"}"""),
            public = true,
        ),
        EndpointRow(
            "5 POST auth/register",
            "signUp",
            "POST",
            "auth/register",
            call = { it.signUp(registration()) },
            expected = { decode<SessionResult>(json("auth/session.json")) },
            reply = json("auth/session.json"),
            requestBody =
                obj(
                    """{"name":"Test","email":"test@example.com","password":"fake","password_confirmation":"fake","device_id":"test-device","device_name":"Phone","language":"nl"}""",
                ),
            public = true,
            seconds = 90,
        ),
        EndpointRow(
            "6 POST auth/forgot-password",
            "requestPasswordReset",
            "POST",
            "auth/forgot-password",
            call = { it.requestPasswordReset("demo@example.com") },
            expected = { Unit },
            status = 204,
            requestBody = obj("""{"email":"demo@example.com"}"""),
            public = true,
        ),
        EndpointRow(
            "7 POST auth/reset-password",
            "resetPassword",
            "POST",
            "auth/reset-password",
            call = { it.resetPassword(ResetPasswordInput("fake", "demo@example.com", "new", "new")) },
            expected = { Unit },
            status = 204,
            requestBody =
                obj(
                    """{"token":"fake","email":"demo@example.com","password":"new","password_confirmation":"new"}""",
                ),
            public = true,
        ),
        EndpointRow(
            "8 GET users/me",
            "verificationStatus",
            "GET",
            "users/me",
            call = { it.verificationStatus() },
            expected = { EmailVerificationStatus("demo@example.com", true, 0) },
            reply = json("account/identity.json"),
        ),
        EndpointRow(
            "9 POST email/verification-notification",
            "resendVerificationEmail",
            "POST",
            "email/verification-notification",
            call = { it.resendVerificationEmail() },
            expected = { decode<EmailVerificationStatus>(json("auth/verification.json")) },
            reply = json("auth/verification.json"),
        ),
        EndpointRow(
            "10 POST email/verify",
            "verifyEmail",
            "POST",
            "email/verify",
            call = { it.verifyEmail(SignedVerificationInput("fake", "fake", 1, "fake")) },
            expected = { decode<EmailVerificationStatus>(json("auth/verification.json")) },
            reply = json("auth/verification.json"),
            requestBody = obj("""{"id":"fake","hash":"fake","expires":1,"signature":"fake"}"""),
        ),
        EndpointRow(
            "11 POST auth/logout",
            "signOut",
            "POST",
            "auth/logout",
            call = { it.signOut() },
            expected = { Unit },
            status = 204,
            seconds = 3,
        ),
        EndpointRow(
            "12 DELETE account",
            "deleteAccount",
            "DELETE",
            "account",
            call = { it.deleteAccount("password") },
            expected = { Unit },
            status = 204,
            requestBody = obj("""{"password":"password"}"""),
        ),
        EndpointRow(
            "13a POST auth/forgot-password",
            "accountSecurity",
            "POST",
            "auth/forgot-password",
            call = { it.accountSecurity(AccountSecurityChange.RequestPasswordReset("demo@example.com")) },
            expected = { AccountSecurityResult.Changed },
            status = 204,
            requestBody = obj("""{"email":"demo@example.com"}"""),
            public = true,
        ),
        EndpointRow(
            "13b PUT account/password",
            "accountSecurity",
            "PUT",
            "account/password",
            call = { it.accountSecurity(AccountSecurityChange.Password("old", "new", "new")) },
            expected = { AccountSecurityResult.Changed },
            status = 204,
            requestBody = obj("""{"current_password":"old","password":"new","password_confirmation":"new"}"""),
        ),
        EndpointRow(
            "13c POST account/two-factor",
            "accountSecurity",
            "POST",
            "account/two-factor",
            call = { it.accountSecurity(AccountSecurityChange.EnableTwoFactor("old")) },
            expected = { AccountSecurityResult.Setup(ApiTwoFactorSetup("secret", "otpauth://test")) },
            reply = """{"secretKey":"secret","otpauthUrl":"otpauth://test"}""",
            requestBody = obj("""{"current_password":"old"}"""),
        ),
        EndpointRow(
            "13d POST account/two-factor/confirmation",
            "accountSecurity",
            "POST",
            "account/two-factor/confirmation",
            call = { it.accountSecurity(AccountSecurityChange.ConfirmTwoFactor("123456")) },
            expected = { AccountSecurityResult.Codes(ApiRecoveryCodes(listOf("code"))) },
            reply = """{"recoveryCodes":["code"]}""",
            requestBody = obj("""{"code":"123456"}"""),
        ),
        EndpointRow(
            "13e DELETE account/two-factor",
            "accountSecurity",
            "DELETE",
            "account/two-factor",
            call = { it.accountSecurity(AccountSecurityChange.DisableTwoFactor("old")) },
            expected = { AccountSecurityResult.Snapshot(decode<AccountSnapshot>(json("account/snapshot.json"))) },
            reply = json("account/snapshot.json"),
            requestBody = obj("""{"current_password":"old"}"""),
        ),
        EndpointRow(
            "13f POST account/two-factor/recovery-codes/reveal",
            "accountSecurity",
            "POST",
            "account/two-factor/recovery-codes/reveal",
            call = { it.accountSecurity(AccountSecurityChange.RevealRecoveryCodes("old")) },
            expected = { AccountSecurityResult.Codes(ApiRecoveryCodes(listOf("code"))) },
            reply = """{"recoveryCodes":["code"]}""",
            requestBody = obj("""{"current_password":"old"}"""),
        ),
        EndpointRow(
            "13g POST account/two-factor/recovery-codes",
            "accountSecurity",
            "POST",
            "account/two-factor/recovery-codes",
            call = { it.accountSecurity(AccountSecurityChange.RegenerateRecoveryCodes("old")) },
            expected = { AccountSecurityResult.Codes(ApiRecoveryCodes(listOf("code"))) },
            reply = """{"recoveryCodes":["code"]}""",
            requestBody = obj("""{"current_password":"old"}"""),
        ),
        EndpointRow(
            "13h GET account/passkeys",
            "accountSecurity",
            "GET",
            "account/passkeys",
            call = { it.accountSecurity(AccountSecurityChange.ListPasskeys) },
            expected = { AccountSecurityResult.Passkeys(decode<ApiPasskeyList>(PASSKEY_LIST).passkeys) },
            reply = PASSKEY_LIST,
        ),
        EndpointRow(
            "13i POST account/passkeys/options",
            "accountSecurity",
            "POST",
            "account/passkeys/options",
            call = { it.accountSecurity(AccountSecurityChange.PasskeyCreationOptions("pw")) },
            expected = {
                AccountSecurityResult.PasskeyCreation(
                    decode<ApiPasskeyRegistrationChallenge>(PASSKEY_CREATION),
                )
            },
            reply = PASSKEY_CREATION,
            requestBody = obj("""{"current_password":"pw"}"""),
        ),
        EndpointRow(
            "13j POST account/passkeys",
            "accountSecurity",
            "POST",
            "account/passkeys",
            call = { it.accountSecurity(AccountSecurityChange.AddPasskey(attestationInput())) },
            expected = { AccountSecurityResult.PasskeyAdded(decode<ApiPasskey>(PASSKEY_ADDED)) },
            reply = PASSKEY_ADDED,
            status = 201,
            requestBody =
                obj(
                    """{"challengeId":"c1","name":"iPhone","credential":{"id":"AQID","rawId":"AQID","type":"public-key","response":{"clientDataJSON":"Y2Q","attestationObject":"YXR0","transports":["internal"]}}}""",
                ),
        ),
        EndpointRow(
            "13k DELETE account/passkeys/12",
            "accountSecurity",
            "DELETE",
            "account/passkeys/12",
            call = { it.accountSecurity(AccountSecurityChange.RemovePasskey(12, "pw")) },
            expected = { AccountSecurityResult.Changed },
            status = 204,
            requestBody = obj("""{"current_password":"pw"}"""),
        ),
        EndpointRow(
            "14 GET users/me",
            "fetchIdentity",
            "GET",
            "users/me",
            call = { it.fetchIdentity() },
            expected = { decode<SessionAccount>(json("account/identity.json")) },
            reply = json("account/identity.json"),
        ),
        EndpointRow(
            "15 GET account",
            "fetchAccount",
            "GET",
            "account",
            call = { it.fetchAccount() },
            expected = { decode<AccountSnapshot>(json("account/snapshot.json")) },
            reply = json("account/snapshot.json"),
        ),
        EndpointRow(
            "16 GET trips",
            "fetchTrips",
            "GET",
            "trips",
            call = { it.fetchTrips() },
            expected = { decode<TripListResponse>(json("trips/list.json")).trips },
            reply = json("trips/list.json"),
        ),
        EndpointRow(
            "17 GET trips/0199a3c0-0000-7000-8000-000000000010",
            "fetchTripDetail",
            "GET",
            "trips/0199a3c0-0000-7000-8000-000000000010",
            call = { it.fetchTripDetail("0199a3c0-0000-7000-8000-000000000010") },
            expected = { TripDetailResponse.decode(json("trips/detail.json").encodeToByteArray()) },
            reply = json("trips/detail.json"),
        ),
        EndpointRow(
            "18 GET airports",
            "searchAirports",
            "GET",
            "airports",
            call = { it.searchAirports("São Paulo & city+airport", 7) },
            expected = { decode<ApiAirportSearchResults>(json("wizard/airports.json")).airports },
            reply = json("wizard/airports.json"),
            public = true,
            query = "limit=7&query=S%C3%A3o%20Paulo%20%26%20city%2Bairport",
        ),
        EndpointRow(
            "19 POST trips/review",
            "reviewTrip",
            "POST",
            "trips/review",
            call = {
                it.reviewTrip(
                    ApiTripReviewInput(
                        wizardInput(),
                        profile = fixtureObject("wizard/review-request.json").getValue("profile").jsonObject,
                    ),
                )
            },
            expected = { decode<ApiTripReview>(json("wizard/review.json")) },
            reply = json("wizard/review.json"),
            requestBody = fixtureObject("wizard/review-request.json"),
            seconds = 90,
        ),
        EndpointRow(
            "20 POST guest/trips/review",
            "reviewGuestTrip",
            "POST",
            "guest/trips/review",
            call = { it.reviewGuestTrip(guestReviewInput()) },
            expected = { decode<ApiTripReview>(json("wizard/review.json")) },
            reply = json("wizard/review.json"),
            requestBody = fixtureObject("wizard/guest-review-request.json"),
            public = true,
            seconds = 90,
        ),
        EndpointRow(
            "21 POST trips/trip/review",
            "reviewTripEdit",
            "POST",
            "trips/trip/review",
            call = { it.reviewTripEdit(ApiTripReviewInput(wizardInput()), "trip", "source") },
            expected = { decode<ApiTripReview>(json("wizard/review.json")) },
            reply = json("wizard/review.json"),
            requestBody =
                JsonObject(
                    fixtureObject("wizard/review-request.json").filterKeys { it != "profile" } +
                        ("source_revision" to JsonPrimitive("source")),
                ),
            seconds = 90,
        ),
        EndpointRow(
            "22 POST trips",
            "saveTrip",
            "POST",
            "trips",
            call = { it.saveTrip(saveInput()) },
            expected = { decode<ApiTripSaveResult>(json("wizard/save.json")) },
            reply = json("wizard/save.json"),
            status = 201,
            requestBody = fixtureObject("wizard/save-request.json"),
            seconds = 90,
        ),
        EndpointRow(
            "23 PUT trips/trip",
            "updateTrip",
            "PUT",
            "trips/trip",
            call = { it.updateTrip("trip", saveInput(), "source") },
            expected = { decode<ApiTripSaveResult>(json("wizard/save.json")) },
            reply = json("wizard/save.json"),
            requestBody =
                JsonObject(fixtureObject("wizard/save-request.json") + ("source_revision" to JsonPrimitive("source"))),
            seconds = 90,
        ),
        EndpointRow(
            "24 GET trips/trip/input",
            "tripInput",
            "GET",
            "trips/trip/input",
            call = { it.tripInput("trip") },
            expected = { decode<ApiTripInputSnapshot>(json("wizard/snapshot.json")) },
            reply = json("wizard/snapshot.json"),
        ),
        EndpointRow(
            "25a PUT trips/test/checks",
            "send",
            "PUT",
            "trips/test/checks",
            call = {
                it.send(
                    OutboxChange.CheckOff(CheckOffChange("test", "fake", BlockType.Sleep, "2026-11-10", 1, null)),
                )
            },
            expected = { MutationReceipt.Checks(decode<TripCheckResult>(json("trips/checks.json"))) },
            reply = json("trips/checks.json"),
            requestBody =
                obj(
                    """{"plan_input_hash":"fake","type":"sleep","local_date":"2026-11-10","ordinal":1,"status":null}""",
                ),
        ),
        EndpointRow(
            "25b PATCH account/body-clock",
            "send",
            "PATCH",
            "account/body-clock",
            call = { it.send(OutboxChange.Settings(SettingsSection.BodyClock, obj("""{"habitual_bed":"23:00"}"""))) },
            expected = { MutationReceipt.Account(decode<AccountSnapshot>(json("account/snapshot.json"))) },
            reply = json("account/snapshot.json"),
            requestBody = obj("""{"habitual_bed":"23:00"}"""),
        ),
        EndpointRow(
            "25c PATCH account/preferences",
            "send",
            "PATCH",
            "account/preferences",
            call = { it.send(OutboxChange.Settings(SettingsSection.Preferences, obj("""{"time_format":"24"}"""))) },
            expected = { MutationReceipt.Account(decode<AccountSnapshot>(json("account/snapshot.json"))) },
            reply = json("account/snapshot.json"),
            requestBody = obj("""{"time_format":"24"}"""),
        ),
        EndpointRow(
            "25d PATCH account/reminders",
            "send",
            "PATCH",
            "account/reminders",
            call = { it.send(OutboxChange.Settings(SettingsSection.Reminders, obj("""{"lead_min":"10"}"""))) },
            expected = { MutationReceipt.Account(decode<AccountSnapshot>(json("account/snapshot.json"))) },
            reply = json("account/snapshot.json"),
            requestBody = obj("""{"lead_min":"10"}"""),
        ),
        EndpointRow(
            "25e PATCH users/me",
            "send",
            "PATCH",
            "users/me",
            call = { it.send(OutboxChange.Profile(ProfileChange("Test", "demo@example.com"))) },
            expected = { MutationReceipt.Identity(decode<SessionAccount>(json("account/identity.json"))) },
            reply = json("account/identity.json"),
            requestBody = obj("""{"name":"Test","email":"demo@example.com"}"""),
        ),
        EndpointRow(
            "25f POST terms/acceptance",
            "send",
            "POST",
            "terms/acceptance",
            call = { it.send(OutboxChange.Policies("1.0")) },
            expected = { MutationReceipt.Acknowledged },
            status = 204,
            requestBody = obj("""{"accepted":true,"version":"1.0"}"""),
        ),
        EndpointRow(
            "25g POST walkthrough",
            "send",
            "POST",
            "walkthrough",
            call = { it.send(OutboxChange.Walkthrough) },
            expected = { MutationReceipt.Acknowledged },
            status = 204,
        ),
        EndpointRow(
            "25h NONE unsupported",
            "send",
            "NONE",
            "unsupported",
            call = { it.send(OutboxChange.Unsupported(OutboxKind.Security, JsonObject(emptyMap()))) },
            expected = { DataFailure.Unsupported(OutboxKind.Security) },
            seconds = 0,
        ),
    )
