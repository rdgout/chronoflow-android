package net.chronoflow.core.data.repository

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.SqlDriver
import app.cash.turbine.test
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.AccountDriverFactory
import net.chronoflow.core.database.store.fetchAccount
import net.chronoflow.core.database.store.saveAccount
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.model.store.CheckMarkRecord
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.model.store.TripRecord
import net.chronoflow.core.testing.sync.syncAccount
import net.chronoflow.core.testing.sync.syncOther
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RepositoryObservationTests {
    @Test fun initialFetchIsOffMainAndValuesAppearAfterAtomicCommitOnly() =
        runTest(timeout = 60.seconds) {
            val marker = ThreadLocal<Boolean>()
            val delegate = StandardTestDispatcher(testScheduler)
            val io =
                object : CoroutineDispatcher() {
                    override fun dispatch(
                        context: CoroutineContext,
                        block: Runnable,
                    ) = delegate.dispatch(context) {
                        marker.set(true)
                        try {
                            block.run()
                        } finally {
                            marker.remove()
                        }
                    }
                }
            val store = RepositoryStore(io)
            try {
                store.database
                    .observe("account") { sql ->
                        assertEquals(true, marker.get())
                        sql.fetchAccount(syncAccount)
                    }.test {
                        assertNull(awaitItem())
                        val final = AccountRecord(syncAccount, "complete", "new@example.test")
                        store.database.write {
                            it.saveAccount(AccountRecord(syncAccount, "half", "old"))
                            it.saveAccount(final)
                        }
                        assertEquals(final, awaitItem())
                        try {
                            store.database.write<Unit> {
                                it.saveAccount(final.copy(name = "rolled back", email = "bad"))
                                throw StoreFailure.Unavailable()
                            }
                            error("expected rollback")
                        } catch (_: StoreFailure.Unavailable) {
                        }
                        expectNoEvents()
                        store.database.close()
                        awaitComplete()
                    }
            } finally {
                store.close()
            }
        }

    @Test fun cancellationAndCloseEndSubscriptionsBeforeAnotherAccountOpens() =
        runTest(timeout = 60.seconds) {
            val store = RepositoryStore(StandardTestDispatcher(testScheduler))
            try {
                val values = mutableListOf<List<TripRecord>>()
                val consumer =
                    backgroundScope.launch {
                        TripRepository(store.database).values().collect { values += it }
                    }
                runCurrent()
                assertEquals(listOf(emptyList<TripRecord>()), values)
                consumer.cancelAndJoin()
                TripRepository(store.database).upsert(TripRecord("late", "{}", departureUtc = "date"))
                store.database.close()
                assertEquals(1, values.size)
                assertEquals(0, store.database.observationCount)
                val second = AccountDatabase(store.root, driverFactory = JdbcAccountDriverFactory(), io = store.io)
                second.open(syncOther)
                try {
                    assertTrue(TripRepository(second).trips().isEmpty())
                } finally {
                    second.close()
                }
            } finally {
                store.close()
            }
        }

    @Test fun accountModelSeparatesUnknownEmptyAndLoadedAndClearsOnDetach() =
        runTest(timeout = 60.seconds) {
            val store = RepositoryStore(StandardTestDispatcher(testScheduler))
            val model = AccountModel(backgroundScope)
            try {
                assertEquals(AccountModel.LoadState.Unknown, model.state.value)
                val repository = AccountRepository(store.database)
                model.attach(repository, 10)
                runCurrent()
                assertEquals(AccountModel.LoadState.Loaded(null), model.state.value)
                val account = AccountRecord(syncAccount, "Name", "email")
                repository.upsert(account)
                runCurrent()
                assertEquals(AccountModel.LoadState.Loaded(account), model.state.value)
                model.detach()
                assertEquals(AccountModel.LoadState.Unknown, model.state.value)
                repository.upsert(account.copy(name = "late", email = "late"))
                store.database.close()
                runCurrent()
                assertEquals(AccountModel.LoadState.Unknown, model.state.value)
            } finally {
                model.detach()
                store.close()
            }
        }

    // (new) Android owners detach from cancelled finally blocks without a deinit backstop.
    @Test fun detachFromCancelledOwnerClearsTheModel() =
        runTest(timeout = 60.seconds) {
            val store = RepositoryStore(StandardTestDispatcher(testScheduler))
            val model = AccountModel(backgroundScope)
            try {
                val repository = AccountRepository(store.database)
                val account = AccountRecord(syncAccount, "A", "a")
                repository.upsert(account)
                model.attach(repository, 1)
                runCurrent()
                assertEquals(AccountModel.LoadState.Loaded(account), model.state.value)
                val owner =
                    backgroundScope.launch {
                        try {
                            awaitCancellation()
                        } finally {
                            model.detach()
                        }
                    }
                runCurrent()
                owner.cancelAndJoin()
                runCurrent()
                assertEquals(AccountModel.LoadState.Unknown, model.state.value)
                assertEquals(0, store.database.observationCount)
                repository.upsert(account.copy(name = "late"))
                runCurrent()
                assertEquals(AccountModel.LoadState.Unknown, model.state.value)
            } finally {
                model.detach()
                store.close()
            }
        }

    @Test fun everyRepositoryEmitsInitialEmptyAndCommittedValues() =
        runTest(timeout = 60.seconds) {
            val store = RepositoryStore(StandardTestDispatcher(testScheduler))
            try {
                val trips = TripRepository(store.database)
                val completed = mutableSetOf<String>()
                val plans = mutableListOf<PlanVersionRecord?>()
                val checks = mutableListOf<List<CheckMarkRecord>>()
                val sections = mutableListOf<List<SettingsRecord>>()
                val pending = mutableListOf<List<OutboxRecord>>()
                backgroundScope.launch {
                    trips.planValues("trip", "hash").collect { plans += it }
                    completed += "plans"
                }
                backgroundScope.launch {
                    trips.checkValues("trip", "hash").collect { checks += it }
                    completed += "checks"
                }
                backgroundScope.launch {
                    SettingsRepository(store.database).values().collect { sections += it }
                    completed += "settings"
                }
                backgroundScope.launch {
                    OutboxRepository(store.database).values().collect { pending += it }
                    completed += "outbox"
                }
                runCurrent()
                assertEquals(listOf<PlanVersionRecord?>(null), plans)
                assertEquals(listOf(emptyList<CheckMarkRecord>()), checks)
                assertEquals(listOf(emptyList<SettingsRecord>()), sections)
                assertEquals(listOf(emptyList<OutboxRecord>()), pending)
                trips.upsert(TripRecord("trip", "{}", departureUtc = "date"))
                val plan = PlanVersionRecord("trip", "hash", "{}".encodeToByteArray(), "date")
                val mark = CheckMarkRecord("trip", "hash", "sleep", "date", 0, "done", "date")
                trips.store(plan, listOf(mark))
                runCurrent()
                assertEquals(listOf(null, plan), plans)
                assertEquals(listOf(emptyList(), listOf(mark)), checks)
                val section = SettingsRecord("preferences", "{}")
                SettingsRepository(store.database).upsert(section)
                runCurrent()
                assertEquals(listOf(emptyList(), listOf(section)), sections)
                val change = OutboxRecord("change", "settings", "{}", "date")
                OutboxRepository(store.database).upsert(change)
                runCurrent()
                assertEquals(listOf(emptyList(), listOf(change)), pending)
                store.database.close()
                runCurrent()
                assertEquals(setOf("plans", "checks", "settings", "outbox"), completed)
                assertEquals(0, store.database.observationCount)
            } finally {
                store.close()
            }
        }

    @Test fun closingAnAttachedDatabaseClearsTheLoadedAccount() =
        runTest(timeout = 60.seconds) {
            val store = RepositoryStore(StandardTestDispatcher(testScheduler))
            val model = AccountModel(backgroundScope)
            try {
                val repository = AccountRepository(store.database)
                val account = AccountRecord(syncAccount, "A", "a")
                repository.upsert(account)
                model.attach(repository, 1)
                runCurrent()
                assertEquals(AccountModel.LoadState.Loaded(account), model.state.value)
                store.database.close()
                runCurrent()
                assertEquals(AccountModel.LoadState.Unknown, model.state.value)
                assertEquals(0, store.database.observationCount)
            } finally {
                model.detach()
                store.close()
            }
        }

    // (new) Android database observers can fail with framework errors outside StoreFailure and SQLException.
    @Test fun accountModelSurfacesUnexpectedObservationFailure() =
        runTest(timeout = 60.seconds) {
            val store = RepositoryStore(StandardTestDispatcher(testScheduler))
            val factory =
                AccountDriverFactory { file ->
                    val driver = JdbcAccountDriverFactory().open(file)
                    object : SqlDriver by driver {
                        override fun addListener(
                            vararg queryKeys: String,
                            listener: Query.Listener,
                        ): Unit = throw IllegalStateException("observer registration failed")
                    }
                }
            val database = AccountDatabase(store.root, driverFactory = factory, io = store.io)
            val model = AccountModel(backgroundScope)
            try {
                store.database.close()
                database.open(syncAccount)
                model.attach(AccountRepository(database), 1)
                runCurrent()
                assertEquals(AccountModel.LoadState.Failed, model.state.value)
                assertEquals(0, database.observationCount)
            } finally {
                model.detach()
                database.close()
                store.close()
            }
        }

    @Test fun modelRejectsOlderAccountAfterReplacementAndSurfacesStoreFailure() =
        runTest(timeout = 60.seconds) {
            val first = RepositoryStore(StandardTestDispatcher(testScheduler))
            val second = RepositoryStore(StandardTestDispatcher(testScheduler), syncOther)
            val model = AccountModel(backgroundScope)
            try {
                val a = AccountRecord(syncAccount, "A", "a")
                val b = AccountRecord(syncOther, "B", "b")
                AccountRepository(first.database).upsert(a)
                AccountRepository(second.database).upsert(b)
                val token = model.attachmentToken
                model.attach(AccountRepository(first.database), 1)
                runCurrent()
                assertEquals(AccountModel.LoadState.Loaded(a), model.state.value)
                model.attach(AccountRepository(second.database), 2)
                runCurrent()
                assertEquals(AccountModel.LoadState.Loaded(b), model.state.value)
                model.attach(AccountRepository(first.database), 1, token)
                assertEquals(2L, model.epoch)
                AccountRepository(first.database).upsert(a.copy(name = "late", email = "late"))
                first.database.close()
                runCurrent()
                assertEquals(AccountModel.LoadState.Loaded(b), model.state.value)
                model.detach()
                second.database.close()
                model.attach(AccountRepository(second.database), 3)
                runCurrent()
                assertEquals(AccountModel.LoadState.Failed, model.state.value)
            } finally {
                model.detach()
                first.close()
                second.close()
            }
        }
}
