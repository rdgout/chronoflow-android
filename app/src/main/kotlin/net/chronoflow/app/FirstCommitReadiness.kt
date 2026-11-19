package net.chronoflow.app

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.sync.session.UnavailableReadiness

internal class FirstCommitReadiness(
    private val firstCommit: Deferred<Unit>,
    private val source: () -> LaunchReadiness,
    private val io: CoroutineDispatcher,
) : LaunchReadiness {
    private fun native(): LaunchReadiness =
        try {
            source()
        } catch (
            cancelled: CancellationException,
        ) {
            throw cancelled
        } catch (_: Exception) {
            UnavailableReadiness
        }

    override suspend fun session(): SessionPresence {
        firstCommit.await()
        return withContext(io) { native().session() }
    }

    override suspend fun gate(): Gate {
        firstCommit.await()
        return withContext(io) { native().gate() }
    }

    override suspend fun hasStoredData(): Boolean {
        firstCommit.await()
        return withContext(io) { native().hasStoredData() }
    }

    override fun gateUpdates(): Flow<Gate> =
        flow {
            firstCommit.await()
            emitAll(native().gateUpdates())
        }.flowOn(io)

    override fun storedDataUpdates(): Flow<Boolean> =
        flow {
            firstCommit.await()
            emitAll(native().storedDataUpdates())
        }.flowOn(io)

    override suspend fun allowFirstLoad() {
        firstCommit.await()
        withContext(io) { native().allowFirstLoad() }
    }
}
