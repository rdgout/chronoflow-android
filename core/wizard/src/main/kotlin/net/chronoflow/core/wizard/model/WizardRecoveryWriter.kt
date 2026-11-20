package net.chronoflow.core.wizard.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class WizardRecoveryWriter(
    private val store: WizardDraftStore,
    writerScope: CoroutineScope,
    private val onFailure: () -> Unit,
) {
    private val sequence = AtomicLong()
    private val latest = AtomicReference<Pair<Long, MemberWizardDraft>?>()
    private val signal = Channel<Unit>(Channel.CONFLATED)
    private var closed = false
    private val written = MutableStateFlow(0L)
    private val consumer =
        writerScope.launch {
            for (ignored in signal) {
                val (number, draft) = latest.get() ?: continue
                try {
                    store.persist(draft)
                } catch (_: CancellationException) {
                    currentCoroutineContext().ensureActive()
                } catch (_: RecoveryRejected) {
                    // A newer wizard instance or a completed save owns the recovery row.
                } catch (_: Exception) {
                    onFailure()
                }
                written.update { maxOf(it, number) }
            }
        }

    @Synchronized
    fun enqueue(draft: MemberWizardDraft) {
        if (closed) return
        latest.set(sequence.incrementAndGet() to draft)
        signal.trySend(Unit)
    }

    @Synchronized
    fun close() {
        closed = true
        signal.close()
    }

    suspend fun flush() {
        val target = sequence.get()
        if (written.value >= target) return
        coroutineScope {
            val progress = async { written.first { it >= target } }
            try {
                select<Unit> {
                    progress.onAwait { }
                    consumer.onJoin { }
                }
            } finally {
                progress.cancel()
            }
        }
    }
}
