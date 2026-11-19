package net.chronoflow.core.model.launch

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.chronoflow.core.model.session.SessionPresence

enum class LaunchEnding { Welcome, App }

enum class Gate(
    val rawValue: String,
) {
    Verify("verify"),
    Terms("terms"),
    Walkthrough("walkthrough"),
    App("app"),
}

interface LaunchReadiness {
    suspend fun session(): SessionPresence

    suspend fun gate(): Gate

    fun gateUpdates(): Flow<Gate> = flow { emit(gate()) }

    suspend fun hasStoredData(): Boolean

    fun storedDataUpdates(): Flow<Boolean> = flow { emit(hasStoredData()) }

    suspend fun allowFirstLoad()
}
