package net.chronoflow.debug

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.chronoflow.core.database.AccountDriverFactory
import net.chronoflow.core.datamock.MockScenarioValues
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.PasswordSignInInput
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
            )
        }
    }
}
