package net.chronoflow.core.network

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.MutationReceipt
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.AccountIdentity
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.api.AccountSnapshot
import net.chronoflow.core.model.api.ApiCheckOffInput
import net.chronoflow.core.model.api.ApiGuestTripReviewInput
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiPasskey
import net.chronoflow.core.model.api.ApiPasskeyChallenge
import net.chronoflow.core.model.api.ApiPasskeyList
import net.chronoflow.core.model.api.ApiPasskeyLoginInput
import net.chronoflow.core.model.api.ApiPasskeyRegistrationChallenge
import net.chronoflow.core.model.api.ApiRecoveryCodes
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.api.ApiTripSaveInput
import net.chronoflow.core.model.api.ApiTwoFactorSetup
import net.chronoflow.core.model.api.EmailVerificationStatus
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.api.RegisterInput
import net.chronoflow.core.model.api.ResetPasswordInput
import net.chronoflow.core.model.api.SessionAccount
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.api.SettingsPatchValidation
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.model.api.SignedVerificationInput
import net.chronoflow.core.model.api.TripWizardErrors
import net.chronoflow.core.model.api.TwoFactorInput
import net.chronoflow.core.model.contracts.ApiAirportSearchResults
import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import net.chronoflow.core.model.contracts.ApiTripInputSnapshot
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.model.contracts.ApiTripSaveResult
import net.chronoflow.core.model.contracts.TripCheckResult
import net.chronoflow.core.model.contracts.TripDetailResponse
import net.chronoflow.core.model.contracts.TripListItem
import net.chronoflow.core.model.contracts.TripListResponse
import java.util.Base64

