@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.datamock

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.MutationReceipt
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.AccountIdentity
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.api.AccountSettings
import net.chronoflow.core.model.api.AccountSnapshot
import net.chronoflow.core.model.api.ApiGuestTripReviewInput
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiPasskey
import net.chronoflow.core.model.api.ApiPasskeyAssertion
import net.chronoflow.core.model.api.ApiPasskeyAuthenticatorSelection
import net.chronoflow.core.model.api.ApiPasskeyChallenge
import net.chronoflow.core.model.api.ApiPasskeyCreationOptions
import net.chronoflow.core.model.api.ApiPasskeyCredentialDescriptor
import net.chronoflow.core.model.api.ApiPasskeyCredentialParameters
import net.chronoflow.core.model.api.ApiPasskeyLoginInput
import net.chronoflow.core.model.api.ApiPasskeyRegistrationChallenge
import net.chronoflow.core.model.api.ApiPasskeyRelyingParty
import net.chronoflow.core.model.api.ApiPasskeyRequestOptions
import net.chronoflow.core.model.api.ApiPasskeyUserEntity
import net.chronoflow.core.model.api.ApiRecoveryCodes
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.api.ApiTripSaveInput
import net.chronoflow.core.model.api.ApiTwoFactorSetup
import net.chronoflow.core.model.api.AuthLanguage
import net.chronoflow.core.model.api.CredentialType
import net.chronoflow.core.model.api.EmailVerificationStatus
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.api.RegisterInput
import net.chronoflow.core.model.api.ResetPasswordInput
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.model.api.SignedVerificationInput
import net.chronoflow.core.model.api.TripWizardErrors
import net.chronoflow.core.model.api.TwoFactorInput
import net.chronoflow.core.model.api.UserVerification
import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import net.chronoflow.core.model.contracts.ApiReviewIdentity
import net.chronoflow.core.model.contracts.ApiTripInput
import net.chronoflow.core.model.contracts.ApiTripInputSnapshot
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.model.contracts.ApiTripSaveResult
import net.chronoflow.core.model.contracts.CheckKey
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.model.contracts.TripCheckResult
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.contracts.TripDetailResponse
import net.chronoflow.core.model.contracts.TripDirection
import net.chronoflow.core.model.contracts.TripListItem
import java.io.IOException
import java.text.BreakIterator
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.roundToLong
import kotlin.random.Random

