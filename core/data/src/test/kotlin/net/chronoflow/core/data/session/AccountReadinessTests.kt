@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.data.session

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.database.store.deleteSetting
import net.chronoflow.core.database.store.saveAccount
import net.chronoflow.core.database.store.saveOutboxRow
import net.chronoflow.core.database.store.saveSetting
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.session.StoredAccountReadiness
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.testing.sync.syncAccount
import net.chronoflow.core.testing.sync.syncOther
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class AccountReadinessTests {
    @Test fun sqliteMarkerAndGatePrecedenceAreObservedTogether() =
        runTest(timeout = 60.seconds) {
            val store = SessionStoreFixture(StandardTestDispatcher(testScheduler))
            try {
                store.accounts.open(AccountID.of(syncAccount))
                val db = store.accounts.activeAccountDatabase()!!
                val repository = AccountReadinessRepository(db)
                try {
                    repository.values(syncOther)
                    fail("expected account isolation")
                } catch (
                    failure: StoreFailure,
                ) {
                    assertEquals(StoreFailure.InvalidAccountID, failure)
                }
                repository.values(syncAccount).test {
                    assertNull(awaitItem())
                    var record = AccountRecord(syncAccount, "Name", "a@example.test", hasVerifiedEmail = false)
                    AccountRepository(db).upsert(record)
                    assertEquals(StoredAccountReadiness(syncAccount, Gate.Verify, false, Gate.Walkthrough), awaitItem())
                    record = record.copy(hasVerifiedEmail = true)
                    AccountRepository(db).upsert(record)
                    assertNull(awaitItem()!!.gate)
                    val terms = AccountPolicies("2", "1", "1")
                    db.write {
                        it.saveSetting(
                            SettingsRecord(
                                "policies",
                                ApiJson.response.encodeToString(AccountPolicies.serializer(), terms),
                            ),
                        )
                    }
                    assertEquals(Gate.Terms, awaitItem()!!.gate)
                    db.write {
                        it.saveSetting(
                            SettingsRecord(
                                "policies",
                                ApiJson.response.encodeToString(
                                    AccountPolicies.serializer(),
                                    terms.copy(acceptedTermsVersion = "2"),
                                ),
                            ),
                        )
                    }
                    assertEquals(Gate.Walkthrough, awaitItem()!!.gate)
                    record = record.copy(walkthroughSeen = true)
                    AccountRepository(db).upsert(record)
                    assertEquals(StoredAccountReadiness(syncAccount, Gate.App, false, Gate.App), awaitItem())
                    db.write { it.saveAccount(record.copy(syncedAt = "marker")) }
                    assertTrue(awaitItem()!!.hasStoredData)
                    db.close()
                    awaitComplete()
                }
            } finally {
                store.close()
            }
        }

    /** An offline walkthrough completion participates in the same gate fetch as the account row. */
    @Test fun pendingWalkthroughRowCountsAsSeen() =
        runTest(timeout = 60.seconds) {
            val store = SessionStoreFixture(StandardTestDispatcher(testScheduler))
            try {
                store.accounts.open(AccountID.of(syncAccount))
                val db = store.accounts.activeAccountDatabase()!!
                db.write {
                    it.saveAccount(AccountRecord(syncAccount, "Name", "email", hasVerifiedEmail = true))
                    it.saveSetting(
                        SettingsRecord(
                            "policies",
                            ApiJson.response.encodeToString(
                                AccountPolicies.serializer(),
                                AccountPolicies("1", "1", "1"),
                            ),
                        ),
                    )
                    it.saveOutboxRow(OutboxRecord("pending", "walkthrough", "{}", "now"))
                }
                val value = AccountReadinessRepository(db).values(syncAccount).first()!!
                assertEquals(Gate.App, value.gate)
                assertFalse(value.hasStoredData)
                assertEquals(Gate.App, value.firstLoadGate)
            } finally {
                store.close()
            }
        }

    /** Missing policies defer routing until the launch explicitly permits its first-load fallback. */
    @Test fun missingPoliciesYieldNullGate() =
        runTest(timeout = 60.seconds) {
            val store = SessionStoreFixture(StandardTestDispatcher(testScheduler))
            try {
                store.accounts.open(AccountID.of(syncAccount))
                val db = store.accounts.activeAccountDatabase()!!
                db.write {
                    it.saveAccount(AccountRecord(syncAccount, "Name", "email", hasVerifiedEmail = true))
                    it.deleteSetting("policies")
                }
                val value = AccountReadinessRepository(db).values(syncAccount).first()!!
                assertNull(value.gate)
                assertEquals(Gate.Walkthrough, value.firstLoadGate)
            } finally {
                store.close()
            }
        }
}
