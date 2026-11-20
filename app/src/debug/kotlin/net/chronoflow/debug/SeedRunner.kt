package net.chronoflow.debug

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.AccountDriverFactory
import net.chronoflow.core.datamock.MockScenarioValues
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.session.SessionMutationPermit
import net.chronoflow.core.model.session.SessionStore
import net.chronoflow.core.model.session.StorageNamespace
import java.io.File

internal class SeedRunner(
    private val storageDir: File,
    private val credentials: SessionStore,
    private val driverFactory: AccountDriverFactory,
    private val source: DataSource,
    private val io: CoroutineDispatcher,
) {
    suspend fun reseed(
        signedIn: SeedAccount?,
        testId: String,
        termsChangesPending: Boolean = false,
    ) = withContext(io) {
        MockSessionSeed.reset(storageDir, credentials, io)
        storageDir.mkdirs()
        if (signedIn != null) {
            MockSessionSeed.run(
                storageDir,
                StorageNamespace.Mock,
                credentials,
                source,
                PasswordSignInInput(
                    if (signedIn ==
                        SeedAccount.Demo
                    ) {
                        MockScenarioValues.demoEmail
                    } else {
                        MockScenarioValues.twoFactorEmail
                    },
                    MockScenarioValues.password,
                    testId,
                    "UI test",
                ),
                MockScenarioValues.twoFactorCode,
                driverFactory,
                io,
                onSessionReady = { database, permit ->
                    if (termsChangesPending) seedTermsChangesPending(database, permit)
                },
            )
        }
    }
}

internal suspend fun seedTermsChangesPending(
    database: AccountDatabase,
    permit: SessionMutationPermit,
) {
    val outbox = OutboxRepository(database)
    if (outbox.pending().isEmpty()) {
        outbox.enqueue(
            OutboxChange.Settings(
                SettingsSection.Preferences,
                JsonObject(mapOf("time_format" to JsonPrimitive("12"))),
            ),
            permit,
        )
    }
}
