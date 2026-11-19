package net.chronoflow.feature.trip.state

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.plan.trippage.TripPageFetch
import net.chronoflow.core.sync.SyncPathMonitoring
import java.io.IOException
import java.security.GeneralSecurityException

class TripPageFetchModel(
    private val tripId: String,
    private val fetchDetail: suspend (String) -> Unit,
    private val path: SyncPathMonitoring?,
    private val scope: CoroutineScope,
) {
    private val _online = MutableStateFlow(true)
    val online = _online.asStateFlow()
    private val _fetch = MutableStateFlow(TripPageFetch.Idle)
    val fetch = _fetch.asStateFlow()
    private var started = false
    private var reconnectPending = false
    private var pathJob: Job? = null
    private var fetchJob: Job? = null
    private var stopJob: Job? = null

    fun setOnline(value: Boolean) {
        val old = _online.value
        _online.value = value
        if (value && !old && started) {
            if (fetchJob?.isActive == true) reconnectPending = true else retry()
        }
    }

    fun start() {
        if (started) return
        started = true
        if (path != null) {
            val previousStop = stopJob
            pathJob =
                scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    previousStop?.join()
                    path.updates().collect { setOnline(it) }
                }
        }
        retry()
    }

    fun retry() {
        if (fetchJob?.isActive == true) return
        if (!online.value) {
            _fetch.value = TripPageFetch.Offline
            return
        }
        _fetch.value = TripPageFetch.Loading
        val job =
            scope.launch(start = CoroutineStart.LAZY) {
                try {
                    fetchDetail(tripId)
                    currentCoroutineContext().ensureActive()
                    _fetch.value = TripPageFetch.Loaded
                } catch (cancelled: CancellationException) {
                    if (!currentCoroutineContext().isActive) throw cancelled
                    _fetch.value = TripPageFetch.Failed
                } catch (failure: DataFailure) {
                    currentCoroutineContext().ensureActive()
                    _fetch.value =
                        when {
                            failure == DataFailure.Offline -> TripPageFetch.Offline
                            failure.status == 404 -> TripPageFetch.NotFound
                            else -> TripPageFetch.Failed
                        }
                } catch (_: SessionFailure) {
                    currentCoroutineContext().ensureActive()
                    _fetch.value = TripPageFetch.Failed
                } catch (_: DeviceIdentityFailure) {
                    currentCoroutineContext().ensureActive()
                    _fetch.value = TripPageFetch.Failed
                } catch (_: StoreFailure) {
                    currentCoroutineContext().ensureActive()
                    _fetch.value = TripPageFetch.Failed
                } catch (_: IOException) {
                    currentCoroutineContext().ensureActive()
                    _fetch.value = TripPageFetch.Failed
                } catch (_: GeneralSecurityException) {
                    currentCoroutineContext().ensureActive()
                    _fetch.value = TripPageFetch.Failed
                } catch (_: RuntimeException) {
                    currentCoroutineContext().ensureActive()
                    _fetch.value = TripPageFetch.Failed
                } finally {
                    if (currentCoroutineContext().isActive) {
                        fetchJob = null
                        if (reconnectPending) {
                            reconnectPending = false
                            if (online.value) retry()
                        }
                    }
                }
            }
        fetchJob = job
        job.start()
    }

    fun stop() {
        val stoppingPath = pathJob
        stoppingPath?.cancel()
        pathJob = null
        fetchJob?.cancel()
        fetchJob = null
        reconnectPending = false
        started = false
        val previousStop = stopJob
        stopJob =
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                withContext(NonCancellable) {
                    previousStop?.join()
                    stoppingPath?.join()
                    path?.stop()
                }
            }
    }
}