class MockDataSource(
    private val store: MockStateStore = MemoryMockStateStore(),
    private val seed: MockServerState? = null,
    offline: Boolean = false,
    private val mutationsOffline: Boolean = false,
    private val remoteTripUpdate: suspend () -> Boolean = { false },
    private val now: () -> Instant = { Instant.now() },
    private val delay: () -> Double = { Random.nextDouble(0.3, 0.8) },
    private val sleep: suspend (Double) -> Unit = { kotlinx.coroutines.delay((it * 1000).roundToLong()) },
    private val id: () -> String = { UUID.randomUUID().toString() },
    private val bearer: (suspend () -> String?)? = null,
    private val assertionProvider: AssertionProvider = { _, _ -> null },
    private val debugAppSeed: Boolean = true,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : DataSource {
    private val mutex = Mutex()
    private var state: MockServerState? = null
    private val server: MockServerState get() = checkNotNull(state)
    private var currentToken: String? = null
    private var requestToken: String? = null
    private var offline = offline
    private var pendingTwoFactor: Set<String> = emptySet()
    private var failures: Map<MockMethod, DataFailure> = emptyMap()

    suspend fun setOffline(value: Boolean) {
        mutex.withLock { offline = value }
    }

    suspend fun failNext(
        method: MockMethod,
        error: DataFailure,
    ) {
        mutex.withLock {
            failures =
                failures + (method to error)
        }
    }

    suspend fun inbox(): List<MockMail> =
        withContext(io) {
            mutex.withLock {
                load()
                server.mail.toList()
            }
        }

    private fun load() {
        if (state != null && store is MemoryMockStateStore) {
            store.load()?.let {
                state = it
                return
            }
        }
        if (state != null) return
        try {
            val restored =
                try {
                    store.load()
                } catch (failure: SerializationException) {
                    if (seed ==
                        null
                    ) {
                        null
                    } else {
                        throw failure
                    }
                }
            if (restored != null) store.save(restored)
            if (seed != null) {
                state = restored ?: seed
                return
            }
            if (debugAppSeed) {
                if (restored != null && restored.accounts.isNotEmpty() &&
                    restored.schemaVersion == MockServerState.currentSchemaVersion
                ) {
                    val refreshed = MockAppSeed.refresh(restored, now(), zone)
                    if (refreshed != restored) store.save(refreshed)
                    state = refreshed
                } else {
                    val initial = MockAppSeed.make(now(), zone = zone)
                    store.save(initial)
                    state = initial
                }
            } else {
                state = restored ?: MockServerState()
            }
        } catch (_: IOException) {
            throw DataFailure.Server(null)
        } catch (_: SerializationException) {
            throw DataFailure.Server(null)
        } catch (_: IllegalArgumentException) {
            throw DataFailure.Server(null)
        } catch (_: DataFailure) {
            throw DataFailure.Server(null)
        }
    }

    private suspend fun <T> begin(
        method: MockMethod,
        action: suspend () -> T,
    ): T {
        currentCoroutineContext().ensureActive()
        sleep(delay().coerceIn(0.3, 0.8))
        currentCoroutineContext().ensureActive()
        val token = if (method == MockMethod.ReviewGuestTrip) null else bearer?.invoke()
        currentCoroutineContext().ensureActive()
        return withContext(io) {
            mutex.withLock {
                if (offline || (mutationsOffline && method == MockMethod.Send)) throw DataFailure.Offline
                failures[method]?.let {
                    failures = failures - method
                    throw it
                }
                load()
                requestToken = if (bearer == null) currentToken else token
                action()
            }
        }
    }

    private fun update(change: (MockServerState) -> MockServerState) {
        if (store is MemoryMockStateStore) {
            state = store.update(server) { change(it) to Unit }.second
            return
        }
        val candidate = change(server)
        try {
            store.save(candidate)
        } catch (_: IOException) {
            throw DataFailure.Server(null)
        } catch (_: SerializationException) {
            throw DataFailure.Server(null)
        } catch (_: IllegalArgumentException) {
            throw DataFailure.Server(null)
        } catch (_: DataFailure) {
            throw DataFailure.Server(null)
        }
        state = candidate
    }

    private fun limit(
        key: String,
        count: Int = 5,
        seconds: Double = 60.0,
    ) {
        update { it.copy(rateWindows = MockRateLimiter.hit(key, count, seconds, seconds(now()), it.rateWindows)) }
    }

    private fun validation(
        field: String,
        key: String,
        args: Map<String, JsonPrimitive> = emptyMap(),
    ): DataFailure =
        DataFailure.Validation(
            mapOf(field to FieldMessage(key, args)),
            422,
        )

    private fun accountID(verified: Boolean = false): String {
        val time = seconds(now())
        val token = requestToken ?: throw DataFailure.Unauthorized(401)
        val session = server.sessions[token]?.takeIf { it.expiresAt > time } ?: throw DataFailure.Unauthorized(401)
        val account = server.accounts[session.accountID] ?: throw DataFailure.Unauthorized(401)
        if (verified && !account.snapshot.hasVerifiedEmail) throw DataFailure.EmailUnverified(403)
        update { value ->
            value.sessions[token]?.let {
                value.copy(
                    sessions =
                        value.sessions + (token to it.copy(expiresAt = time + 90 * 86400)),
                )
            }
                ?: value
        }
        return session.accountID
    }

    private fun emailKey(value: String): String = value.trim().lowercase(Locale.ROOT)

    private fun find(email: String): String? =
        server.accounts.entries
            .firstOrNull {
                emailKey(it.value.snapshot.email) ==
                    emailKey(email)
            }?.key

    private fun validUUID(value: String): Boolean =
        Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$").matches(value)

    private fun device(
        deviceID: String,
        name: String,
    ) {
        if (!validUUID(deviceID)) {
            throw validation(
                "device_id",
                "The :attribute field must be a valid UUID.",
                mapOf("attribute" to JsonPrimitive("device_id")),
            )
        }
        if (name.isBlank()) throw validation("device_name", "Please fill this in.")
        if (name.codePointCount(0, name.length) >
            255
        ) {
            throw validation("device_name", "Use no more than :max characters.", mapOf("max" to JsonPrimitive(255)))
        }
    }

    private fun graphemeCount(value: String): Int {
        val iterator = BreakIterator.getCharacterInstance(Locale.ROOT).apply { setText(value) }
        var count = 0
        while (iterator.next() != BreakIterator.DONE) count++
        return count
    }

    private fun password(
        value: String,
        confirmation: String,
    ) {
        val fields = linkedMapOf<String, FieldMessage>()
        if (value != confirmation) fields["password_confirmation"] = FieldMessage("The passwords don't match yet.")
        val types = value.codePoints().toArray().map { Character.getType(it) }
        val symbols =
            setOf(
                Character.MATH_SYMBOL,
                Character.CURRENCY_SYMBOL,
                Character.MODIFIER_SYMBOL,
                Character.OTHER_SYMBOL,
                Character.CONNECTOR_PUNCTUATION,
                Character.DASH_PUNCTUATION,
                Character.START_PUNCTUATION,
                Character.END_PUNCTUATION,
                Character.INITIAL_QUOTE_PUNCTUATION,
                Character.FINAL_QUOTE_PUNCTUATION,
                Character.OTHER_PUNCTUATION,
            ).map {
                it.toInt()
            }.toSet()
        val message =
            when {
                graphemeCount(value) < 12 -> {
                    FieldMessage("Use at least :min characters.", mapOf("min" to JsonPrimitive(12)))
                }

                types.none { it == Character.UPPERCASE_LETTER.toInt() || it == Character.TITLECASE_LETTER.toInt() } ||
                    types.none { it == Character.LOWERCASE_LETTER.toInt() } -> {
                    FieldMessage(
                        "Use both uppercase and lowercase letters.",
                    )
                }

                Character.DECIMAL_DIGIT_NUMBER.toInt() !in types -> {
                    FieldMessage("Use at least one number.")
                }

                types.none { it in symbols } -> {
                    FieldMessage("Use at least one symbol.")
                }

                else -> {
                    null
                }
            }
        if (message != null) fields["password"] = message
        if (fields.isNotEmpty()) throw DataFailure.Validation(fields, 422)
    }

    private fun authenticate(
        accountID: String,
        deviceID: String,
        draft: String? = null,
        secret: String? = null,
    ): SessionResult {
        val token = "mock-" + id()
        val account = server.accounts.getValue(accountID)
        val claim = draft?.let { server.claims[it] }
        val tripID =
            claim
                ?.takeIf {
                    secret != null && it.secretHash == MockGuestReview.hash(secret) &&
                        it.isLive(seconds(now())) &&
                        it.accountID == accountID
                }?.tripID
        update {
            it.copy(
                sessions =
                    it.sessions.filterValues { session ->
                        !(
                            session.accountID == accountID &&
                                session.deviceID == deviceID.lowercase(Locale.ROOT)
                        )
                    } +
                        (token to MockSession(accountID, deviceID.lowercase(Locale.ROOT), seconds(now()) + 90 * 86400)),
            )
        }
        currentToken = token
        requestToken = token
        return SessionResult(token, account.identity, tripID)
    }

    override suspend fun signIn(input: PasswordSignInInput): SignInResult =
        begin(MockMethod.SignIn) {
            limit("login:" + emailKey(input.email))
            device(input.deviceID, input.deviceName)
            if (emailKey(input.email).isEmpty()) throw validation("email", "Please fill this in.")
            if (input.password.isEmpty()) throw validation("password", "Please fill this in.")
            val accountID =
                find(input.email) ?: throw validation("email", "These sign-in details don't match our records.")
            val account = server.accounts.getValue(accountID)
            if (account.password !=
                input.password
            ) {
                throw validation("email", "These sign-in details don't match our records.")
            }
            if (account.twoFactorCode != null) {
                val challenge = id().replace("-", "") + id().replace("-", "")
                update {
                    it.copy(
                        twoFactor =
                            it.twoFactor +
                                (challenge to MockTwoFactor(accountID, input.deviceID, seconds(now()) + 300, 0)),
                    )
                }
                SignInResult.TwoFactor(challenge)
            } else {
                SignInResult.Session(authenticate(accountID, input.deviceID, input.draftToken, input.guestSecret))
            }
        }

    override suspend fun completeTwoFactor(input: TwoFactorInput): SessionResult =
        begin(MockMethod.CompleteTwoFactor) {
            limit("two-factor:" + input.challengeId)
            val value =
                when (val proof = input.proof) {
                    is TwoFactorInput.Proof.Code -> proof.value
                    is TwoFactorInput.Proof.RecoveryCode -> proof.value
                }
            if (value.isBlank()) {
                throw DataFailure.Validation(
                    mapOf(
                        "code" to FieldMessage("Enter the 6-digit code from your authenticator app."),
                        "recovery_code" to
                            FieldMessage(
                                "Enter one of the recovery codes you saved when you set up two-factor authentication.",
                            ),
                    ),
                    422,
                )
            }
            val code = input.proof is TwoFactorInput.Proof.Code
            val field = if (code) "code" else "recovery_code"
            if (code &&
                (graphemeCount(value) != 6 || value.any { it !in '0'..'9' })
            ) {
                throw validation(field, "Enter the 6-digit code from your authenticator app.")
            }
            val key = if (code) "The authentication code was not accepted. Try again." else "This recovery code was not accepted. Try another saved code."
            val challenge =
                server.twoFactor[input.challengeId]?.takeIf { it.expiresAt > seconds(now()) }
                    ?: throw validation(field, key)
            val account = server.accounts[challenge.accountID] ?: throw validation(field, key)
            val accepted = if (code) value == account.twoFactorCode else value in account.recoveryCodes
            if (!accepted) {
                update { current ->
                    current.copy(
                        twoFactor =
                            if (challenge.attempts >=
                                4
                            ) {
                                current.twoFactor - input.challengeId
                            } else {
                                val pending = current.twoFactor[input.challengeId] ?: return@update current
                                current.twoFactor +
                                    (input.challengeId to pending.copy(attempts = pending.attempts + 1))
                            },
                    )
                }
                throw validation(field, key)
            }
            update { valueState ->
                valueState
                    .copy(
                        twoFactor = valueState.twoFactor - input.challengeId,
                    ).updateAccount(challenge.accountID) {
                        if (code) {
                            it
                        } else {
                            it.copy(
                                recoveryCodes =
                                    it.recoveryCodes - value,
                            )
                        }
                    }
            }
            authenticate(challenge.accountID, challenge.deviceID)
        }

    override suspend fun signUp(input: RegisterInput): SessionResult =
        begin(MockMethod.SignUp) {
            limit("registration")
            device(input.deviceID, input.deviceName)
            if (input.name.isBlank()) throw validation("name", "Please fill this in.")
            if (input.name.codePointCount(0, input.name.length) >
                255
            ) {
                throw validation("name", "Use no more than :max characters.", mapOf("max" to JsonPrimitive(255)))
            }
            if ('@' !in input.email) throw validation("email", "Enter a valid email address, like name@example.com.")
            val token = input.draftToken
            val secret = input.guestSecret
            val receipt = token?.let { server.claims[it] }
            val existing = receipt?.let { server.accounts[it.accountID] }
            if (receipt != null && secret != null && receipt.isLive(seconds(now())) &&
                receipt.secretHash == MockGuestReview.hash(secret) &&
                existing != null &&
                emailKey(existing.snapshot.email) == emailKey(input.email)
            ) {
                if (existing.password != input.password ||
                    existing.twoFactorCode != null
                ) {
                    throw validation("email", "This email address is already in use.")
                }
                password(input.password, input.passwordConfirmation)
                return@begin authenticate(receipt.accountID, input.deviceID, token, secret)
            }
            if (find(input.email) != null) throw validation("email", "This email address is already in use.")
            password(input.password, input.passwordConfirmation)
            val guest = guestClaimDraft(input, server)
            val saved = guest?.let { MockTripGeneration.saved(it.draft, input.draftToken!!) }
            val accountID = id()
            var snapshot =
                mockRegistration(accountID, input.name, emailKey(input.email), input.language ?: AuthLanguage.En, now())
            if (guest !=
                null
            ) {
                snapshot =
                    snapshot.with(
                        settings =
                            snapshot.settings.with(
                                bodyClock = guest.draft.profile,
                                sleepAnswered = true,
                                planCredits =
                                    JsonObject(
                                        snapshot.settings.planCredits +
                                            (
                                                "credits" to
                                                    JsonPrimitive(
                                                        3 - (
                                                            guest.draft.review.price
                                                                ?.cost ?: 0
                                                        ),
                                                    )
                                            ),
                                    ),
                            ),
                        readiness = MockSettings.readiness(guest.draft.profile),
                    )
            }
            val account = MockServerAccount(snapshot, input.password)
            update { value ->
                guestClaimDraft(input, value)
                if (value.accounts.values.any {
                        emailKey(it.snapshot.email) == emailKey(input.email)
                    }
                ) {
                    throw validation("email", "This email address is already in use.")
                }
                var next = value.copy(accounts = value.accounts + (accountID to account))
                if (guest != null && saved != null && token != null && secret != null) {
                    next =
                        next
                            .updateAccount(accountID) {
                                it.copy(
                                    trips = listOf(saved.first),
                                    details =
                                        it.details + (token to saved.second),
                                )
                            }.copy(
                                wizardInputs = next.wizardInputs.orEmpty() + ("$accountID:$token" to saved.third),
                                wizardPaidDirections =
                                    next.wizardPaidDirections.orEmpty() +
                                        (
                                            "$accountID:$token" to
                                                if ((
                                                        guest.draft.review.price
                                                            ?.cost ?: 0
                                                    ) >
                                                    0
                                                ) {
                                                    listOf(TripDirection.Outbound)
                                                } else {
                                                    emptyList()
                                                }
                                        ),
                                claims =
                                    next.claims +
                                        (
                                            token to
                                                MockClaim.of(
                                                    accountID,
                                                    token,
                                                    secret,
                                                    guest.deviceID,
                                                    seconds(now()) + 7 * 86400,
                                                )
                                        ),
                                guestDrafts = next.guestDrafts?.minus(token),
                            )
                }
                next
            }
            sendVerification(accountID)
            authenticate(accountID, input.deviceID, input.draftToken, input.guestSecret)
        }

    private fun guestClaimDraft(
        input: RegisterInput,
        state: MockServerState,
    ): MockGuestDraft? {
        if (input.draftToken == null && input.guestSecret == null && input.inputRevision == null &&
            input.reviewRevision == null
        ) {
            return null
        }
        val guestSecret = input.guestSecret
        val guest = input.draftToken?.let { state.guestDrafts?.get(it) }
        if (input.draftToken == null || guestSecret == null || (input.inputRevision ?: 0) <= 0 ||
            (input.reviewRevision ?: 0) <= 0 ||
            guest == null ||
            guest.deviceID != input.deviceID.lowercase(Locale.ROOT) ||
            guest.secretHash != MockGuestReview.hash(guestSecret) ||
            guest.draft.expiresAt <= seconds(now()) ||
            guest.draft.review.identity.inputRevision != input.inputRevision ||
            guest.draft.review.identity.reviewRevision != input.reviewRevision ||
            !guest.draft.review.canSave
        ) {
            throw validation("review_expired", "Review your trip again before creating your account.")
        }
        return guest
    }

    override suspend fun requestPasswordReset(email: String): Unit =
        begin(MockMethod.RequestPasswordReset) {
            limit("forgot")
            if (email.isBlank()) throw validation("email", "Please fill this in.")
            if ('@' !in email) throw validation("email", "Enter a valid email address, like name@example.com.")
            val accountID = find(email) ?: return@begin
            val normalized =
                server.accounts
                    .getValue(accountID)
                    .snapshot.email
            val token = id()
            val time = seconds(now())
            update {
                it.copy(
                    resets = it.resets + (accountID to MockReset(token, time + 3600)),
                    mail =
                        it.mail +
                            MockMail.PasswordReset(
                                id(),
                                normalized,
                                time,
                                ResetPasswordInput(token, normalized, "", ""),
                            ),
                )
            }
        }

    override suspend fun resetPassword(input: ResetPasswordInput): Unit =
        begin(MockMethod.ResetPassword) {
            limit("reset")
            password(input.password, input.passwordConfirmation)
            val accountID = find(input.email)
            val link = accountID?.let { server.resets[it] }
            if (accountID == null || link == null || link.token != input.token ||
                link.expiresAt <= seconds(now())
            ) {
                throw validation("token", "This password reset link is invalid or has expired.")
            }
            update {
                it.updateAccount(accountID) { account -> account.copy(password = input.password) }.copy(
                    resets =
                        it.resets - accountID,
                    sessions =
                        it.sessions.filterValues { session ->
                            session.accountID != accountID
                        },
                    twoFactor =
                        it.twoFactor.filterValues { challenge ->
                            challenge.accountID !=
                                accountID
                        },
                )
            }
        }

    private fun verification(
        accountID: String,
        identityOnly: Boolean = false,
    ): EmailVerificationStatus {
        val account = server.accounts.getValue(accountID)
        return EmailVerificationStatus(
            account.snapshot.email,
            account.snapshot.hasVerifiedEmail,
            if (identityOnly ||
                account.snapshot.hasVerifiedEmail
            ) {
                0
            } else {
                maxOf(0, ceil(account.resendAvailableAt - seconds(now())).toInt())
            },
        )
    }

    override suspend fun verificationStatus(): EmailVerificationStatus =
        begin(MockMethod.VerificationStatus) {
            verification(accountID(), true)
        }

    override suspend fun resendVerificationEmail(): EmailVerificationStatus =
        begin(MockMethod.ResendVerificationEmail) {
            val accountID = accountID()
            val status = verification(accountID)
            if (status.resendAfterSeconds > 0) throw DataFailure.Throttled(status.resendAfterSeconds.toDouble(), 429)
            if (!status.hasVerifiedEmail) sendVerification(accountID)
            verification(accountID)
        }

    private fun signature(
        id: String,
        hash: String,
        expires: Long,
    ): String =
        hmacSha256Hex(
            server.signingKey.encodeToByteArray(),
            "https://app.chronoflow.net/email/verify/$id/$hash?expires=$expires",
        )

    private fun emailHash(email: String): String = sha1Hex(email)

    private fun sendVerification(accountID: String) {
        val email =
            server.accounts
                .getValue(accountID)
                .snapshot.email
        val expires = now().epochSecond + 3600
        val hash = emailHash(email)
        val input = SignedVerificationInput(accountID, hash, expires, signature(accountID, hash, expires))
        update {
            it.updateAccount(accountID) { account -> account.copy(resendAvailableAt = seconds(now()) + 60) }.copy(
                mail =
                    it.mail + MockMail.Verification(id(), email, seconds(now()), input),
            )
        }
    }

    override suspend fun verifyEmail(input: SignedVerificationInput): EmailVerificationStatus =
        begin(MockMethod.VerifyEmail) {
            val accountID = accountID()
            val account = server.accounts.getValue(accountID)
            if (input.id != accountID || input.hash != emailHash(account.snapshot.email) ||
                input.expires < now().epochSecond ||
                input.signature != signature(input.id, input.hash, input.expires)
            ) {
                throw validation("signature", "This email verification link is invalid or has expired.")
            }
            update {
                it.updateAccount(
                    accountID,
                ) { accountValue -> accountValue.copy(snapshot = accountValue.snapshot.with(hasVerifiedEmail = true)) }
            }
            verification(accountID)
        }

    override suspend fun signOut(): Unit =
        begin(MockMethod.SignOut) {
            accountID()
            requestToken?.let { token -> update { it.copy(sessions = it.sessions - token) } }
            currentToken = null
            requestToken = null
        }

    override suspend fun fetchIdentity(): AccountIdentity =
        begin(MockMethod.FetchIdentity) {
            server.accounts.getValue(accountID()).identity
        }

    override suspend fun fetchAccount(): AccountSnapshot =
        begin(MockMethod.FetchAccount) {
            server.accounts.getValue(accountID(true)).snapshot
        }

    override suspend fun fetchTrips(): List<TripListItem> =
        begin(MockMethod.FetchTrips) {
            val owner = accountID(true)
            if (remoteTripUpdate()) {
                update {
                    it.updateAccount(owner) { account ->
                        if (account.trips.isEmpty()) {
                            account
                        } else {
                            val first = account.trips.first()
                            val flight = if (first.card.flightNumber == "CF123") "CF456" else "CF123"
                            val tripID = first.card.id
                            val detail =
                                account.details[tripID]?.let { value ->
                                    value.copy(
                                        trip =
                                            value.trip.copy(
                                                legs =
                                                    value.trip.legs.mapIndexed { index, leg ->
                                                        if (index ==
                                                            0
                                                        ) {
                                                            leg.copy(flightNumber = flight)
                                                        } else {
                                                            leg
                                                        }
                                                    },
                                            ),
                                        detailVersion = "refresh-$flight",
                                    )
                                }
                            val trip =
                                first.copy(
                                    card = first.card.copy(flightNumber = flight),
                                    detailVersion =
                                        detail?.detailVersion ?: first.detailVersion,
                                )
                            account.copy(
                                trips = listOf(trip) + account.trips.drop(1),
                                details =
                                    if (detail !=
                                        null
                                    ) {
                                        account.details + (tripID to detail)
                                    } else {
                                        account.details
                                    },
                            )
                        }
                    }
                }
            }
            server.accounts
                .getValue(owner)
                .trips
                .toList()
        }

    suspend fun replaceRemoteTrips(
        trips: List<TripListItem>,
        details: Map<String, TripDetail>,
    ): Unit =
        withContext(io) {
            mutex.withLock {
                load()
                val owner = accountID(true)
                update {
                    it.updateAccount(
                        owner,
                    ) { account -> account.copy(trips = trips.toList(), details = details.toMap()) }
                }
            }
        }

    override suspend fun fetchTripDetail(id: String): TripDetailResponse =
        begin(MockMethod.FetchTripDetail) {
            val detail = server.accounts.getValue(accountID(true)).details[id] ?: throw DataFailure.Server(404)
            TripDetailResponse.encoding(detail)
        }

    override suspend fun passkeyOptions(): ApiPasskeyChallenge =
        begin(MockMethod.PasskeyOptions) {
            limit("passkey-options", 10)
            val material = id() + id()
            val challengeID = sha256Hex(material)
            val result =
                ApiPasskeyChallenge(
                    challengeID,
                    ApiPasskeyRequestOptions(
                        base64Url(sha256Bytes("challenge:$material")),
                        "app.chronoflow.net",
                        60000,
                        UserVerification.Required,
                        emptyList(),
                    ),
                )
            update { it.copy(passkeys = it.passkeys + (challengeID to MockPasskey(result, seconds(now()) + 120))) }
            result
        }

    private fun validateAssertion(credential: ApiPasskeyAssertion) {
        val r = credential.response
        val fields =
            listOf(credential.id, credential.rawId, r.clientDataJSON, r.authenticatorData, r.signature) +
                listOfNotNull(r.userHandle)

        fun validURL(value: String): Boolean =
            value.isNotEmpty() &&
                value.all {
                    it in "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"
                } &&
                base64URL(value) != null
        val raw = base64URL(credential.rawId)
        val authenticator = base64URL(r.authenticatorData)
        val signature = base64URL(r.signature)
        if (fields.any { graphemeCount(it) > 8192 } || !validURL(credential.id) || !validURL(r.clientDataJSON) ||
            (raw == null || raw.isEmpty()) ||
            !raw.contentEquals(base64URL(credential.id)) ||
            (authenticator == null || authenticator.isEmpty()) ||
            (signature == null || signature.isEmpty()) ||
            !(r.userHandle == null || r.userHandle == "" || r.userHandle?.let { base64URL(it) } != null)
        ) {
            throw validation("credential", "We couldn't sign you in with that passkey. Please try again.")
        }
    }

    override suspend fun passkeyLogin(input: ApiPasskeyLoginInput): SessionResult =
        begin(MockMethod.PasskeyLogin) {
            val hash = sha256Hex(input.credential.id)
            limit("passkey-login:$hash")
            limit("passkey-login-ip", 20)
            server.rateWindows["passkey-failed:$hash"]
                ?.takeIf {
                    it.expiresAt > seconds(now()) && it.count >= 20
                }?.let { throw DataFailure.Throttled(it.expiresAt - seconds(now()), 429) }
            try {
                device(input.deviceID, input.deviceName)
                if (input.challengeId.isEmpty()) throw validation("challengeId", "Please fill this in.")
                if (graphemeCount(input.challengeId) >
                    64
                ) {
                    throw validation(
                        "challengeId",
                        "Use no more than :max characters.",
                        mapOf("max" to JsonPrimitive(64)),
                    )
                }
                for ((field, value, maximum) in listOf(
                    Triple("draft_token", input.draftToken, 64),
                    Triple("guest_secret", input.guestSecret, 128),
                )) {
                    if (value != null &&
                        graphemeCount(value) > maximum
                    ) {
                        throw validation(
                            field,
                            "Use no more than :max characters.",
                            mapOf("max" to JsonPrimitive(maximum)),
                        )
                    }
                }
                validateAssertion(input.credential)
                val stored =
                    server.passkeys[input.challengeId]?.takeIf { it.expiresAt > seconds(now()) }
                        ?: throw validation("challengeId", "The passkey request expired. Please try again.")
                update { it.copy(passkeys = it.passkeys - input.challengeId) }
                val accountID =
                    assertionProvider(input.credential, stored.challenge)?.takeIf { it in server.accounts }
                        ?: throw validation(
                            "credential",
                            "We couldn't sign you in with that passkey. Please try again.",
                        )
                authenticate(accountID, input.deviceID, input.draftToken, input.guestSecret)
            } catch (failure: DataFailure) {
                if (failure is DataFailure.Validation) limit("passkey-failed:$hash", 20, 3600.0)
                throw failure
            }
        }

    override suspend fun send(change: OutboxChange): MutationReceipt =
        begin(MockMethod.Send) {
            if (change is OutboxChange.Unsupported) throw DataFailure.Unsupported(change.kind)
            val accountID = accountID(change !is OutboxChange.Profile)
            when (change) {
                is OutboxChange.Profile -> {
                    val input = change.change
                    if (input.name.isBlank()) throw validation("name", "Please fill this in.")
                    if (emailKey(input.email) !=
                        server.accounts
                            .getValue(accountID)
                            .snapshot.email
                    ) {
                        throw validation("current_password", "Please fill this in.")
                    }
                    update {
                        it.updateAccount(
                            accountID,
                        ) { account -> account.copy(snapshot = account.snapshot.with(name = input.name)) }
                    }
                    MutationReceipt.Identity(server.accounts.getValue(accountID).identity)
                }

                is OutboxChange.Policies -> {
                    val policies =
                        server.accounts
                            .getValue(accountID)
                            .snapshot.settings.policies
                    if (change.version !=
                        policies.termsVersion
                    ) {
                        throw validation("version", "The terms have changed. Read the current version and agree again.")
                    }
                    update {
                        it.updateAccount(
                            accountID,
                        ) { account ->
                            account.copy(
                                snapshot =
                                    account.snapshot.with(
                                        settings =
                                            account.snapshot.settings.with(
                                                policies =
                                                    AccountPolicies(
                                                        policies.termsVersion,
                                                        policies.privacyVersion,
                                                        change.version,
                                                        iso(now()),
                                                    ),
                                            ),
                                    ),
                            )
                        }
                    }
                    MutationReceipt.Acknowledged
                }

                OutboxChange.Walkthrough -> {
                    update { it.updateAccount(accountID) { account -> account.copy(walkthroughSeen = true) } }
                    MutationReceipt.Acknowledged
                }

                is OutboxChange.Settings -> {
                    MockSettings.validate(
                        change.section,
                        change.patch,
                        server.accounts
                            .getValue(accountID)
                            .snapshot.settings,
                    )
                    update {
                        it.updateAccount(accountID) { account ->
                            val settings = MockSettings.apply(change.patch, change.section, account.snapshot.settings)
                            account.copy(
                                snapshot =
                                    account.snapshot.with(
                                        settings = settings,
                                        readiness =
                                            if (change.section ==
                                                SettingsSection.BodyClock
                                            ) {
                                                MockSettings.readiness(settings.bodyClock)
                                            } else {
                                                account.snapshot.readiness
                                            },
                                    ),
                            )
                        }
                    }
                    MutationReceipt.Account(server.accounts.getValue(accountID).snapshot)
                }

                is OutboxChange.CheckOff -> {
                    val input = change.change
                    if (input.ordinal !in 1..255) throw validation("ordinal", "This step cannot be checked off.")
                    if (!Regex(
                            "^\\d{4}-\\d{2}-\\d{2}$",
                        ).matches(input.localDate)
                    ) {
                        throw validation("local_date", "Choose a valid local date.")
                    }
                    var detail =
                        server.accounts.getValue(accountID).details[input.tripID] ?: throw DataFailure.Server(404)
                    if (detail.checkState.planInputHash !=
                        input.planInputHash
                    ) {
                        throw validation("form", "This plan has changed. Refresh it before marking this step.")
                    }
                    val key = CheckKey(input.type, input.localDate, input.ordinal)
                    if (detail.plan.blocks.none {
                            it.actionable && it.checkKey == key
                        }
                    ) {
                        throw validation("form", "This plan has changed. Refresh it before marking this step.")
                    }
                    if (detail.checkState.marks
                            .firstOrNull { it.key == key }
                            ?.status ==
                        input.status
                    ) {
                        return@begin MutationReceipt.Checks(TripCheckResult(detail.checkState, detail.detailVersion))
                    }
                    val marks =
                        detail.checkState.marks.filter { it.key != key } +
                            listOfNotNull(input.status?.let { CheckMark(key, it, iso(now())) })
                    detail = detail.copy(checkState = detail.checkState.copy(marks = marks), detailVersion = id())
                    val saved = detail
                    update {
                        it.updateAccount(accountID) { account ->
                            account.copy(
                                details =
                                    account.details + (input.tripID to saved),
                                trips =
                                    account.trips.map { trip ->
                                        if (trip.card.id ==
                                            input.tripID
                                        ) {
                                            trip.copy(
                                                checkState = saved.checkState,
                                                detailVersion = saved.detailVersion,
                                            )
                                        } else {
                                            trip
                                        }
                                    },
                            )
                        }
                    }
                    MutationReceipt.Checks(TripCheckResult(detail.checkState, detail.detailVersion))
                }

                is OutboxChange.Unsupported -> {
                    throw DataFailure.Unsupported(change.kind)
                }
            }
        }

    override suspend fun reviewGuestTrip(input: ApiGuestTripReviewInput): ApiTripReview =
        begin(MockMethod.ReviewGuestTrip) {
            val device = input.deviceID.trim().lowercase(Locale.ROOT)
            val time = seconds(now())
            limit("guest-review:$device")
            if (!validUUID(device)) throw validation("device_id", "Choose one of the available options.")
            val live = server.guestDrafts.orEmpty().filterValues { it.draft.expiresAt > time }
            val suppliedHash = input.guestSecret?.let(MockGuestReview::hash)
            val previousIdentity = input.identity
            if (previousIdentity != null) {
                val previous = live[previousIdentity.token]
                if (previous == null || previous.deviceID != device || previous.secretHash != suppliedHash ||
                    previous.draft.review.identity.inputRevision != previousIdentity.inputRevision ||
                    previous.draft.review.identity.reviewRevision != previousIdentity.reviewRevision
                ) {
                    throw DataFailure.ReviewExpired
                }
            }
            val known =
                suppliedHash?.let { hash ->
                    live.values.any { it.deviceID == device && it.secretHash == hash } ||
                        server.claims.values.any { it.deviceID == device && it.secretHash == hash && it.isLive(time) }
                }
                    ?: false
            val minted = if (previousIdentity == null && !known) MockGuestReview.secret() else null
            val secretHash = minted?.let(MockGuestReview::hash) ?: checkNotNull(suppliedHash)
            val identity =
                ApiReviewIdentity(
                    previousIdentity?.token ?: id(),
                    (previousIdentity?.inputRevision ?: 0) + 1,
                    (previousIdentity?.reviewRevision ?: 0) + 1,
                    null,
                    null,
                )
            val prepared: Pair<ApiTripInput, AccountSettings>
            var review: ApiTripReview
            try {
                prepared = MockGuestReview.prepare(input, now())
                review = MockTripGeneration.review(prepared.first, identity, prepared.second, false, emptyList())
            } catch (failure: DataFailure) {
                throw TripWizardErrors.map(failure, input.input.outbound.legs.size)
            }
            review =
                review.copy(
                    prompts =
                        review.prompts.filter {
                            it.fields == listOf("age_band") ||
                                it.fields == listOf("shift_work")
                        },
                )
            val storedReview = review
            update { stateValue ->
                var drafts = stateValue.guestDrafts.orEmpty().filterValues { it.draft.expiresAt > time }
                if (previousIdentity != null &&
                    drafts[previousIdentity.token] != live[previousIdentity.token]
                ) {
                    throw DataFailure.ReviewExpired
                }
                drafts =
                    drafts +
                    (
                        identity.token to
                            MockGuestDraft(
                                device,
                                secretHash,
                                MockWizardDraft(
                                    "",
                                    null,
                                    prepared.first,
                                    storedReview,
                                    prepared.second.bodyClock,
                                    time + 1800,
                                ),
                            )
                    )
                val deviceDrafts =
                    drafts.entries
                        .filter {
                            it.value.deviceID == device
                        }.sortedWith(compareBy({ it.value.draft.expiresAt }, { it.key }))
                drafts = drafts - deviceDrafts.take(maxOf(0, deviceDrafts.size - 5)).map { it.key }.toSet()
                stateValue.copy(guestDrafts = drafts)
            }
            review.copy(identity = review.identity.copy(guestSecret = minted))
        }

    override suspend fun searchAirports(
        query: String,
        limit: Int?,
    ): List<ApiAirportSuggestion> =
        begin(MockMethod.SearchAirports) {
            val trimmed = query.trim()
            val count = limit ?: 8
            if (graphemeCount(query) > 60 || graphemeCount(trimmed) !in 2..60 ||
                count !in 1..20
            ) {
                throw validation(
                    if (graphemeCount(trimmed) in
                        2..60
                    ) {
                        "limit"
                    } else {
                        "query"
                    },
                    "Choose one of the available options.",
                )
            }
            MockTripGeneration
                .airports()
                .filter { airport ->
                    listOf(
                        airport.iata,
                        airport.name,
                        airport.city ?: "",
                        airport.country,
                    ).any { it.contains(trimmed, ignoreCase = true) }
                }.take(count)
        }

    override suspend fun tripInput(id: String): ApiTripInputSnapshot =
        begin(MockMethod.TripInput) {
            val owner = accountID(true)
            server.wizardInputs?.get("$owner:$id")
                ?: MockTripGeneration.snapshot(
                    server.accounts.getValue(owner).details[id] ?: throw DataFailure.Server(404),
                )
        }

    override suspend fun reviewTrip(input: ApiTripReviewInput): ApiTripReview =
        begin(MockMethod.ReviewTrip) {
            wizardReview(input, null, null)
        }

    override suspend fun reviewTripEdit(
        input: ApiTripReviewInput,
        id: String,
        sourceRevision: String,
    ): ApiTripReview =
        begin(MockMethod.ReviewTripEdit) {
            wizardReview(input, id, sourceRevision)
        }

    private fun wizardReview(
        input: ApiTripReviewInput,
        tripID: String?,
        sourceRevision: String?,
    ): ApiTripReview {
        val owner = accountID(true)
        val time = seconds(now())
        limit("review:$owner", 30)
        if (tripID != null) {
            if (server.accounts[owner]?.details?.get(tripID) == null) throw DataFailure.Server(404)
            val revision =
                server.wizardInputs?.get("$owner:$tripID")?.sourceRevision
                    ?: server.accounts
                        .getValue(owner)
                        .details[tripID]
                        ?.plan
                        ?.meta
                        ?.inputHash
            if (revision != sourceRevision) throw DataFailure.ReviewExpired
        }
        val previousIdentity = input.identity
        if (previousIdentity != null) {
            val previous = server.wizardDrafts?.get(previousIdentity.token)
            if (previous == null || previous.owner != owner || previous.tripID != tripID || previous.expiresAt <= time ||
                previous.review.identity.inputRevision != previousIdentity.inputRevision ||
                previous.review.identity.reviewRevision != previousIdentity.reviewRevision
            ) {
                throw DataFailure.ReviewExpired
            }
        }
        var settings =
            server.accounts
                .getValue(owner)
                .snapshot.settings
        val profile = input.profile
        if (!profile.isNullOrEmpty()) {
            try {
                MockSettings.validate(SettingsSection.BodyClock, profile, settings)
            } catch (failure: DataFailure) {
                if (failure is DataFailure.Validation) {
                    throw TripWizardErrors.map(
                        DataFailure.Validation(
                            failure.fields.mapKeys {
                                "profile." +
                                    it.key
                            },
                            failure.status,
                        ),
                        input.input.outbound.legs.size,
                    )
                }
                throw failure
            }
            settings =
                settings.with(
                    bodyClock = JsonObject(settings.bodyClock + profile.mapKeys { MockSettings.camel(it.key) }),
                    sleepAnswered =
                        settings.sleepAnswered ||
                            profile.keys.any {
                                it in
                                    listOf(
                                        "habitual_bed",
                                        "habitual_wake",
                                        "free_day_status",
                                        "free_day_bed",
                                        "free_day_wake",
                                        "chronotype",
                                    )
                            },
                )
        }
        val identity =
            ApiReviewIdentity(
                previousIdentity?.token ?: id(),
                (previousIdentity?.inputRevision ?: 0) + 1,
                (previousIdentity?.reviewRevision ?: 0) + 1,
                sourceRevision,
                null,
            )
        val review =
            try {
                MockTripGeneration.review(
                    input.input,
                    identity,
                    settings,
                    tripID != null,
                    wizardPaidDirections(owner, tripID),
                )
            } catch (
                failure: DataFailure,
            ) {
                throw TripWizardErrors.map(failure, input.input.outbound.legs.size)
            }
        val finalSettings = settings
        update { stateValue ->
            stateValue
                .updateAccount(owner) {
                    it.copy(
                        snapshot =
                            it.snapshot.with(
                                settings = finalSettings,
                                readiness = MockSettings.readiness(finalSettings.bodyClock),
                            ),
                    )
                }.copy(
                    wizardDrafts =
                        stateValue.wizardDrafts.orEmpty().filterValues { it.expiresAt > time } +
                            (
                                identity.token to
                                    MockWizardDraft(
                                        owner,
                                        tripID,
                                        input.input,
                                        review,
                                        finalSettings.bodyClock,
                                        time + 1800,
                                    )
                            ),
                )
        }
        return review
    }

    override suspend fun saveTrip(identity: ApiTripSaveInput): ApiTripSaveResult =
        begin(MockMethod.SaveTrip) {
            wizardSave(identity, null, null)
        }

    override suspend fun updateTrip(
        id: String,
        identity: ApiTripSaveInput,
        sourceRevision: String,
    ): ApiTripSaveResult =
        begin(MockMethod.UpdateTrip) {
            wizardSave(identity, id, sourceRevision)
        }

    private fun wizardSave(
        identity: ApiTripSaveInput,
        tripID: String?,
        sourceRevision: String?,
    ): ApiTripSaveResult {
        val owner = accountID(true)
        val time = seconds(now())
        val intent =
            MockTripGeneration.canonical(
                ApiJson.response.encodeToJsonElement(ApiTripSaveInput.serializer(), identity),
            ) +
                ":" +
                (tripID ?: "") +
                ":" +
                (sourceRevision ?: "")
        val key = "$owner:${identity.draftToken}"
        server.wizardReceipts?.get(key)?.takeIf { it.expiresAt > time }?.let {
            if (it.intent != intent ||
                server.accounts.getValue(owner).details[it.tripID] == null
            ) {
                throw DataFailure.ReviewExpired
            }
            return ApiTripSaveResult(it.tripID)
        }
        val draft = server.wizardDrafts?.get(identity.draftToken)
        if (draft == null || draft.owner != owner || draft.tripID != tripID || draft.expiresAt <= time ||
            draft.review.identity.inputRevision != identity.inputRevision ||
            draft.review.identity.reviewRevision != identity.reviewRevision ||
            draft.profile !=
            server.accounts
                .getValue(owner)
                .snapshot.settings.bodyClock ||
            (draft.review.status != PlanStatus.Ok && draft.review.status != PlanStatus.Modified)
        ) {
            throw DataFailure.ReviewExpired
        }
        if (tripID != null) {
            val revision =
                server.wizardInputs?.get("$owner:$tripID")?.sourceRevision
                    ?: server.accounts
                        .getValue(owner)
                        .details[tripID]
                        ?.plan
                        ?.meta
                        ?.inputHash
            if (revision != sourceRevision ||
                draft.review.identity.sourceRevision != sourceRevision
            ) {
                throw DataFailure.ReviewExpired
            }
        }
        val currentReview =
            MockTripGeneration.review(
                draft.input,
                draft.review.identity,
                server.accounts
                    .getValue(owner)
                    .snapshot.settings,
                tripID != null,
                wizardPaidDirections(owner, tripID),
            )
        if (!currentReview.canSave) throw DataFailure.NotEnoughCredits
        limit("trip-save:$owner", 10)
        val id = tripID ?: identity.draftToken
        var (item, detail, input) = MockTripGeneration.saved(draft, id)
        val previous = tripID?.let { server.accounts.getValue(owner).details[it] }
        if (previous != null) {
            detail =
                detail.copy(
                    checkState =
                        detail.checkState.copy(
                            marks =
                                previous.checkState.marks.filter { mark ->
                                    detail.plan.blocks.any {
                                        it.actionable &&
                                            it.checkKey == mark.key
                                    }
                                },
                        ),
                )
            item = item.copy(checkState = detail.checkState)
        }
        val previousPaid = wizardPaidDirections(owner, tripID)
        val savedItem = item
        val savedDetail = detail
        update { stateValue ->
            val directions =
                if (draft.input.`return` ==
                    null
                ) {
                    listOf(TripDirection.Outbound)
                } else {
                    listOf(TripDirection.Outbound, TripDirection.Return)
                }
            stateValue
                .updateAccount(owner) { account ->
                    account.copy(
                        trips =
                            account.trips.filter { it.card.id != id } + savedItem,
                        details = account.details + (id to savedDetail),
                        snapshot =
                            account.snapshot.with(
                                settings =
                                    account.snapshot.settings.with(
                                        planCredits =
                                            JsonObject(
                                                account.snapshot.settings.planCredits +
                                                    (
                                                        "credits" to
                                                            JsonPrimitive(
                                                                (currentReview.price?.balance ?: 0) -
                                                                    (currentReview.price?.cost ?: 0),
                                                            )
                                                    ),
                                            ),
                                    ),
                            ),
                    )
                }.copy(
                    wizardReceipts =
                        stateValue.wizardReceipts.orEmpty() + (key to MockWizardReceipt(intent, id, time + 86400)),
                    wizardDrafts = stateValue.wizardDrafts?.minus(identity.draftToken),
                    wizardInputs = stateValue.wizardInputs.orEmpty() + ("$owner:$id" to input),
                    wizardPaidDirections =
                        stateValue.wizardPaidDirections.orEmpty() +
                            (
                                "$owner:$id" to
                                    (
                                        previousPaid +
                                            directions.filter {
                                                it !in currentReview.price?.freeDirections.orEmpty()
                                            }
                                    ).distinct()
                            ),
                )
        }
        return ApiTripSaveResult(id)
    }

    private fun wizardPaidDirections(
        owner: String,
        tripID: String?,
    ): List<TripDirection> {
        if (tripID == null) return emptyList()
        server.wizardPaidDirections?.get("$owner:$tripID")?.let { return it }
        return if (server.accounts[owner]
                ?.details
                ?.get(tripID)
                ?.trip
                ?.legs
                ?.any { it.returnLeg } ==
            true
        ) {
            listOf(TripDirection.Outbound, TripDirection.Return)
        } else {
            listOf(TripDirection.Outbound)
        }
    }

    override suspend fun deleteAccount(password: String): Unit =
        begin(MockMethod.DeleteAccount) {
            val id = accountID(true)
            limit("credential:$id", 6)
            if (server.accounts[id]?.password !=
                password
            ) {
                throw validation("password", "The provided password was incorrect.")
            }
            update {
                it.copy(
                    accounts = it.accounts - id,
                    sessions =
                        it.sessions.filterValues { value ->
                            value.accountID !=
                                id
                        },
                )
            }
            currentToken = null
            requestToken = null
        }

    override suspend fun accountSecurity(change: AccountSecurityChange): AccountSecurityResult {
        // The mutex is non-reentrant; the delegated request starts its own begin after these checks.
        if (change is AccountSecurityChange.RequestPasswordReset) {
            begin(MockMethod.AccountSecurity) {
                val accountID = accountID(true)
                limit("credential:$accountID", 6)
            }
            requestPasswordReset(change.email)
            return AccountSecurityResult.Changed
        }
        return begin(MockMethod.AccountSecurity) {
            val accountID = accountID(true)
            if (change is AccountSecurityChange.ListPasskeys || change is AccountSecurityChange.PasskeyCreationOptions ||
                change is AccountSecurityChange.AddPasskey ||
                change is AccountSecurityChange.RemovePasskey
            ) {
                return@begin passkeySecurity(change, accountID)
            }
            if (change is AccountSecurityChange.ConfirmTwoFactor) {
                limit(
                    "two-factor-confirm:$accountID",
                    5,
                )
            } else {
                limit("credential:$accountID", 6)
            }
            val current =
                when (change) {
                    is AccountSecurityChange.Password -> change.current
                    is AccountSecurityChange.EnableTwoFactor -> change.current
                    is AccountSecurityChange.DisableTwoFactor -> change.current
                    is AccountSecurityChange.RevealRecoveryCodes -> change.current
                    is AccountSecurityChange.RegenerateRecoveryCodes -> change.current
                    else -> null
                }
            if (current != null &&
                current != server.accounts.getValue(accountID).password
            ) {
                throw validation("current_password", "The provided password was incorrect.")
            }
            val enabled =
                server.accounts
                    .getValue(accountID)
                    .snapshot.security["twoFactorEnabled"]
                    .bool == true
            when (change) {
                is AccountSecurityChange.Password -> {
                    password(change.password, change.confirmation)
                    update { it.updateAccount(accountID) { account -> account.copy(password = change.password) } }
                    AccountSecurityResult.Changed
                }

                is AccountSecurityChange.EnableTwoFactor -> {
                    if (enabled) {
                        throw DataFailure.Validation(
                            mapOf("form" to FieldMessage("Two-factor authentication is already on.")),
                            409,
                        )
                    }
                    pendingTwoFactor = pendingTwoFactor + accountID
                    AccountSecurityResult.Setup(
                        ApiTwoFactorSetup(
                            "JBSWY3DPEHPK3PXP",
                            "otpauth://totp/ChronoFlow?secret=JBSWY3DPEHPK3PXP&issuer=ChronoFlow",
                        ),
                    )
                }

                is AccountSecurityChange.ConfirmTwoFactor -> {
                    if (enabled) throw validation("form", "Two-factor authentication is already on.")
                    if (accountID !in
                        pendingTwoFactor
                    ) {
                        throw DataFailure.Validation(
                            mapOf("form" to FieldMessage("Start two-factor setup again.")),
                            409,
                        )
                    }
                    if (change.code !=
                        "123456"
                    ) {
                        throw validation("code", "The authentication code was not accepted. Try again.")
                    }
                    val timestamp = iso(now())
                    val codes = recoveryCodes()
                    update {
                        it.updateAccount(accountID) { account ->
                            account.copy(
                                twoFactorCode = "123456",
                                snapshot =
                                    account.snapshot.with(
                                        security =
                                            JsonObject(
                                                account.snapshot.security +
                                                    mapOf(
                                                        "twoFactorEnabled" to JsonPrimitive(true),
                                                        "twoFactorConfirmedAt" to JsonPrimitive(timestamp),
                                                    ),
                                            ),
                                    ),
                                recoveryCodes = codes.toSet(),
                            )
                        }
                    }
                    pendingTwoFactor = pendingTwoFactor - accountID
                    AccountSecurityResult.Codes(ApiRecoveryCodes(codes))
                }

                is AccountSecurityChange.DisableTwoFactor -> {
                    update {
                        it.updateAccount(accountID) { account ->
                            account.copy(
                                twoFactorCode = null,
                                snapshot =
                                    account.snapshot.with(
                                        security =
                                            JsonObject(
                                                account.snapshot.security +
                                                    mapOf(
                                                        "twoFactorEnabled" to JsonPrimitive(false),
                                                        "twoFactorConfirmedAt" to JsonNull,
                                                    ),
                                            ),
                                    ),
                                recoveryCodes = emptySet(),
                            )
                        }
                    }
                    AccountSecurityResult.Snapshot(server.accounts.getValue(accountID).snapshot)
                }

                is AccountSecurityChange.RevealRecoveryCodes, is AccountSecurityChange.RegenerateRecoveryCodes -> {
                    if (!enabled) {
                        throw DataFailure.Validation(
                            mapOf("form" to FieldMessage("Two-factor authentication is off.")),
                            409,
                        )
                    }
                    if (change is AccountSecurityChange.RegenerateRecoveryCodes) {
                        val codes = recoveryCodes().toSet()
                        update { it.updateAccount(accountID) { account -> account.copy(recoveryCodes = codes) } }
                    }
                    AccountSecurityResult.Codes(
                        ApiRecoveryCodes(
                            server.accounts
                                .getValue(accountID)
                                .recoveryCodes
                                .sorted(),
                        ),
                    )
                }

                else -> {
                    throw DataFailure.Server(null)
                }
            }
        }
    }

    private fun recoveryCodes(): List<String> =
        (0 until 8).map {
            UUID
                .randomUUID()
                .toString()
                .take(8)
                .lowercase(Locale.ROOT) +
                "-$it"
        }

    private fun passkeySecurity(
        change: AccountSecurityChange,
        accountID: String,
    ): AccountSecurityResult {
        val time = seconds(now())
        val account = server.accounts.getValue(accountID)
        if (change ==
            AccountSecurityChange.ListPasskeys
        ) {
            return AccountSecurityResult.Passkeys(account.passkeys.orEmpty())
        }
        limit("credential:$accountID", 6)
        return when (change) {
            is AccountSecurityChange.PasskeyCreationOptions -> {
                if (change.current !=
                    account.password
                ) {
                    throw validation("current_password", "The provided password was incorrect.")
                }
                val material = id() + id()
                val challengeID = sha256Hex(material)
                val challenge = base64Url(sha256Bytes("registration:$material"))
                val token = requestToken
                update {
                    it.copy(
                        passkeyRegistrations =
                            it.passkeyRegistrations.orEmpty().filterValues { value -> value.expiresAt > time } +
                                (challengeID to MockPasskeyRegistration(accountID, challenge, time + 120, token)),
                    )
                }
                AccountSecurityResult.PasskeyCreation(
                    ApiPasskeyRegistrationChallenge(
                        challengeID,
                        ApiPasskeyCreationOptions(
                            challenge,
                            60000,
                            ApiPasskeyRelyingParty("app.chronoflow.net", "app.chronoflow.net"),
                            ApiPasskeyUserEntity(
                                base64Url("user:$accountID".encodeToByteArray()),
                                account.snapshot.email,
                                account.snapshot.name,
                            ),
                            listOf(
                                ApiPasskeyCredentialParameters(CredentialType.PublicKey, -7),
                                ApiPasskeyCredentialParameters(CredentialType.PublicKey, -257),
                            ),
                            ApiPasskeyAuthenticatorSelection(true, "required", "required"),
                            "none",
                            account.passkeyCredentialIDs.orEmpty().map {
                                ApiPasskeyCredentialDescriptor(CredentialType.PublicKey, it)
                            },
                        ),
                    ),
                )
            }

            is AccountSecurityChange.AddPasskey -> {
                val input = change.input
                val name = input.name.trim()
                val fields = linkedMapOf<String, FieldMessage>()
                for ((field, value) in listOf("challengeId" to input.challengeId, "name" to name)) {
                    val maximum = if (field == "name") 255 else 64
                    if (value.isEmpty()) {
                        fields[field] = FieldMessage("Please fill this in.")
                    } else if (graphemeCount(value) >
                        maximum
                    ) {
                        fields[field] =
                            FieldMessage("Use no more than :max characters.", mapOf("max" to JsonPrimitive(maximum)))
                    }
                }
                if (fields.isNotEmpty()) throw DataFailure.Validation(fields, 422)
                val stored = server.passkeyRegistrations?.get(input.challengeId)
                if (stored == null || stored.accountID != accountID || stored.token != requestToken ||
                    stored.expiresAt <= time
                ) {
                    throw validation("challengeId", "The passkey registration request expired. Please try again.")
                }
                update { it.copy(passkeyRegistrations = it.passkeyRegistrations?.minus(input.challengeId)) }
                val credential = input.credential

                fun valid(value: String): Boolean = base64URL(value)?.isNotEmpty() == true
                if (credential.type != CredentialType.PublicKey || !valid(credential.id) || '=' in credential.id ||
                    !valid(credential.rawId) ||
                    !base64URL(credential.rawId).contentEquals(base64URL(credential.id)) ||
                    !valid(credential.response.clientDataJSON) ||
                    '=' in credential.response.clientDataJSON ||
                    !valid(credential.response.attestationObject) ||
                    credential.id in account.passkeyCredentialIDs.orEmpty()
                ) {
                    throw validation("credential", "We couldn't register that passkey. Please try again.")
                }
                val item = ApiPasskey((account.passkeys?.maxOfOrNull { it.id } ?: 0L) + 1, name, null, iso(now()), null)
                update {
                    it.updateAccount(accountID) { value ->
                        val list = listOf(item) + value.passkeys.orEmpty()
                        value.copy(
                            passkeys = list,
                            passkeyCredentialIDs =
                                value.passkeyCredentialIDs.orEmpty() + credential.id,
                            snapshot =
                                value.snapshot.with(
                                    security =
                                        JsonObject(
                                            value.snapshot.security + ("passkeyCount" to JsonPrimitive(list.size)),
                                        ),
                                ),
                        )
                    }
                }
                AccountSecurityResult.PasskeyAdded(item)
            }

            is AccountSecurityChange.RemovePasskey -> {
                if (account.passkeys?.any { it.id == change.id } != true) throw DataFailure.Server(404)
                if (change.current !=
                    account.password
                ) {
                    throw validation("current_password", "The provided password was incorrect.")
                }
                val keep = requestToken
                update { stateValue ->
                    stateValue
                        .updateAccount(accountID) { value ->
                            val list = value.passkeys.orEmpty().filter { it.id != change.id }
                            value.copy(
                                passkeys = list,
                                snapshot =
                                    value.snapshot.with(
                                        security =
                                            JsonObject(
                                                value.snapshot.security + ("passkeyCount" to JsonPrimitive(list.size)),
                                            ),
                                    ),
                            )
                        }.copy(
                            sessions = stateValue.sessions.filter { it.value.accountID != accountID || it.key == keep },
                        )
                }
                AccountSecurityResult.Changed
            }

            else -> {
                throw DataFailure.Server(null)
            }
        }
    }
}

typealias AssertionProvider = (ApiPasskeyAssertion, ApiPasskeyChallenge) -> String?