class ApiDataSource(
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

    override suspend fun deleteAccount(password: String) =
        client.requestNoContent(
            ApiEndpoint("DELETE", listOf("account"), body = body(buildJsonObject { put("password", password) })),
        )

    override suspend fun accountSecurity(change: AccountSecurityChange): AccountSecurityResult {
        fun endpoint(
            method: String,
            path: List<String>,
            fields: JsonObject,
        ) = ApiEndpoint(method, listOf("account") + path, body = body(fields))

        fun current(value: String) = buildJsonObject { put("current_password", value) }
        return when (change) {
            is AccountSecurityChange.RequestPasswordReset -> {
                requestPasswordReset(change.email)
                AccountSecurityResult.Changed
            }

            is AccountSecurityChange.Password -> {
                client.requestNoContent(
                    endpoint(
                        "PUT",
                        listOf("password"),
                        buildJsonObject {
                            put("current_password", change.current)
                            put("password", change.password)
                            put("password_confirmation", change.confirmation)
                        },
                    ),
                )
                AccountSecurityResult.Changed
            }

            is AccountSecurityChange.EnableTwoFactor -> {
                AccountSecurityResult.Setup(
                    client.request(
                        endpoint("POST", listOf("two-factor"), current(change.current)),
                        ApiTwoFactorSetup.serializer(),
                    ),
                )
            }

            is AccountSecurityChange.ConfirmTwoFactor -> {
                AccountSecurityResult.Codes(
                    client.request(
                        endpoint(
                            "POST",
                            listOf("two-factor", "confirmation"),
                            buildJsonObject {
                                put("code", change.code)
                            },
                        ),
                        ApiRecoveryCodes.serializer(),
                    ),
                )
            }

            is AccountSecurityChange.DisableTwoFactor -> {
                AccountSecurityResult.Snapshot(
                    client.request(
                        endpoint("DELETE", listOf("two-factor"), current(change.current)),
                        AccountSnapshot.serializer(),
                    ),
                )
            }

            is AccountSecurityChange.RevealRecoveryCodes -> {
                AccountSecurityResult.Codes(
                    client.request(
                        endpoint("POST", listOf("two-factor", "recovery-codes", "reveal"), current(change.current)),
                        ApiRecoveryCodes.serializer(),
                    ),
                )
            }

            is AccountSecurityChange.RegenerateRecoveryCodes -> {
                AccountSecurityResult.Codes(
                    client.request(
                        endpoint("POST", listOf("two-factor", "recovery-codes"), current(change.current)),
                        ApiRecoveryCodes.serializer(),
                    ),
                )
            }

            AccountSecurityChange.ListPasskeys -> {
                AccountSecurityResult.Passkeys(
                    client
                        .request(
                            ApiEndpoint(path = listOf("account", "passkeys")),
                            ApiPasskeyList.serializer(),
                        ).passkeys,
                )
            }

            is AccountSecurityChange.PasskeyCreationOptions -> {
                AccountSecurityResult.PasskeyCreation(
                    client.request(
                        endpoint("POST", listOf("passkeys", "options"), current(change.current)),
                        ApiPasskeyRegistrationChallenge.serializer(),
                    ),
                )
            }

            is AccountSecurityChange.AddPasskey -> {
                AccountSecurityResult.PasskeyAdded(
                    client.request(
                        ApiEndpoint("POST", listOf("account", "passkeys"), body = body(change.input)),
                        ApiPasskey.serializer(),
                        expectedStatus = 201,
                    ),
                )
            }

            is AccountSecurityChange.RemovePasskey -> {
                client.requestNoContent(
                    endpoint("DELETE", listOf("passkeys", change.id.toString()), current(change.current)),
                )
                AccountSecurityResult.Changed
            }
        }
    }

    override suspend fun searchAirports(
        query: String,
        limit: Int?,
    ): List<ApiAirportSuggestion> {
        val parameters = mutableMapOf("query" to query)
        limit?.let { parameters["limit"] = it.toString() }
        return client
            .request(
                ApiEndpoint(path = listOf("airports"), access = ApiEndpoint.Access.PublicRoute, query = parameters),
                ApiAirportSearchResults.serializer(),
            ).airports
    }

    override suspend fun reviewTrip(input: ApiTripReviewInput): ApiTripReview = review(input, listOf("trips", "review"))

    override suspend fun reviewGuestTrip(input: ApiGuestTripReviewInput): ApiTripReview =
        mapFailures(input.input.outbound.legs.size) {
            client.request(
                ApiEndpoint(
                    "POST",
                    listOf("guest", "trips", "review"),
                    ApiEndpoint.Access.PublicRoute,
                    body(input),
                    ApiTimeouts.Generation,
                ),
                ApiTripReview.serializer(),
            )
        }

    override suspend fun reviewTripEdit(
        input: ApiTripReviewInput,
        id: String,
        sourceRevision: String,
    ): ApiTripReview = review(input, listOf("trips", id, "review"), sourceRevision)

    private suspend fun review(
        input: ApiTripReviewInput,
        path: List<String>,
        sourceRevision: String? = null,
    ): ApiTripReview =
        mapFailures(input.input.outbound.legs.size) {
            val fields = ApiJson.request.encodeToJsonElement(ApiTripReviewInput.serializer(), input) as JsonObject
            val payload = withSource(fields, sourceRevision)
            client.request(
                ApiEndpoint("POST", path, body = body(payload), timeout = ApiTimeouts.Generation),
                ApiTripReview.serializer(),
            )
        }

    override suspend fun saveTrip(identity: ApiTripSaveInput): ApiTripSaveResult =
        save(identity, listOf("trips"), "POST", null, 201)

    override suspend fun updateTrip(
        id: String,
        identity: ApiTripSaveInput,
        sourceRevision: String,
    ): ApiTripSaveResult = save(identity, listOf("trips", id), "PUT", sourceRevision, 200)

    private suspend fun save(
        identity: ApiTripSaveInput,
        path: List<String>,
        method: String,
        sourceRevision: String?,
        expectedStatus: Int,
    ): ApiTripSaveResult =
        mapFailures {
            val fields = ApiJson.request.encodeToJsonElement(ApiTripSaveInput.serializer(), identity) as JsonObject
            client.request(
                ApiEndpoint(
                    method,
                    path,
                    body = body(withSource(fields, sourceRevision)),
                    timeout = ApiTimeouts.Generation,
                ),
                expectedStatus,
            ) { bytes ->
                val result = ApiJson.response.decodeFromString(ApiTripSaveResult.serializer(), bytes.decodeToString())
                if (result.tripId.isEmpty()) throw DataFailure.Server(expectedStatus)
                result
            }
        }

    override suspend fun tripInput(id: String): ApiTripInputSnapshot =
        client.request(ApiEndpoint(path = listOf("trips", id, "input")), ApiTripInputSnapshot.serializer())

    private fun withSource(
        fields: JsonObject,
        sourceRevision: String?,
    ): JsonObject =
        if (sourceRevision ==
            null
        ) {
            fields
        } else {
            JsonObject(fields + ("source_revision" to JsonPrimitive(sourceRevision)))
        }

    private suspend fun <T> mapFailures(
        outboundCount: Int? = null,
        work: suspend () -> T,
    ): T =
        try {
            work()
        } catch (failure: DataFailure) {
            throw TripWizardErrors.map(failure, outboundCount)
        }

    private inline fun <reified T> body(input: T): ByteArray = ApiJson.request.encodeToString(input).encodeToByteArray()

    private fun publicPost(
        path: List<String>,
        body: ByteArray,
        timeout: kotlin.time.Duration = ApiTimeouts.Default,
    ) = ApiEndpoint("POST", path, ApiEndpoint.Access.PublicRoute, body, timeout)
}
