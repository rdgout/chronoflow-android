package net.chronoflow.app.auth

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
import java.time.Instant

const val RECOVERY_PROOF = "AbCdEf1234-GhIjKl5678"

fun twoFactorTestSource(
    io: CoroutineDispatcher,
    verified: Boolean = true,
    password: String = "password",
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
                        MockServerAccount(snapshot, password, code, setOf(RECOVERY_PROOF), walkthroughSeen = true),
                    ),
            ),
        now = now,
        delay = { 0.0 },
        sleep = sleep,
        debugAppSeed = false,
        io = io,
    )
}
