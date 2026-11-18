package net.chronoflow.app

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.auth.ResetPasswordLink
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.network.ApiClient
import net.chronoflow.core.network.ApiDataSource
import net.chronoflow.core.network.DataSourceFactory
import java.io.File

fun interface MockSources {
    suspend fun latestResetLink(
        source: DataSource,
        email: String,
    ): ResetPasswordLink? = null

    fun dataSource(
        root: File,
        overrides: GraphOverrides,
        bearer: suspend () -> String?,
    ): DataSource
}

val DataSourceFactory.namespace: StorageNamespace
    get() = if (source == DataSourceFactory.Source.Api) StorageNamespace.Api else StorageNamespace.Mock

class SessionDataSources(
    private val selection: DataSourceFactory,
    private val storageRoot: suspend () -> File,
    private val overrides: GraphOverrides,
    private val mockSources: MockSources?,
    private val bearer: suspend () -> String?,
    private val io: CoroutineDispatcher,
) {
    private val mutex = Mutex()
    private val clients = mutableListOf<ApiClient>()
    private val sessions = mutableMapOf<StoredSession, DataSource>()
    private var publicSource: DataSource? = null
    private var mock: DataSource? = null

    suspend fun forSession(session: StoredSession): DataSource =
        withContext(io) {
            mutex.withLock {
                if (selection.source == DataSourceFactory.Source.Mock) {
                    mock()
                } else {
                    sessions.getOrPut(session) { api { session.token } }
                }
            }
        }

    suspend fun public(): DataSource =
        withContext(io) {
            mutex.withLock {
                if (selection.source == DataSourceFactory.Source.Mock) {
                    mock()
                } else {
                    publicSource ?: api { null }.also { publicSource = it }
                }
            }
        }

    private suspend fun mock(): DataSource =
        mock ?: checkNotNull(mockSources).dataSource(storageRoot(), overrides, bearer).also { mock = it }

    private fun api(bearer: suspend () -> String?): DataSource {
        val client = ApiClient(baseUrl = selection.baseUrl, bearer = bearer, io = io)
        clients += client
        return ApiDataSource(client)
    }

    suspend fun evictIdleConnections() {
        val built = mutex.withLock { clients.toList() }
        built.forEach { it.evictIdleConnections() }
    }
}
