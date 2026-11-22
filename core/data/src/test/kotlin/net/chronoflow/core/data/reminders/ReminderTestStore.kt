package net.chronoflow.core.data.reminders

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.model.store.CheckMarkRecord
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.model.store.TripRecord
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.core.reminders.ReminderPlan
import net.chronoflow.core.reminders.ReminderSettings
import net.chronoflow.core.testing.reminders.FakeReminderStrings
import net.chronoflow.core.testing.reminders.reminderAccount
import net.chronoflow.core.testing.reminders.reminderFormatter
import net.chronoflow.core.testing.reminders.reminderNow
import java.io.File
import java.time.Instant

class ReminderTestStore private constructor(
    val database: AccountDatabase,
    private val root: File,
) {
    suspend fun store(plan: ReminderPlan) {
        TripRepository(database).upsert(
            TripRecord(
                plan.card.id,
                ApiJson.response.encodeToString(TripCardView.serializer(), plan.card),
                currentInputHash = plan.detail.plan.meta.inputHash,
                keepOffline = true,
                departureUtc = plan.card.departureUtc,
            ),
        )
        TripRepository(database).store(
            PlanVersionRecord(
                plan.card.id,
                plan.detail.plan.meta.inputHash,
                ApiJson.response.encodeToString(TripDetail.serializer(), plan.detail).encodeToByteArray(),
                reminderNow.toString(),
                "1",
            ),
            plan.marks.map {
                CheckMarkRecord(
                    plan.card.id,
                    plan.detail.plan.meta.inputHash,
                    it.key.type.raw,
                    it.key.localDate,
                    it.key.ordinal,
                    it.status.raw,
                    it.markedAt,
                )
            },
        )
    }

    suspend fun settings(value: ReminderSettings = ReminderSettings()) {
        SettingsRepository(
            database,
        ).upsert(
            SettingsRecord(
                "reminders",
                """{"light":${value.light},"avoid":${value.avoid},"caffeine":${value.caffeine},"winddown":${value.winddown},"leadMin":${value.leadMin}}""",
            ),
        )
    }

    suspend fun close() =
        withContext(NonCancellable) {
            database.close()
            check(root.deleteRecursively())
        }

    companion object {
        suspend operator fun invoke(
            io: CoroutineDispatcher,
            accountId: String = reminderAccount,
        ): ReminderTestStore {
            val root = TestRoots.temporaryStoreRoot()
            val db = AccountDatabase(root, driverFactory = JdbcAccountDriverFactory(), io = io)
            db.open(accountId)
            AccountRepository(db).upsert(AccountRecord(accountId, "Demo", "demo@example.com"))
            return ReminderTestStore(db, root).also { it.settings() }
        }
    }
}

fun TestScope.reminderService(
    alarms: ReminderAlarms = FakeReminderAlarms(),
    notices: ReminderNotices = FakeReminderNotices(),
    permission: suspend () -> ReminderPermission = { ReminderPermission.Granted },
    language: suspend () -> Language = { Language.En },
    clock: () -> Instant = { reminderNow },
): ReminderService {
    val dispatcher = StandardTestDispatcher(testScheduler)
    return ReminderService(
        alarms,
        notices,
        permission,
        language,
        FakeReminderStrings(),
        { reminderFormatter() },
        PlanDecoder(dispatcher),
        clock,
        dispatcher,
    )
}
