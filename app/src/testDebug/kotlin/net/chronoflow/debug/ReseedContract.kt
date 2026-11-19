package net.chronoflow.debug

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.session.SessionAccountDatabase
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.datamock.MockScenarioValues
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.session.SessionStore
import net.chronoflow.core.model.session.StorageNamespace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.io.File

internal suspend fun reseedContract(
    root: File,
    credentials: SessionStore,
    io: CoroutineDispatcher,
    reseed: suspend (SeedAccount) -> Unit,
) {
    reseed(SeedAccount.TwoFactor)
    val cold = seedSummary(root, credentials, io)
    reseed(SeedAccount.Demo)
    val accounts = SessionAccountDatabase({ root }, StorageNamespace.Mock, JdbcAccountDriverFactory(), io)
    try {
        accounts.open(AccountID.of(MockScenarioValues.demoID))
        val outbox = OutboxRepository(checkNotNull(accounts.activeAccountDatabase()))
        outbox.enqueue(
            OutboxChange.Settings(
                SettingsSection.Preferences,
                JsonObject(
                    mapOf("time_format" to JsonPrimitive("12")),
                ),
            ),
        )
        assertEquals(1, outbox.pending().size)
    } finally {
        accounts.close()
    }
    reseed(SeedAccount.TwoFactor)
    assertEquals(cold, seedSummary(root, credentials, io))
    withContext(io) {
        assertFalse(File(root, "ChronoFlowMock/Accounts/${MockScenarioValues.demoID}").exists())
        assertTrue(File(root, "signed-in-seed.done").exists())
    }
}
