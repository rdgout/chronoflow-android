@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.reminders

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.sync.SqlSyncDatabase
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.AccountSettings
import net.chronoflow.core.model.api.AccountSnapshot
import net.chronoflow.core.model.session.SessionMutationPermit
import net.chronoflow.core.model.session.SyncEpoch
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.core.reminders.ReminderSettings
import net.chronoflow.core.sync.PlanRetention
import net.chronoflow.core.testing.sync.snapshot
import net.chronoflow.core.testing.sync.syncAccount
import net.chronoflow.core.testing.sync.syncDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class RemindersModelTests {
    @Test fun settingsOutboxPendingProtectionPermissionReturnAndRelaunch() =
        runTest(timeout = 60.seconds) {
            val io = StandardTestDispatcher(testScheduler)
            val root = TestRoots.temporaryStoreRoot()
            val database = AccountDatabase(root, driverFactory = JdbcAccountDriverFactory(), io = io)
            val repository = SettingsRepository(database)
            val outbox = OutboxRepository(database)
            val permit = SessionMutationPermit()
            val deps =
                object : TestRemindersDependencies() {
                    override fun reminderSettings() =
                        repository
                            .values()
                            .map { rows ->
                                ReminderSettings.decode(rows.firstOrNull { it.section == "reminders" }?.value)
                            }.distinctUntilChanged()

                    override suspend fun enqueueReminders(patch: JsonObject) {
                        outbox.enqueue(OutboxChange.Settings(SettingsSection.Reminders, patch), permit, now = syncDate)
                    }
                }
            val model = RemindersViewModel(deps, io)
            val relaunched = RemindersViewModel(deps, io)
            try {
                database.open(syncAccount)
                AccountRepository(database).upsert(AccountRecord(syncAccount, "Demo", "demo@example.com"))
                val defaults = """{"light":true,"avoid":true,"caffeine":true,"winddown":true,"leadMin":30}"""
                repository.upsert(SettingsRecord("reminders", defaults))
                model.start()
                runCurrent()
                assertTrue(model.state.value.active)
                model.setEnabled(false)
                runCurrent()
                model.setLead(60)
                runCurrent()
                val rows = outbox.pending()
                assertEquals(2, rows.size)
                assertTrue(rows.all { it.kind == "settings" && OutboxPayload.dependency(it) == "settings:reminders" })
                val original = snapshot()
                val originalSettings = original.settings
                val incoming =
                    net.chronoflow.core.model.api.ApiJson.response
                        .parseToJsonElement(defaults) as JsonObject
                val incomingSettings =
                    AccountSettings(
                        originalSettings.bodyClock,
                        originalSettings.sleepAnswered,
                        originalSettings.preferences,
                        incoming,
                        originalSettings.planCredits,
                        originalSettings.policies,
                    )
                val account =
                    AccountSnapshot(
                        original.id,
                        original.name,
                        original.email,
                        original.hasVerifiedEmail,
                        incomingSettings,
                        original.readiness,
                        original.billing,
                        original.security,
                    )
                SqlSyncDatabase(database).applyPull(
                    SyncEpoch(AccountID.of(syncAccount), 1),
                    account,
                    emptyList(),
                    syncDate.toString(),
                    PlanRetention(),
                )
                val local =
                    checkNotNull(
                        ReminderSettings.decode(repository.settings().single { it.section == "reminders" }.value),
                    )
                assertFalse(local.enabled)
                assertEquals(60, local.leadMin)
                model.stop()
                relaunched.start()
                runCurrent()
                assertFalse(relaunched.state.value.active)
                assertEquals(
                    60,
                    relaunched.state.value.settings
                        ?.leadMin,
                )
                deps.authorization = ReminderPermission.Blocked
                relaunched.refreshPermission()
                runCurrent()
                assertEquals(ReminderPermission.Blocked, relaunched.state.value.permission)
                deps.authorization = ReminderPermission.Granted
                relaunched.refreshPermission()
                runCurrent()
                assertTrue(
                    relaunched.state.value.settings
                        ?.enabled == true,
                )
                assertEquals(
                    60,
                    relaunched.state.value.settings
                        ?.leadMin,
                )
                relaunched.stop()
                permit.invalidate()
                val count = outbox.pending().size
                relaunched.setEnabled(false)
                runCurrent()
                assertEquals(count, outbox.pending().size)
                assertFalse(relaunched.state.value.busy)
                assertFalse(relaunched.failed)
            } finally {
                withContext(NonCancellable) {
                    model.stop()
                    relaunched.stop()
                    model.viewModelScope.coroutineContext.job
                        .cancelAndJoin()
                    relaunched.viewModelScope.coroutineContext.job
                        .cancelAndJoin()
                    database.close()
                    check(root.deleteRecursively())
                }
            }
        }
}
