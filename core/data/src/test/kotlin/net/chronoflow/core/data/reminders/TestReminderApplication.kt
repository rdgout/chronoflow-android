package net.chronoflow.core.data.reminders

import android.app.Application
import android.content.ComponentName
import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.testing.reminders.reminderAccount
import net.chronoflow.core.testing.reminders.reminderNow
import java.time.Instant

class TestReminderApplication :
    Application(),
    ReminderHost {
    override lateinit var reminderRuntime: ReminderRuntime
}

class FakeReminderRuntime(
    private val context: Context,
    override val io: CoroutineDispatcher,
    override val scope: CoroutineScope,
) : ReminderRuntime {
    override val notificationIcon = android.R.drawable.ic_dialog_info
    override val tapComponent = ComponentName(context, "x.Main")
    override var clock: () -> Instant = { reminderNow }
    var account: String? = reminderAccount
    var accountReads = 0
    val deliveries = mutableListOf<Pair<String, String>>()
    var passes = 0
    var pass: suspend () -> Unit = {}
    private val deliveryGate = ReminderDeliveryGate()

    override suspend fun storedAccountId(): String? {
        accountReads++
        return account
    }

    override suspend fun ensureChannel() {
        AndroidReminderNotices(context, io) { channelName(context, it) }.ensureChannel(Language.En)
    }

    override suspend fun postIfCurrent(
        accountId: String,
        post: () -> Unit,
    ): Boolean = deliveryGate.postIfCurrent(accountId, ::storedAccountId, post)

    override suspend fun delivered(
        accountId: String,
        identifier: String,
    ) {
        deliveries += accountId to identifier
    }

    override suspend fun backgroundPass() {
        passes++
        pass()
    }
}
