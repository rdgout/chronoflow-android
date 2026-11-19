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
            pathJob =
                scope.launch(start = CoroutineStart.UNDISPATCHED) {
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
                    throw cancelled
                } catch (failure: DataFailure) {
                    _fetch.value =
                        when {
                            failure == DataFailure.Offline -> TripPageFetch.Offline
                            failure.status == 404 -> TripPageFetch.NotFound
                            else -> TripPageFetch.Failed
                        }
                } catch (_: SessionFailure) {
                    _fetch.value = TripPageFetch.Failed
                } catch (_: DeviceIdentityFailure) {
                    _fetch.value = TripPageFetch.Failed
                } catch (_: StoreFailure) {
                    _fetch.value = TripPageFetch.Failed
                } catch (_: IOException) {
                    _fetch.value = TripPageFetch.Failed
                } catch (_: GeneralSecurityException) {
                    _fetch.value = TripPageFetch.Failed
                } catch (_: RuntimeException) {
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
        pathJob?.cancel()
        pathJob = null
        fetchJob?.cancel()
        fetchJob = null
        reconnectPending = false
        started = false
        scope.launch(start = CoroutineStart.UNDISPATCHED) { withContext(NonCancellable) { path?.stop() } }
    }
}
