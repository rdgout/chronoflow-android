package net.chronoflow.core.plan

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CopyCatalogProvider(
    private val io: CoroutineDispatcher,
) {
    private val mutex = Mutex()
    private var loaded: CopyCatalog? = null

    suspend fun catalog(): CopyCatalog = mutex.withLock { loaded ?: CopyCatalog.load(io).also { loaded = it } }
}
