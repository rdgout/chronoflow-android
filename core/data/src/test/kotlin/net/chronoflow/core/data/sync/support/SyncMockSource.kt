package net.chronoflow.core.data.sync.support

import kotlinx.coroutines.CoroutineDispatcher
import net.chronoflow.core.datamock.MemoryMockStateStore
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.datamock.MockServerAccount
import net.chronoflow.core.datamock.MockServerState
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.testing.sync.card
import net.chronoflow.core.testing.sync.detail
import net.chronoflow.core.testing.sync.snapshot
import net.chronoflow.core.testing.sync.syncDate
import net.chronoflow.core.testing.sync.syncOther
import net.chronoflow.core.testing.sync.syncTrip
import java.time.ZoneId

internal suspend fun syncMockSource(
    io: CoroutineDispatcher,
    store: MemoryMockStateStore = MemoryMockStateStore(),
    remoteTripUpdate: suspend () -> Boolean = { false },
): MockDataSource {
    val account =
        MockServerAccount(
            snapshot(),
            "fixture-password",
            trips = listOf(card()),
            details =
                mapOf(syncTrip to detail()),
        )
    val source =
        MockDataSource(
            store = store,
            seed = MockServerState(listOf(account), "fixture-signing-key"),
            remoteTripUpdate = remoteTripUpdate,
            now = { syncDate },
            sleep = { },
            id = { syncOther },
            zone = ZoneId.of("Europe/Amsterdam"),
            io = io,
        )
    source.signIn(PasswordSignInInput("demo@example.com", "fixture-password", syncOther, "Test"))
    return source
}
