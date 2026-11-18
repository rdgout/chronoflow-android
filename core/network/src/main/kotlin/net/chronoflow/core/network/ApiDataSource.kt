package net.chronoflow.core.network

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.MutationReceipt
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.AccountIdentity
import net.chronoflow.core.model.api.AccountSnapshot
import net.chronoflow.core.model.api.ApiCheckOffInput
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiPasskeyChallenge
import net.chronoflow.core.model.api.ApiPasskeyLoginInput
import net.chronoflow.core.model.api.EmailVerificationStatus
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.api.RegisterInput
import net.chronoflow.core.model.api.ResetPasswordInput
import net.chronoflow.core.model.api.SessionAccount
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.api.SettingsPatchValidation
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.model.api.SignedVerificationInput
import net.chronoflow.core.model.api.TwoFactorInput
import net.chronoflow.core.model.contracts.TripCheckResult
import net.chronoflow.core.model.contracts.TripDetailResponse
import net.chronoflow.core.model.contracts.TripListItem
import net.chronoflow.core.model.contracts.TripListResponse
import java.util.Base64

abstract class ApiDataSource(
    private val client: ApiClient,
) : DataSource {
    override suspend fun passkeyOptions(): ApiPasskeyChallenge =
        client.request(
            ApiEndpoint("POST", listOf("auth", "passkey", "options"), ApiEndpoint.Access.PublicRoute),
        ) { bytes ->
            val result = ApiJson.response.decodeFromString(ApiPasskeyChallenge.serializer(), bytes.decodeToString())
            val challenge = result.options.challenge
            val validChallenge =
                challenge.length == 43 &&
                    challenge.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '-' || it == '_' }
            val count = if (validChallenge) Base64.getUrlDecoder().decode(challenge).size else 0
            if (result.challengeId.length != 64 || result.options.rpId != "app.chronoflow.net" ||
                result.options.timeout != 60000 || count != 32
            ) {
                throw DataFailure.Server(200)
            }
            result
        }

    override suspend fun passkeyLogin(input: ApiPasskeyLoginInput): SessionResult =
        client.request(publicPost(listOf("auth", "passkey", "login"), body(input)), SessionResult.serializer())

    override suspend fun signIn(input: PasswordSignInInput): SignInResult =
        client.request(publicPost(listOf("auth", "login"), body(input)), SignInResult.serializer())

    override suspend fun completeTwoFactor(input: TwoFactorInput): SessionResult =
        client.request(publicPost(listOf("auth", "two-factor"), body(input)), SessionResult.serializer())

    override suspend fun signUp(input: RegisterInput): SessionResult =
        client.request(
            publicPost(listOf("auth", "register"), body(input), ApiTimeouts.Generation),
            SessionResult.serializer(),
        )

    override suspend fun requestPasswordReset(email: String) =
        client.requestNoContent(
            publicPost(listOf("auth", "forgot-password"), body(buildJsonObject { put("email", email) })),
        )

    override suspend fun resetPassword(input: ResetPasswordInput) =
        client.requestNoContent(publicPost(listOf("auth", "reset-password"), body(input)))

    override suspend fun verificationStatus(): EmailVerificationStatus {
        val identity = fetchIdentity()
        return EmailVerificationStatus(identity.email, identity.hasVerifiedEmail, 0)
    }

    override suspend fun resendVerificationEmail(): EmailVerificationStatus =
        client.request(
            ApiEndpoint("POST", listOf("email", "verification-notification")),
            EmailVerificationStatus.serializer(),
        )

    override suspend fun verifyEmail(input: SignedVerificationInput): EmailVerificationStatus =
        client.request(
            ApiEndpoint("POST", listOf("email", "verify"), body = body(input)),
            EmailVerificationStatus.serializer(),
        )

    override suspend fun signOut() =
        client.requestNoContent(ApiEndpoint("POST", listOf("auth", "logout"), timeout = ApiTimeouts.Logout))

    override suspend fun fetchIdentity(): AccountIdentity =
        client.request(ApiEndpoint(path = listOf("users", "me")), SessionAccount.serializer())

    override suspend fun fetchAccount(): AccountSnapshot =
        client.request(ApiEndpoint(path = listOf("account")), AccountSnapshot.serializer())

    override suspend fun fetchTrips(): List<TripListItem> =
        client.request(ApiEndpoint(path = listOf("trips"))) { bytes ->
            val result = ApiJson.response.decodeFromString(TripListResponse.serializer(), bytes.decodeToString())
            if (result.trips.any { it.card.id.isEmpty() || it.card.departureUtc.isEmpty() }) {
                throw DataFailure.Server(200)
            }
            result.trips
        }

    override suspend fun fetchTripDetail(id: String): TripDetailResponse =
        client.request(ApiEndpoint(path = listOf("trips", id))) { bytes ->
            val result = TripDetailResponse.decode(bytes)
            if (result.detail.trip.id != id) throw DataFailure.Server(200)
            result
        }

    override suspend fun send(change: OutboxChange): MutationReceipt =
        when (change) {
            is OutboxChange.CheckOff -> {
                val c = change.change
                val input = ApiCheckOffInput(c.planInputHash, c.type, c.localDate, c.ordinal, c.status)
                MutationReceipt.Checks(
                    client.request(
                        ApiEndpoint("PUT", listOf("trips", c.tripID, "checks"), body = body(input)),
                        TripCheckResult.serializer(),
                    ),
                )
            }

            is OutboxChange.Settings -> {
                SettingsPatchValidation.validate(change.section, change.patch)
                val path = if (change.section == SettingsSection.BodyClock) "body-clock" else change.section.raw
                MutationReceipt.Account(
                    client.request(
                        ApiEndpoint("PATCH", listOf("account", path), body = body(change.patch)),
                        AccountSnapshot.serializer(),
                    ),
                )
            }

            is OutboxChange.Profile -> {
                MutationReceipt.Identity(
                    client.request(
                        ApiEndpoint("PATCH", listOf("users", "me"), body = body(change.change)),
                        SessionAccount.serializer(),
                    ),
                )
            }

            is OutboxChange.Policies -> {
                client.requestNoContent(
                    ApiEndpoint(
                        "POST",
                        listOf("terms", "acceptance"),
                        body =
                            body(
                                buildJsonObject {
                                    put("accepted", true)
                                    put("version", change.version)
                                },
                            ),
                    ),
                )
                MutationReceipt.Acknowledged
            }

            OutboxChange.Walkthrough -> {
                client.requestNoContent(ApiEndpoint("POST", listOf("walkthrough")))
                MutationReceipt.Acknowledged
            }

            is OutboxChange.Unsupported -> {
                throw DataFailure.Unsupported(change.kind)
            }
        }

    private inline fun <reified T> body(input: T): ByteArray = ApiJson.request.encodeToString(input).encodeToByteArray()

    private fun publicPost(
        path: List<String>,
        body: ByteArray,
        timeout: kotlin.time.Duration = ApiTimeouts.Default,
    ) = ApiEndpoint("POST", path, ApiEndpoint.Access.PublicRoute, body, timeout)
}
