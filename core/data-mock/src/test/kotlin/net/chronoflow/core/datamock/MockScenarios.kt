package net.chronoflow.core.datamock

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.AccountSnapshot
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiPasskeyAssertion
import net.chronoflow.core.model.api.ApiPasskeyAssertionResponse
import net.chronoflow.core.model.api.ApiPasskeyChallenge
import net.chronoflow.core.model.api.ApiPasskeyLoginInput
import net.chronoflow.core.model.api.AuthLanguage
import net.chronoflow.core.model.api.CredentialType
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.api.RegisterInput
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.contracts.TripListResponse
import net.chronoflow.core.testing.Fixtures
import java.time.Instant
import java.util.Locale

internal val demoID get() = MockScenarioValues.demoID
internal val twoFactorID get() = MockScenarioValues.twoFactorID
internal val demoTripID = "0199a3c0-0000-7000-8000-000000000010"
internal val mockDeviceID = "0199a3c0-0000-7000-8000-000000000100"
internal val fixturePassword get() = MockScenarioValues.password
internal val fixtureRecovery get() = MockScenarioValues.recoveryCode
internal val newPassword = "NewPassword12!"

internal class MockClock {
    private var time = Instant.ofEpochSecond(1_793_923_200)
    private var sequence = 200
    private var recordedDelays = emptyList<Double>()

    @Synchronized fun now(): Instant = time

    @Synchronized fun advance(seconds: Double) {
        time = time.plusMillis((seconds * 1000).toLong())
    }

    @Synchronized fun id(): String {
        sequence++
        return String.format(Locale.ROOT, "0199a3c0-0000-7000-8000-%012d", sequence)
    }

    suspend fun sleep(delay: Double) {
        currentCoroutineContext().ensureActive()
        record(delay)
    }

    @Synchronized private fun record(delay: Double) {
        recordedDelays = recordedDelays + delay
    }

    val delays: List<Double> @Synchronized get() = recordedDelays.toList()
}

private val decodedScenario by lazy {
    val snapshot =
        ApiJson.response.decodeFromString(
            AccountSnapshot.serializer(),
            Fixtures.text("account/snapshot.json"),
        )
    val trips = ApiJson.response.decodeFromString(TripListResponse.serializer(), Fixtures.text("trips/list.json")).trips
    val detail = ApiJson.response.decodeFromString(TripDetail.serializer(), Fixtures.text("trips/detail.json"))
    val demo = MockServerAccount(snapshot, fixturePassword, trips = trips, details = mapOf(demoTripID to detail))
    val other =
        MockServerAccount(
            snapshot.with(id = twoFactorID, email = MockScenarioValues.twoFactorEmail),
            fixturePassword,
            MockScenarioValues.twoFactorCode,
            setOf(fixtureRecovery),
        )
    MockServerState(listOf(demo, other), "artificial-mock-signing-key")
}

internal fun scenarioState(): MockServerState = decodedScenario

internal fun mockSource(
    clock: MockClock = MockClock(),
    store: MockStateStore = MemoryMockStateStore(),
    seed: MockServerState? = null,
    bearer: (suspend () -> String?)? = null,
    io: CoroutineDispatcher = Dispatchers.Unconfined,
    mutationsOffline: Boolean = false,
): MockDataSource =
    MockDataSource(
        store,
        seed ?: scenarioState(),
        mutationsOffline = mutationsOffline,
        now = clock::now,
        delay = {
            0.5
        },
        sleep = clock::sleep,
        id = clock::id,
        bearer = bearer,
        io = io,
        zone = mockZone,
        assertionProvider = { assertion, challenge ->
            if (assertion.response.signature != "c2ln" ||
                assertion.response.clientDataJSON != challenge.options.challenge
            ) {
                null
            } else {
                when (assertion.id) {
                    "AA" -> demoID
                    "AQ" -> twoFactorID
                    else -> null
                }
            }
        },
    )

internal fun passwordInput(
    email: String = MockScenarioValues.demoEmail,
    password: String = fixturePassword,
    device: String = mockDeviceID,
): PasswordSignInInput = PasswordSignInInput(email, password, device, "iPhone")

internal suspend fun login(
    source: DataSource,
    email: String = MockScenarioValues.demoEmail,
    device: String = mockDeviceID,
): SessionResult =
    (source.signIn(passwordInput(email, device = device)) as? SignInResult.Session)?.result
        ?: throw DataFailure.Server(null)

internal fun passkeyInput(
    challenge: ApiPasskeyChallenge,
    credentialID: String = "AA",
    signature: String = "c2ln",
): ApiPasskeyLoginInput =
    ApiPasskeyLoginInput(
        challenge.challengeId,
        ApiPasskeyAssertion(
            credentialID,
            if (credentialID ==
                "AA"
            ) {
                "AA=="
            } else {
                "AQ=="
            },
            CredentialType.PublicKey,
            ApiPasskeyAssertionResponse(challenge.options.challenge, "AA==", signature),
        ),
        mockDeviceID,
        "iPhone",
    )

internal fun registration(email: String = "new@example.com"): RegisterInput =
    RegisterInput("New Traveller", email, newPassword, newPassword, mockDeviceID, "iPhone", AuthLanguage.Nl)

internal suspend fun failure(block: suspend () -> Any?): DataFailure =
    try {
        block()
        error("expected DataFailure")
    } catch (failure: DataFailure) {
        failure
    }
