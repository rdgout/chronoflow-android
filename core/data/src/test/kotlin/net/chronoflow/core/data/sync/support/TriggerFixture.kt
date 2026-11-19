package net.chronoflow.core.data.sync.support

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.withContext
import net.chronoflow.core.sync.SyncLifecycle
import net.chronoflow.core.sync.SyncPathMonitoring
import net.chronoflow.core.sync.SyncScheduler
import net.chronoflow.core.testing.sync.ManualSyncScheduler
import net.chronoflow.core.testing.sync.ScriptedSource
import net.chronoflow.core.testing.sync.TestGate
import net.chronoflow.core.testing.sync.syncDate
import java.time.Instant

internal class TriggerPath : SyncPathMonitoring {
    private val channel = Channel<Boolean>(Channel.UNLIMITED)
    var stopped = false
        private set

    override fun updates(): Flow<Boolean> = channel.receiveAsFlow()

    suspend fun send(online: Boolean) {
        channel.send(online)
    }

    override suspend fun stop() {
        stopped = true
        channel.close()
    }
}

internal class TriggerEvents {
    val unauthorized = mutableListOf<Long>()
    val verification = mutableListOf<Long>()

    fun refused(epoch: Long) {
        unauthorized += epoch
    }

    fun verify(epoch: Long) {
        verification += epoch
    }
}

internal class TriggerTime {
    private var value = syncDate

    fun now(): Instant = value

    fun advance(seconds: Double) {
        value = value.plusMillis((seconds * 1000).toLong())
    }
}

internal class TriggerFixture private constructor(
    val store: SyncStore,
    val dispatcher: CoroutineDispatcher,
    val source: ScriptedSource,
    val clock: ManualSyncScheduler,
    val path: TriggerPath,
    val events: TriggerEvents,
    val time: TriggerTime,
    val lifecycle: SyncLifecycle,
) {
    val scheduler: SyncScheduler get() = SyncScheduler(time::now, clock::sleep)
    private val gates = mutableListOf<TestGate>()

    fun gate(): TestGate = TestGate().also { gates += it }

    suspend fun close() =
        withContext(NonCancellable) {
            gates.forEach { it.release() }
            lifecycle.stop()
            store.close()
        }

    companion object {
        suspend fun make(scope: TestScope): TriggerFixture {
            val dispatcher = StandardTestDispatcher(scope.testScheduler)
            val store = SyncStore(dispatcher)
            val source = ScriptedSource()
            val clock = ManualSyncScheduler()
            val path = TriggerPath()
            val events = TriggerEvents()
            val time = TriggerTime()
            val lifecycle =
                SyncLifecycle(
                    store.sync,
                    source,
                    store.epoch,
                    SyncScheduler(time::now, clock::sleep),
                    path = path,
                    unauthorized = { events.refused(it) },
                    verification = { events.verify(it) },
                    dispatcher = dispatcher,
                )
            return TriggerFixture(store, dispatcher, source, clock, path, events, time, lifecycle)
        }
    }
}
