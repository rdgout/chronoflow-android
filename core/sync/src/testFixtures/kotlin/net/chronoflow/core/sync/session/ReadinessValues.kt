package net.chronoflow.core.sync.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.session.StoredAccountReadiness
import net.chronoflow.core.testing.sync.syncAccount

class ReadinessValues(
    gate: Gate? = Gate.App,
    stored: Boolean = false,
) : AccountReadinessSource {
    val state = MutableStateFlow<StoredAccountReadiness?>(StoredAccountReadiness(syncAccount, gate, stored))
    var reads = 0
        private set
    var fails = false

    override suspend fun values(accountId: String): Flow<StoredAccountReadiness?> {
        reads += 1
        if (fails) throw SessionFailure.DatabaseUnavailable
        return state
    }

    fun set(
        gate: Gate? = Gate.App,
        stored: Boolean,
    ) {
        state.value = StoredAccountReadiness(syncAccount, gate, stored)
    }
}
