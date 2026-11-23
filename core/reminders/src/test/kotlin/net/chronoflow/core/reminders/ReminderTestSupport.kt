package net.chronoflow.core.reminders

import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.testing.reminders.FakeReminderStrings
import net.chronoflow.core.testing.reminders.reminderAccount
import net.chronoflow.core.testing.reminders.reminderNow
import java.time.Instant

abstract class ReminderTestSupport(
    private val formatter: CopyLineFormatter,
) {
    protected fun scheduled(
        plans: List<ReminderPlan>,
        now: Instant = reminderNow,
        ledger: List<ReminderLedgerEntry> = emptyList(),
        settings: ReminderSettings = ReminderSettings(),
        language: Language = Language.En,
        timeFormat: TimeFormat = TimeFormat.Value24,
        strings: ReminderStrings = FakeReminderStrings(),
    ): List<ReminderRequest> =
        reminderRequests(
            plans,
            reminderAccount,
            settings,
            ledger,
            now,
            language,
            timeFormat,
            formatter,
            strings,
        )
}
