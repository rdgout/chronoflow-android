package net.chronoflow.core.sync.refusal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.chronoflow.core.sync.SyncRefusal
import net.chronoflow.core.sync.SyncResult
import net.chronoflow.core.sync.SyncState

class RefusedChangeModel {
    private val mutableVisible = MutableStateFlow<SyncRefusal?>(null)
    val visible = mutableVisible.asStateFlow()
    private var pending: SyncRefusal? = null
    private val consumed = ArrayDeque<String>()

    @Synchronized
    fun receive(result: SyncResult) {
        result.refusals.lastOrNull { it.notice != null && it.id !in consumed }?.let { pending = it }
        if (result.state != SyncState.Idle || result.retryAfter != null) return
        pending?.let { mutableVisible.value = it }
        pending = null
    }

    @Synchronized
    fun consume(id: String) {
        if (id !in consumed) {
            consumed.addLast(id)
            if (consumed.size > CONSUMED_LIMIT) consumed.removeFirst()
        }
        if (mutableVisible.value?.id == id) mutableVisible.value = null
        if (pending?.id == id) pending = null
    }

    @Synchronized
    fun clear() {
        pending = null
        mutableVisible.value = null
        consumed.clear()
    }

    private companion object {
        const val CONSUMED_LIMIT = 32
    }
}
