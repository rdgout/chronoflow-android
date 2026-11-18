package net.chronoflow.core.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import net.chronoflow.core.sync.SyncPathMonitoring

class ConnectivitySyncPathMonitor(
    context: Context,
) : SyncPathMonitoring {
    private val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    private val lock = Any()
    private val registrations = mutableSetOf<Registration>()

    private class Registration(
        val callback: ConnectivityManager.NetworkCallback,
        val producer: ProducerScope<Boolean>,
    )

    override fun updates(): Flow<Boolean> =
        callbackFlow {
            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities,
                    ) {
                        trySend(validated(networkCapabilities))
                    }

                    override fun onLost(network: Network) {
                        trySend(false)
                    }
                }
            val registration = Registration(callback, this)
            synchronized(lock) {
                trySend(validated(manager.getNetworkCapabilities(manager.activeNetwork)))
                manager.registerDefaultNetworkCallback(callback)
                registrations += registration
            }
            awaitClose {
                synchronized(lock) {
                    if (registrations.remove(registration)) manager.unregisterNetworkCallback(callback)
                }
            }
        }.distinctUntilChanged()

    override suspend fun stop() {
        val closing =
            synchronized(lock) {
                registrations.toList().also { pending ->
                    registrations.clear()
                    pending.forEach { manager.unregisterNetworkCallback(it.callback) }
                }
            }
        closing.forEach { it.producer.close() }
    }

    private fun validated(capabilities: NetworkCapabilities?): Boolean =
        capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
