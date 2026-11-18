package net.chronoflow.core.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce

class Debounced {
    fun updates(source: Flow<Int>): Flow<Int> = source.debounce(1000)
}
