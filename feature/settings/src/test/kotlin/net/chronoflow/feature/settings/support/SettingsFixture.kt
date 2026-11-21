@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.support

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.session.AccountWiper
import net.chronoflow.core.data.session.SessionAccountDatabase
import net.chronoflow.core.data.sync.SqlSyncDatabase
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.session.SessionMutationPermit
import net.chronoflow.core.model.session.SessionSnapshot
import net.chronoflow.core.model.session.SessionStore
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.model.session.SyncEpoch
import net.chronoflow.core.plan.CopyCatalog
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.sync.FullPull
import net.chronoflow.core.sync.OutboxFlusher
import net.chronoflow.core.sync.SessionSync
import net.chronoflow.core.sync.SyncScheduler
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionTeardown
import net.chronoflow.core.testing.FakePlanClock
import net.chronoflow.core.testing.junit.measureClassWarmup
import net.chronoflow.feature.settings.SettingsDependencies
import net.chronoflow.feature.settings.home.SettingsHomeViewModel
import org.junit.BeforeClass
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.seconds

internal val settingsNow: Instant = Instant.ofEpochSecond(1_794_132_000)

internal class SettingsFixture(
    val root: File,
    val io: CoroutineDispatcher,
    mutationsOffline: Boolean,
) {
    class Credentials : SessionStore {
        var data: ByteArray? = null

        override suspend fun read() = data?.copyOf()

        override suspend fun write(data: ByteArray) {
            this.data = data.copyOf()
        }

        override suspend fun delete() {
            data = null
        }
    }

    val credentials = Credentials()
    val accounts = SessionAccountDatabase({ root }, StorageNamespace.Mock, JdbcAccountDriverFactory(), io)
    private val wiper = AccountWiper(root, StorageNamespace.Mock, credentials, io = io)
    val source =
        MockDataSource(
            seed = MockAppSeed.make(settingsNow, walkthroughSeen = true),
            mutationsOffline = mutationsOffline,
            now = { settingsNow },
            sleep = {},
            io = io,
        )
    var flush: suspend () -> Unit = { OutboxFlusher(sql, source, now = { settingsNow }).run(epoch) }
    var logout: suspend () -> Unit = {}
    var signOutCalls = 0
    val controller =
        SessionController(
            credentials,
            accounts,
            SessionTeardown(
                sleep = { delay(it) },
                pendingCount = { outbox.pending().size },
                flush = { flush() },
                beginWipe = wiper::begin,
                erase = wiper::erase,
                finishWipe = wiper::finish,
                logout = { logout() },
            ),
            io,
        )
    var syncSource: net.chronoflow.core.model.DataSource = source
    val sync =
        SessionSync(
            accounts,
            SyncScheduler({ settingsNow }, { awaitCancellation() }),
            source = { syncSource },
            unauthorized = { controller.unauthorized(it) },
            verification = {},
            dispatcher = io,
        )
    lateinit var result: SessionResult
    lateinit var snapshot: SessionSnapshot
    lateinit var permit: SessionMutationPermit
    val database get() = checkNotNull(storedDatabase)
    private var storedDatabase: net.chronoflow.core.database.AccountDatabase? = null
    val outbox get() = OutboxRepository(database)
    val settings get() = SettingsRepository(database)
    val account get() = AccountRepository(database)
    val trips get() = TripRepository(database)
    val sql get() = SqlSyncDatabase(database)
    val epoch get() = SyncEpoch(AccountID.of(result.account.id), snapshot.epoch)
    val clock = FakePlanClock(settingsNow)
    val securityScope = CoroutineScope(SupervisorJob() + io)
    var securitySyncRequests = 0
    val deps: SettingsDependencies get() =
        object : SettingsDependencies {
            override val securityScope get() = this@SettingsFixture.securityScope
            override val snapshot = this@SettingsFixture.snapshot
            override val passwordPromptSeed: String? = null
            override val clipboard: net.chronoflow.feature.settings.security.SensitiveClipboard
                get() = error("Clipboard is not used by the store fixture")

            override suspend fun requestSecurity(
                change: net.chronoflow.core.model.api.AccountSecurityChange,
            ): net.chronoflow.core.model.api.AccountSecurityResult =
                withContext(io) {
                    if (!isCurrent()) {
                        throw net.chronoflow.core.model.session
                            .StaleSessionException()
                    }
                    val result =
                        try {
                            sync.accountSecurity(change, snapshot.session, snapshot.epoch)
                        } catch (_: kotlinx.coroutines.CancellationException) {
                            kotlinx.coroutines.currentCoroutineContext().ensureActive()
                            throw net.chronoflow.core.model.session
                                .StaleSessionException()
                        }
                    if (!isCurrent()) {
                        throw net.chronoflow.core.model.session
                            .StaleSessionException()
                    }
                    val admitted =
                        controller.mutationPermit(snapshot)
                            ?: throw net.chronoflow.core.model.session
                                .StaleSessionException()
                    net.chronoflow.core.data.security
                        .AccountSecurityStore(
                            database,
                            settings,
                        ).apply(change, result, admitted)
                    sync.requestSync()
                    securitySyncRequests++
                    result
                }

            override fun account() = account.values()

            override fun settings() = settings.values()

            override fun outbox() = outbox.values()

            override suspend fun pendingOutbox() = outbox.pending()

            override fun syncEvents() = sync.events()

            override fun sessionUpdates() = controller.updates.filterNotNull()

            override suspend fun isCurrent() = controller.activeSession() == snapshot

            override suspend fun signOut(discardPending: Boolean) =
                controller.signOut(snapshot, discardPending).also {
                    signOutCalls++
                }

            override suspend fun enqueueProfile(
                name: String,
                email: String,
            ) {
                outbox.enqueue(
                    OutboxChange.Profile(
                        net.chronoflow.core.model
                            .ProfileChange(name, email),
                    ),
                    permit,
                    now = settingsNow,
                )
            }

            override suspend fun requestSync() {}

            override val clock get() = this@SettingsFixture.clock
            override val zone = ZoneOffset.UTC

            override suspend fun formatter() = CopyLineFormatter(catalog)

            override suspend fun enqueueSettings(
                section: net.chronoflow.core.model.SettingsSection,
                patch: kotlinx.serialization.json.JsonObject,
            ) {
                outbox.enqueue(OutboxChange.Settings(section, patch), permit, now = settingsNow)
            }
        }
    val store = ViewModelStore()
    val viewModels = mutableListOf<androidx.lifecycle.ViewModel>()

    fun model(): SettingsHomeViewModel =
        SettingsHomeViewModel(deps, io).also {
            store.put("home-${viewModels.size}", it)
            viewModels += it
        }

    fun profile(): net.chronoflow.feature.settings.profile.ProfileViewModel =
        net.chronoflow.feature.settings.profile.ProfileViewModel(deps, androidx.lifecycle.SavedStateHandle(), io).also {
            store.put("profile-${viewModels.size}", it)
            viewModels += it
        }

    fun preferences(): net.chronoflow.feature.settings.preferences.PreferencesViewModel =
        net.chronoflow.feature.settings.preferences.PreferencesViewModel(deps, FakeThemeDependencies(), io).also {
            store.put("preferences-${viewModels.size}", it)
            viewModels += it
        }

    fun bodyClock(): net.chronoflow.feature.settings.bodyclock.BodyClockViewModel =
        net.chronoflow.feature.settings.bodyclock.BodyClockViewModel(deps, io).also {
            store.put("bodyClock-${viewModels.size}", it)
            viewModels += it
        }

    suspend fun reopen() {
        store.clear()
        viewModels.forEach {
            it.viewModelScope.coroutineContext.job
                .cancelAndJoin()
        }
        accounts.close()
        accounts.open(AccountID.of(result.account.id))
        accounts.upsert(result.account)
        storedDatabase = checkNotNull(accounts.activeAccountDatabase())
    }

    suspend fun pending() =
        withContext(io) {
            val trip = trips.trips().first { it.keepOffline }
            val hash = checkNotNull(trip.currentInputHash)
            val plan = checkNotNull(trips.plan(trip.id, hash))
            val detail = ApiJson.response.decodeFromString(TripDetail.serializer(), plan.detail.decodeToString())
            val key =
                checkNotNull(
                    detail.plan.blocks
                        .first { it.actionable && it.checkKey != null }
                        .checkKey,
                )
            outbox.enqueue(
                OutboxChange.CheckOff(
                    CheckOffChange(trip.id, hash, key.type, key.localDate, key.ordinal, CheckStatus.Done),
                ),
                permit,
                now = settingsNow,
            )
        }

    suspend fun close() =
        withContext(NonCancellable) {
            store.clear()
            viewModels.forEach {
                it.viewModelScope.coroutineContext.job
                    .cancelAndJoin()
            }
            securityScope.coroutineContext.job.cancelAndJoin()
            sync.stop()
            joinOwnedScope(sync)
            controller.close()
            joinOwnedScope(controller)
            accounts.close()
            withContext(io) { check(root.deleteRecursively()) }
        }

    companion object {
        val catalog by lazy {
            CopyCatalog(
                checkNotNull(
                    SettingsFixture::class.java.classLoader?.getResourceAsStream("plan_copy.json"),
                ).use {
                    it.readBytes()
                },
            )
        }

        suspend fun make(
            scope: TestScope,
            mutationsOffline: Boolean = true,
        ): SettingsFixture {
            val io = StandardTestDispatcher(scope.testScheduler)
            val f = SettingsFixture(withContext(io) { TestRoots.temporaryStoreRoot() }, io, mutationsOffline)
            try {
                f.result =
                    (
                        f.source.signIn(
                            PasswordSignInInput(
                                "demo@example.com",
                                "password",
                                "019a562b-8000-7000-8000-000000000099",
                                "Settings test",
                            ),
                        ) as SignInResult.Session
                    ).result
                f.controller.accept(f.result)
                f.snapshot = checkNotNull(f.controller.activeSession())
                f.storedDatabase = checkNotNull(f.accounts.activeAccountDatabase())
                f.permit = checkNotNull(f.controller.mutationPermit(f.snapshot))
                FullPull(f.sql, f.source, now = { settingsNow }).run(f.epoch)
                return f
            } catch (failure: Throwable) {
                f.close()
                throw failure
            }
        }
    }
}

private suspend fun joinOwnedScope(owner: Any) {
    val field = owner.javaClass.getDeclaredField("scope").apply { isAccessible = true }
    (field.get(owner) as CoroutineScope).coroutineContext.job.cancelAndJoin()
}

internal suspend fun TestScope.withSettingsFixture(
    mutationsOffline: Boolean = true,
    block: suspend (SettingsFixture) -> Unit,
) {
    val f = SettingsFixture.make(this, mutationsOffline)
    try {
        block(f)
    } finally {
        f.close()
    }
}

abstract class SettingsFixtureTest {
    companion object {
        private val warm by lazy {
            runTest(timeout = 60.seconds) {
                withSettingsFixture { f ->
                    f.model()
                    runCurrent()
                    SettingsFixture.catalog
                }
            }
        }

        @JvmStatic @BeforeClass
        fun warmFixtureOncePerJvm() {
            measureClassWarmup(SettingsFixtureTest::class.java, 0, "jdbc-and-settings-fixture") { warm }
        }
    }
}
