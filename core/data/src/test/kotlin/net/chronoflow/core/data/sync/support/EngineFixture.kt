package net.chronoflow.core.data.sync.support

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.withContext
import net.chronoflow.core.sync.SyncEngine
import net.chronoflow.core.sync.SyncScheduler
import net.chronoflow.core.testing.sync.ManualSyncScheduler
import net.chronoflow.core.testing.sync.ScriptedSource
import net.chronoflow.core.testing.sync.TestGate
import net.chronoflow.core.testing.sync.syncDate

internal class EngineFixture(
    val store: SyncStore,
    val source: ScriptedSource,
    val engine: SyncEngine,
    val scheduler: ManualSyncScheduler,
    val dispatcher: CoroutineDispatcher,
) {
    private val gates = mutableListOf<TestGate>()

    fun gate(): TestGate = TestGate().also { gates += it }

    suspend fun close() =
        withContext(NonCancellable) {
            gates.forEach { it.release() }
            engine.cancelAndWait()
            store.close()
        }

    companion object {
        suspend fun make(scope: TestScope): EngineFixture {
            val dispatcher = StandardTestDispatcher(scope.testScheduler)
            val store = SyncStore(dispatcher)
            val source = ScriptedSource()
            val scheduler = ManualSyncScheduler()
            val engine =
                SyncEngine(
                    store.sync,
                    source,
                    store.epoch,
                    scheduler = SyncScheduler({ syncDate }, scheduler::sleep),
                    dispatcher = dispatcher,
                )
            return EngineFixture(store, source, engine, scheduler, dispatcher)
        }
    }
}
