package net.chronoflow.core.data.guest

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.store.StoreApplication
import net.chronoflow.core.data.wizard.support.WIZARD_ACCOUNT
import net.chronoflow.core.data.wizard.support.WizardStore
import net.chronoflow.core.database.store.fetchSetting
import net.chronoflow.core.database.store.saveAccount
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.api.AccountSettings
import net.chronoflow.core.model.api.AccountSnapshot
import net.chronoflow.core.model.auth.GuestSessionContinuation
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.model.store.SettingsRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class GuestContinuationRepositoryTest {
    @Test fun consumesOnlyAtTheAppGate() =
        runTest(timeout = 60.seconds) {
            withStore {
                val repository = GuestContinuationRepository(database)
                val trip = GuestSessionContinuation.Trip("t")
                repository.store(trip)
                assertNull(repository.consume(Gate.Verify))
                assertNull(repository.consume(Gate.Terms))
                assertNull(repository.consume(Gate.Walkthrough))
                assertEquals(trip, repository.values().first())
                assertEquals(trip, repository.consume(Gate.App))
                assertNull(database.read { it.fetchSetting("guestContinuation") })
                assertNull(repository.consume(Gate.App))
            }
        }

    @Test fun valuesEmitTheStoredDestination() =
        runTest(timeout = 60.seconds) {
            withStore {
                val repository = GuestContinuationRepository(database)
                repository.store(GuestSessionContinuation.Trip("t"))
                assertEquals(GuestSessionContinuation.Trip("t"), repository.values().first { it != null })
            }
        }

    @Test fun survivesCloseAndReopen() =
        runTest(timeout = 60.seconds) {
            withStore {
                GuestContinuationRepository(database).store(GuestSessionContinuation.MemberWizard)
                database.close()
                database.open(WIZARD_ACCOUNT)
                val restored = GuestContinuationRepository(database).consume(Gate.App)
                assertEquals(GuestSessionContinuation.MemberWizard, restored)
            }
        }

    @Test fun corruptRowIsDroppedOnConsume() =
        runTest(timeout = 60.seconds) {
            withStore {
                SettingsRepository(database).upsert(SettingsRecord("guestContinuation", """{"kind":"unknown"}"""))
                assertNull(GuestContinuationRepository(database).consume(Gate.App))
                assertNull(database.read { it.fetchSetting("guestContinuation") })
            }
        }

    @Test fun otherSettingsAreUntouched() =
        runTest(timeout = 60.seconds) {
            withStore {
                val settings = SettingsRepository(database)
                settings.upsert(SettingsRecord("bodyClock", "{}"))
                val body = settings.settings().first()
                val repository = GuestContinuationRepository(database)
                repository.store(GuestSessionContinuation.Trips)
                repository.consume(Gate.App)
                assertEquals(listOf(body), settings.settings())
            }
        }

    @Test fun snapshotApplicationKeepsTheRow() =
        runTest(timeout = 60.seconds) {
            withStore {
                val empty = JsonObject(emptyMap())
                val settings =
                    AccountSettings(
                        bodyClock = empty,
                        sleepAnswered = false,
                        preferences = empty,
                        reminders = empty,
                        planCredits = JsonObject(mapOf("credits" to JsonPrimitive(3))),
                        policies = AccountPolicies("1", "1"),
                    )
                val id = AccountID.of(WIZARD_ACCOUNT)
                val repository = GuestContinuationRepository(database)
                repository.store(GuestSessionContinuation.Trip("t"))
                database.write {
                    it.saveAccount(AccountRecord(WIZARD_ACCOUNT, "Traveller", "test@example.com"))
                    StoreApplication.snapshot(
                        AccountSnapshot(
                            WIZARD_ACCOUNT,
                            "Traveller",
                            "test@example.com",
                            true,
                            settings,
                            empty,
                            empty,
                            empty,
                        ),
                        id,
                        it,
                    )
                }
                assertEquals(GuestSessionContinuation.Trip("t"), repository.values().first())
                assertEquals("""{"credits":3}""", database.read { it.fetchSetting("planCredits")?.value })
            }
        }

    private suspend fun TestScope.withStore(block: suspend WizardStore.() -> Unit) {
        val fixture = WizardStore(StandardTestDispatcher(testScheduler))
        try {
            fixture.block()
        } finally {
            fixture.close()
        }
    }
}
