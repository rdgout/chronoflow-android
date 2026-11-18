package net.chronoflow.core.sync

import kotlinx.coroutines.flow.Flow

interface SyncPathMonitoring {
    fun updates(): Flow<Boolean>

    suspend fun stop()
}
