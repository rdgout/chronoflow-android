package net.chronoflow.core.testing.wizard

import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.wizard.generation.TripGenerationEffects

class EffectProbe {
    private val lock = Any()
    private var mergeCount = 0
    private var syncCount = 0
    private val pulledIDs = mutableListOf<String>()

    @Volatile private var pullFailure = false
    val merges: Int get() = synchronized(lock) { mergeCount }
    val syncs: Int get() = synchronized(lock) { syncCount }
    val pulls: List<String> get() = synchronized(lock) { pulledIDs.toList() }

    fun failPull() {
        pullFailure = true
    }

    fun merged() {
        synchronized(lock) { mergeCount++ }
    }

    fun synced() {
        synchronized(lock) { syncCount++ }
    }

    fun pulled(id: String) {
        synchronized(lock) { pulledIDs.add(id) }
        if (pullFailure) throw DataFailure.Offline
    }

    fun effects(): TripGenerationEffects = TripGenerationEffects({ merged() }, { pulled(it) }, { synced() })
}
