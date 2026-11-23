package net.chronoflow.feature.auth.model

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import net.chronoflow.core.datamock.MemoryMockStateStore
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.datamock.MockServerAccount
import net.chronoflow.core.datamock.MockServerState
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.api.AccountSettings
import net.chronoflow.core.model.api.AccountSnapshot
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.testing.Fixtures
import net.chronoflow.feature.auth.testing.AUTH_DEVICE_ID
import net.chronoflow.feature.auth.testing.FakeSecretField
import net.chronoflow.feature.auth.testing.FakeSessionController
import net.chronoflow.feature.auth.testing.RecordingRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import java.time.Instant

const val RECOVERY_PROOF = "AbCdEf1234-GhIjKl5678"

fun twoFactorSource(
    io: CoroutineDispatcher,
    verified: Boolean = true,
    code: String = "123456",
    now: () -> Instant = { Instant.ofEpochSecond(1_800_000_000) },
    sleep: suspend (Double) -> Unit = {},
): MockDataSource {
    val original =
        ApiJson.response.decodeFromString(
            AccountSnapshot.serializer(),
            Fixtures.text("account/snapshot.json"),
        )
    val s = original.settings
    val snapshot =
        AccountSnapshot(
            original.id,
            original.name,
            "twofactor@example.com",
            verified,
            AccountSettings(
                s.bodyClock,
                s.sleepAnswered,
                s.preferences,
                s.reminders,
                s.planCredits,
                AccountPolicies("1", "1", "1"),
            ),
            original.readiness,
            original.billing,
            original.security,
        )
    return MockDataSource(
        store = MemoryMockStateStore(),
        seed =
            MockServerState(
                accounts =
                    listOf(
                        MockServerAccount(snapshot, "password", code, setOf(RECOVERY_PROOF), walkthroughSeen = true),
                    ),
            ),
        now = now,
        delay = { 0.0 },
        sleep = sleep,
        debugAppSeed = false,
        io = io,
    )
}

class TestClock {
    var instant: Instant = Instant.ofEpochSecond(1_800_000_000)

    fun now(): Instant = instant

    fun advance(seconds: Long) {
        instant = instant.plusSeconds(seconds)
    }
}

class TestGate {
    var armed = false
    val entered = CompletableDeferred<Unit>()
    val released = CompletableDeferred<Unit>()

    suspend fun wait() {
        if (!armed) return
        entered.complete(Unit)
        released.await()
    }
}

suspend fun challenged(
    source: MockDataSource,
    session: SessionAcceptor = FakeSessionController(),
    routes: RecordingRoutes = RecordingRoutes(),
): SignInModel {
    val model =
        SignInModel(
            source,
            session,
            { AUTH_DEVICE_ID },
            { "test" },
            accepted = routes::acceptedSession,
            onTwoFactor = routes::twoFactor,
            onLeaveTwoFactor = routes::backToSignIn,
        )
    model.editEmail("twofactor@example.com")
    model.submitPassword(FakeSecretField("password")) {}
    assertEquals(1, routes.actions.count { it == "twoFactor" })
    assertNotNull(model.navigation.challengeID)
    assertNotNull(model.twoFactor.value)
    return model
}
