@file:Suppress("ktlint:standard:filename")

package net.chronoflow.core.datamock

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import net.chronoflow.core.model.api.AccountIdentity
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.api.AccountSettings
import net.chronoflow.core.model.api.AccountSnapshot
import net.chronoflow.core.model.api.ApiPasskey
import net.chronoflow.core.model.api.ApiPasskeyChallenge
import net.chronoflow.core.model.api.AuthLanguage
import net.chronoflow.core.model.contracts.ApiTripInputSnapshot
import net.chronoflow.core.model.contracts.ObjectSerializer
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.contracts.TripDirection
import net.chronoflow.core.model.contracts.TripListItem
import net.chronoflow.core.model.contracts.optionalStringAt
import net.chronoflow.core.model.contracts.stringAt
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.time.Instant
import java.util.Locale
import java.util.UUID

@Serializable
data class MockServerAccount(
    val snapshot: AccountSnapshot,
    val password: String,
    val twoFactorCode: String? = null,
    @Required val recoveryCodes: Set<String> = emptySet(),
    @Required val walkthroughSeen: Boolean = false,
    @Required val resendAvailableAt: Double = 0.0,
    @Required val trips: List<TripListItem> = emptyList(),
    @Required val details: Map<String, TripDetail> = emptyMap(),
    val passkeys: List<ApiPasskey>? = null,
    val passkeyCredentialIDs: List<String>? = null,
) {
    val identity: AccountIdentity
        get() =
            AccountIdentity(
                snapshot.id,
                snapshot.name,
                snapshot.email,
                snapshot.hasVerifiedEmail,
                snapshot.settings.policies,
                walkthroughSeen,
            )

    override fun toString(): String = "MockServerAccount(id=${snapshot.id}, credentials=***)"
}

@Serializable
data class MockServerState(
    val accounts: Map<String, MockServerAccount>,
    val signingKey: String,
    @Required val schemaVersion: Int = currentSchemaVersion,
    val seedAnchor: String? = null,
    @Required val sessions: Map<String, MockSession> = emptyMap(),
    @Required val twoFactor: Map<String, MockTwoFactor> = emptyMap(),
    @Required val passkeys: Map<String, MockPasskey> = emptyMap(),
    val passkeyRegistrations: Map<String, MockPasskeyRegistration>? = null,
    @Required val resets: Map<String, MockReset> = emptyMap(),
    @Required val rateWindows: Map<String, MockRateLimiter.Window> = emptyMap(),
    @Required val mail: List<MockMail> = emptyList(),
    val wizardDrafts: Map<String, MockWizardDraft>? = null,
    val wizardReceipts: Map<String, MockWizardReceipt>? = null,
    val wizardPaidDirections: Map<String, List<TripDirection>>? = null,
    val wizardInputs: Map<String, ApiTripInputSnapshot>? = null,
    val guestDrafts: Map<String, MockGuestDraft>? = null,
    @Required val claims: Map<String, MockClaim> = emptyMap(),
) {
    constructor(
        accounts: List<MockServerAccount> = emptyList(),
        signingKey: String = UUID.randomUUID().toString().uppercase(Locale.ROOT),
    ) : this(
        accounts.associateBy { it.snapshot.id },
        signingKey,
    )

    companion object {
        @Suppress("ktlint:standard:property-naming")
        const val currentSchemaVersion = 2
    }

    override fun toString(): String = "MockServerState(***)"
}

@Serializable(with = MockClaim.Serializer::class)
data class MockClaim(
    val accountID: String,
    val tripID: String,
    val secretHash: String,
    val deviceID: String? = null,
    val expiresAt: Double? = null,
) {
    companion object {
        fun of(
            accountID: String,
            tripID: String,
            secret: String,
            deviceID: String? = null,
            expiresAt: Double? = null,
        ): MockClaim = MockClaim(accountID, tripID, MockGuestReview.hash(secret), deviceID, expiresAt)
    }

    fun isLive(at: Double): Boolean = (expiresAt ?: Double.MAX_VALUE) > at

    override fun toString(): String = "MockClaim(accountID=$accountID, tripID=$tripID, possession=***)"

    object Serializer : ObjectSerializer<MockClaim>("net.chronoflow.core.datamock.MockClaim") {
        override fun decode(
            json: Json,
            obj: JsonObject,
        ): MockClaim =
            MockClaim(
                obj.stringAt("accountID"),
                obj.stringAt("tripID"),
                obj.optionalStringAt("secretHash") ?: MockGuestReview.hash(obj.stringAt("secret")),
                obj.optionalStringAt("deviceID"),
                obj["expiresAt"]?.takeIf { it != JsonNull }?.let {
                    (it as? JsonPrimitive)?.takeIf { p -> !p.isString }?.doubleOrNull
                        ?: throw SerializationException("expiresAt must be a number")
                },
            )

        override fun encode(
            json: Json,
            value: MockClaim,
        ): JsonObject =
            JsonObject(
                buildMap {
                    put("accountID", JsonPrimitive(value.accountID))
                    put("tripID", JsonPrimitive(value.tripID))
                    put("secretHash", JsonPrimitive(value.secretHash))
                    value.deviceID?.let { put("deviceID", JsonPrimitive(it)) }
                    value.expiresAt?.let { put("expiresAt", JsonPrimitive(it)) }
                },
            )
    }
}

@Serializable
data class MockSession(
    val accountID: String,
    val deviceID: String,
    val expiresAt: Double,
)

