package net.chronoflow.app

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.session.SessionSnapshot

class SessionActivation(
    private val scope: CoroutineScope,
    private val evictIdleConnections: suspend () -> Unit,
    private val setEnabled: suspend (Boolean) -> Unit,
    private val onForeground: suspend () -> Unit,
    private val retryUnauthorized: suspend () -> Unit,
    private val retryReadiness: suspend () -> Unit,
    private val activeSession: suspend () -> SessionSnapshot?,
    private val resume: suspend (SessionSnapshot) -> Unit,
) : DefaultLifecycleObserver {
    private val mutex = Mutex()
    private var startedOnce = false

    suspend fun prepareBackground() =
        mutex.withLock {
            if (!startedOnce) setEnabled(false)
        }

    override fun onStart(owner: LifecycleOwner) {
        scope.launch { setActive(true) }
    }

    override fun onStop(owner: LifecycleOwner) {
        scope.launch { setActive(false) }
    }

    suspend fun setActive(active: Boolean) =
        mutex.withLock {
            if (!active) {
                withContext(NonCancellable) { setEnabled(false) }
            } else {
                startedOnce = true
                evictIdleConnections()
                setEnabled(true)
                onForeground()
                try {
                    retryUnauthorized()
                } catch (
                    cancelled: CancellationException,
                ) {
                    throw cancelled
                } catch (_: Exception) {
                    return@withLock
                }
                retryReadiness()
                activeSession()?.let { resume(it) }
            }
        }
}
