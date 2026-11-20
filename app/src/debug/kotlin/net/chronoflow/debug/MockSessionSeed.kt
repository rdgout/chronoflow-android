package net.chronoflow.debug

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.session.SessionAccountDatabase
import net.chronoflow.core.data.sync.SqlSyncDatabase
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.AccountDriverFactory
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.model.api.TwoFactorInput
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.session.SessionMutationPermit
import net.chronoflow.core.model.session.SessionStore
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.model.session.SyncEpoch
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.sync.FullPull
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionTeardown
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.time.Instant

object MockSessionSeed {
    suspend fun reset(
        root: File,
        credentials: SessionStore,
        io: CoroutineDispatcher,
    ) = withContext(io) {
        credentials.delete()
        if (root.exists() && !root.deleteRecursively()) throw StoreFailure.Unavailable()
    }

    suspend fun run(
        root: File,
        ns: StorageNamespace,
        credentials: SessionStore,
        source: DataSource,
        input: PasswordSignInInput,
        twoFactorCode: String,
        driverFactory: AccountDriverFactory,
        io: CoroutineDispatcher,
        onSessionReady: suspend (AccountDatabase, SessionMutationPermit) -> Unit = { _, _ -> },
    ) = withContext(io) {
        val marker = File(root, "signed-in-seed.done")
        if (marker.exists()) return@withContext
        val result =
            when (val signIn = source.signIn(input)) {
                is SignInResult.Session -> {
                    signIn.result
                }

                is SignInResult.TwoFactor -> {
                    source.completeTwoFactor(
                        TwoFactorInput(signIn.challengeId, TwoFactorInput.Proof.Code(twoFactorCode)),
                    )
                }
            }
        val accounts = SessionAccountDatabase({ root }, ns, driverFactory, io)
        val controller = SessionController(credentials, accounts, SessionTeardown(sleep = { delay(it) }), io)
        try {
            controller.accept(result)
            val database = accounts.activeAccountDatabase() ?: throw SessionFailure.DatabaseUnavailable
            val session = controller.activeSession() ?: throw SessionFailure.DatabaseUnavailable
            FullPull(SqlSyncDatabase(database), source, now = {
                Instant.now()
            }).run(SyncEpoch(AccountID.of(result.account.id), session.epoch))
            val permit = controller.mutationPermit(session) ?: throw SessionFailure.DatabaseUnavailable
            onSessionReady(database, permit)
            accounts.close()
            root.mkdirs()
            val temporary = File(root, "signed-in-seed.done.tmp")
            temporary.writeBytes(byteArrayOf())
            Files.move(temporary.toPath(), marker.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } finally {
            withContext(NonCancellable) {
                try {
                    accounts.close()
                } catch (
                    cancelled: CancellationException,
                ) {
                    throw cancelled
                } catch (_: StoreFailure) {
                } finally {
                    controller.close()
                }
            }
        }
    }
}
