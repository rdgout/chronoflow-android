package net.chronoflow.core.data.wizard.support

import kotlinx.coroutines.CoroutineDispatcher
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.model.api.PasswordSignInInput
import java.time.Instant
import java.time.ZoneOffset

internal suspend fun mockWizardSource(
    io: CoroutineDispatcher,
    now: Instant,
): MockDataSource {
    val seed = MockAppSeed.make(now, zone = ZoneOffset.UTC)
    val account = seed.accounts.values.first { it.twoFactorCode == null }
    val source = MockDataSource(seed = seed, now = { now }, sleep = {}, zone = ZoneOffset.UTC, io = io)
    source.signIn(
        PasswordSignInInput(
            account.snapshot.email,
            account.password,
            "019a562b-8000-7000-8000-000000000099",
            "Tests",
        ),
    )
    return source
}