@Serializable
data class MockTwoFactor(
    val accountID: String,
    val deviceID: String,
    val expiresAt: Double,
    val attempts: Int,
)

@Serializable
data class MockPasskey(
    val challenge: ApiPasskeyChallenge,
    val expiresAt: Double,
) {
    override fun toString(): String = "MockPasskey(***)"
}

@Serializable
data class MockPasskeyRegistration(
    val accountID: String,
    val challenge: String,
    val expiresAt: Double,
    val token: String?,
) {
    override fun toString(): String = "MockPasskeyRegistration(***)"
}

@Serializable
data class MockReset(
    val token: String,
    val expiresAt: Double,
) {
    override fun toString(): String = "MockReset(***)"
}

interface MockStateStore {
    fun load(): MockServerState?

    fun save(state: MockServerState)
}

class MemoryMockStateStore(
    initial: MockServerState? = null,
) : MockStateStore {
    private var state = initial

    @Synchronized override fun load(): MockServerState? = state

    @Synchronized override fun save(state: MockServerState) {
        this.state = state
    }

    @Synchronized internal fun <T> update(
        initial: MockServerState,
        change: (MockServerState) -> Pair<MockServerState, T>,
    ): Pair<T, MockServerState> {
        val (candidate, result) = change(state ?: initial)
        state = candidate
        return result to candidate
    }
}

class FileMockStateStore(
    root: File,
) : MockStateStore {
    val file: File = File(root, "ChronoFlowMock/Server/state.json")

    override fun load(): MockServerState? {
        if (!file.exists()) return null
        return try {
            MockJson.decodeFromString(MockServerState.serializer(), file.readText())
        } catch (_: SerializationException) {
            // Decoder diagnostics may contain the input, including credentials.
            throw SerializationException("invalid mock server state")
        }
    }

    override fun save(state: MockServerState) {
        Files.createDirectories(file.parentFile.toPath())
        val temporary = File(file.parentFile, "state.json.tmp")
        temporary.writeText(MockJson.encodeToString(MockServerState.serializer(), state))
        Files.move(temporary.toPath(), file.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
    }
}

internal fun mockRegistration(
    id: String,
    name: String,
    email: String,
    language: AuthLanguage,
    now: Instant,
): AccountSnapshot {
    val bodyFields =
        listOf(
            "freeDayBed",
            "freeDayWake",
            "chronotype",
            "ageBand",
            "shiftWork",
            "caffeineHabit",
            "cupsFilterCoffee",
            "cupsTea",
            "cupsCola",
            "cupsEnergyDrink",
            "lightDevice",
            "blueBlockers",
            "blueBlockerMdfd",
        )
    val body =
        JsonObject(
            bodyFields.associateWith { JsonNull } +
                mapOf(
                    "habitualBed" to JsonPrimitive("23:00"),
                    "habitualWake" to JsonPrimitive("07:00"),
                    "freeDayStatus" to JsonPrimitive("unconfirmed"),
                    "caffeineSensitive" to JsonPrimitive(false),
                    "caffeineNonUserOptIn" to JsonPrimitive(false),
                    "excludeCaffeine" to JsonPrimitive(false),
                    "flightMovementBreaks" to JsonPrimitive(false),
                    "invalidDrafts" to kotlinx.serialization.json.JsonArray(emptyList()),
                ),
        )
    return AccountSnapshot(
        id,
        name,
        email,
        false,
        AccountSettings(
            bodyClock = body,
            sleepAnswered = false,
            preferences =
                JsonObject(
                    mapOf(
                        "hiddenCategories" to kotlinx.serialization.json.JsonArray(emptyList()),
                        "showOptionalItems" to JsonPrimitive(false),
                        "timeFormat" to JsonPrimitive("24"),
                        "language" to
                            JsonPrimitive(
                                when (language) {
                                    AuthLanguage.En -> "en"
                                    AuthLanguage.Nl -> "nl"
                                },
                            ),
                    ),
                ),
            reminders =
                JsonObject(
                    mapOf(
                        "light" to JsonPrimitive(true),
                        "avoid" to JsonPrimitive(true),
                        "caffeine" to JsonPrimitive(true),
                        "winddown" to JsonPrimitive(false),
                        "leadMin" to JsonPrimitive(30),
                    ),
                ),
            planCredits = JsonObject(mapOf("credits" to JsonPrimitive(3), "unlimited" to JsonPrimitive(false))),
            policies = AccountPolicies("1.0", "1.0", "1.0", iso(now)),
        ),
        readiness = MockSettings.readiness(body),
        billing =
            JsonObject(
                mapOf(
                    "subscription" to
                        JsonObject(
                            mapOf(
                                "tier" to JsonPrimitive("free"),
                                "status" to JsonPrimitive("free"),
                                "endsAt" to JsonNull,
                                "renewsAt" to JsonNull,
                                "canResume" to JsonPrimitive(false),
                                "canCancel" to JsonPrimitive(false),
                                "paymentFailed" to JsonPrimitive(false),
                                "unlimited" to JsonPrimitive(false),
                            ),
                        ),
                ),
            ),
        security =
            JsonObject(
                mapOf(
                    "twoFactorEnabled" to JsonPrimitive(false),
                    "twoFactorConfirmedAt" to JsonNull,
                    "passwordRules" to
                        JsonPrimitive(
                            "minlength: 12; required: lower; required: upper; " +
                                "required: digit; required: special;",
                        ),
                ),
            ),
    )
}
