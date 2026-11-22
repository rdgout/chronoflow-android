package net.chronoflow.core.data.reminders

import kotlinx.coroutines.CompletableDeferred
import net.chronoflow.core.reminders.ReminderRequest

class FakeReminderAlarms : ReminderAlarms {
    val active = linkedMapOf<String, Pair<ReminderRequest, Int>>()
    val sets = mutableListOf<ReminderRequest>()
    val cancels = mutableListOf<String>()
    private val rejected = mutableSetOf<String>()
    private var hold: CompletableDeferred<Unit>? = null
    private var pausesNext = false
    var setting = false
        private set
    var maximumConcurrentSets = 0
        private set
    private var concurrent = 0

    fun reject(blockId: String) {
        rejected += blockId
    }

    fun holdNext() {
        pausesNext = true
        hold = CompletableDeferred()
    }

    fun release() {
        hold?.complete(Unit)
    }

    override suspend fun set(
        request: ReminderRequest,
        requestCode: Int,
    ) {
        setting = true
        concurrent++
        maximumConcurrentSets = maxOf(maximumConcurrentSets, concurrent)
        try {
            if (pausesNext) {
                pausesNext = false
                hold?.await()
            }
            if (request.blockID in rejected) throw ReminderAlarmRefused(null)
            active[request.identifier] = request to requestCode
            sets += request
        } finally {
            setting = false
            concurrent--
        }
    }

    override suspend fun cancel(
        identifier: String,
        requestCode: Int,
    ) {
        cancels += identifier
        active.remove(identifier)
    }
}
