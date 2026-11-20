@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.onboarding.support

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.session.AccountReadinessRepository
import net.chronoflow.core.data.session.AccountWiper
import net.chronoflow.core.data.session.SessionAccountDatabase
import net.chronoflow.core.data.sync.SqlSyncDatabase
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.model.session.SessionMutationPermit
import net.chronoflow.core.model.session.SessionSnapshot
import net.chronoflow.core.model.session.SessionStore
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.model.session.SyncEpoch
import net.chronoflow.core.sync.SyncEngine
import net.chronoflow.core.sync.SyncScheduler
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionTeardown
import net.chronoflow.feature.onboarding.OnboardingSession
import net.chronoflow.feature.onboarding.walkthrough.WalkthroughModel
import java.io.File
import java.time.Instant

internal val onboardingNow: Instant = Instant.parse("2026-11-20T12:00:00Z")

internal class OnboardingFixture private constructor(
    val root: File,
    val io: CoroutineDispatcher,
    seen: Boolean,
    termsPending: Boolean,
) {
    val credentials =
        object : SessionStore {
            private var value: ByteArray? = null

            override suspend fun read() = value?.copyOf()

            override suspend fun write(data: ByteArray) {
                value = data.copyOf()
            }

            override suspend fun delete() {
                value = null
            }
        }
    val accounts = SessionAccountDatabase({ root }, StorageNamespace.Mock, JdbcAccountDriverFactory(), io)
    private val wiper = AccountWiper(root, StorageNamespace.Mock, credentials, io = io)
    val controller =
        SessionController(
            credentials,
            accounts,
            SessionTeardown(
                sleep = { awaitCancellation() },
                pendingCount = { outbox.pending().size },
                flush = { throw DataFailure.Offline },
                beginWipe = wiper::begin,
                erase = wiper::erase,
                finishWipe = wiper::finish,
            ),
            io,
        )
    val source =
        MockDataSource(
            seed = MockAppSeed.make(onboardingNow, walkthroughSeen = seen, termsPending = termsPending),
            now = { onboardingNow },
            sleep = {},
            io = io,
        )
    lateinit var result: SessionResult
        private set
    lateinit var snapshot: SessionSnapshot
        private set
    lateinit var permit: SessionMutationPermit
        private set
    val database get() = checkNotNull(storedDatabase)
    private var storedDatabase: net.chronoflow.core.database.AccountDatabase? = null
    val outbox get() = OutboxRepository(database)
    val settings get() = SettingsRepository(database)
    val account get() = AccountRepository(database)
    val sync get() = SqlSyncDatabase(database)
    val epoch get() = SyncEpoch(AccountID.of(result.account.id), snapshot.epoch)

    fun session() = OnboardingSession.from(controller, snapshot, permit, outbox, settings)

    fun walkthroughModel() = WalkthroughModel(complete = { outbox.enqueue(OutboxChange.Walkthrough, permit) })

    fun termsModel() =
        net.chronoflow.feature.onboarding.terms.TermsUpdateModel(result.account.policies) {
            outbox.enqueue(OutboxChange.Policies(it), permit)
        }

    suspend fun gate() = AccountReadinessRepository(database).values(result.account.id).first()?.gate

    fun engine() =
        SyncEngine(
            sync,
            source,
            epoch,
            scheduler =
                SyncScheduler({
                    onboardingNow
                }, { awaitCancellation() }),
            dispatcher = io,
        )

    suspend fun close() =
        withContext(NonCancellable) {
            controller.close()
            val field = SessionController::class.java.getDeclaredField("scope").apply { isAccessible = true }
            (field.get(controller) as CoroutineScope).coroutineContext.job.cancelAndJoin()
            accounts.close()
            withContext(io) { check(root.deleteRecursively()) }
        }

    companion object {
        suspend fun make(
            scope: TestScope,
            seen: Boolean = false,
            termsPending: Boolean = false,
        ): OnboardingFixture {
            val io = StandardTestDispatcher(scope.testScheduler)
            val root = withContext(io) { TestRoots.temporaryStoreRoot() }
            val f = OnboardingFixture(root, io, seen, termsPending)
            try {
                f.result =
                    (
                        f.source.signIn(
                            PasswordSignInInput(
                                "demo@example.com",
                                "password",
                                "019a562b-8000-7000-8000-000000000099",
                                "Onboarding test",
                            ),
                        ) as SignInResult.Session
                    ).result
                f.controller.accept(f.result)
                f.snapshot = checkNotNull(f.controller.activeSession())
                f.storedDatabase = checkNotNull(f.accounts.activeAccountDatabase())
                f.permit = checkNotNull(f.controller.mutationPermit(f.snapshot))
                return f
            } catch (failure: Throwable) {
                f.close()
                throw failure
            }
        }
    }
}

internal suspend fun TestScope.withOnboardingFixture(
    seen: Boolean = false,
    termsPending: Boolean = false,
    block: suspend (OnboardingFixture) -> Unit,
) {
    val f = OnboardingFixture.make(this, seen, termsPending)
    try {
        block(f)
    } finally {
        f.close()
    }
}
