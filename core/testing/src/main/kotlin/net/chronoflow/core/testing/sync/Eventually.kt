package net.chronoflow.core.testing.sync

import kotlinx.coroutines.yield

suspend fun eventually(condition: suspend () -> Boolean): Boolean {
    repeat(1000) {
        if (condition()) return true
        yield()
    }
    return false
}
