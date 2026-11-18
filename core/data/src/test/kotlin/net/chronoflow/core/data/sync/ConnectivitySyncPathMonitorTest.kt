package net.chronoflow.core.data.sync

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities
import org.robolectric.shadows.ShadowNetworkInfo
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("DEPRECATION")
class ConnectivitySyncPathMonitorTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val manager = app.getSystemService(ConnectivityManager::class.java)
    private val shadow = shadowOf(manager)

    private fun validated(): NetworkCapabilities {
        val value = ShadowNetworkCapabilities.newInstance()
        shadowOf(value).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        shadowOf(value).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return value
    }

    private fun noNetwork() {
        shadow.setActiveNetworkInfo(null)
        shadow.clearAllNetworks()
        shadow.setStrictUnregistration(true)
    }

    @Test fun currentStateComesFirst() =
        runTest(timeout = 60.seconds) {
            noNetwork()
            val monitor = ConnectivitySyncPathMonitor(app)
            try {
                assertFalse(monitor.updates().first())
                runCurrent()
                assertTrue(shadow.networkCallbacks.isEmpty())
                val info =
                    ShadowNetworkInfo.newInstance(
                        NetworkInfo.DetailedState.CONNECTED,
                        ConnectivityManager.TYPE_WIFI,
                        0,
                        true,
                        true,
                    )
                shadow.setActiveNetworkInfo(info)
                shadow.setDefaultNetworkActive(true)
                val network = requireNotNull(manager.activeNetwork)
                val captive = validated()
                shadowOf(captive).removeCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                shadow.setNetworkCapabilities(network, captive)
                assertFalse(monitor.updates().first())
                shadow.setNetworkCapabilities(network, validated())
                assertTrue(monitor.updates().first())
            } finally {
                monitor.stop()
            }
        }

    @Test fun transitionsAreDistinct() =
        runTest(timeout = 60.seconds) {
            noNetwork()
            val monitor = ConnectivitySyncPathMonitor(app)
            val emitted = mutableListOf<Boolean>()
            val consumer =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    monitor.updates().collect { emitted += it }
                }
            try {
                runCurrent()
                val callback = shadow.networkCallbacks.single()
                val network = ShadowNetwork.newInstance(42)
                callback.onCapabilitiesChanged(network, validated())
                runCurrent()
                callback.onCapabilitiesChanged(network, validated())
                runCurrent()
                callback.onLost(network)
                runCurrent()
                callback.onCapabilitiesChanged(network, validated())
                runCurrent()
                assertEquals(listOf(false, true, false, true), emitted)
                monitor.stop()
                runCurrent()
                assertTrue(consumer.isCompleted)
            } finally {
                monitor.stop()
            }
        }

    @Test fun stopUnregisters() =
        runTest(timeout = 60.seconds) {
            noNetwork()
            val monitor = ConnectivitySyncPathMonitor(app)
            val first =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    monitor.updates().collect { }
                }
            try {
                runCurrent()
                val oldCallback = shadow.networkCallbacks.single()
                monitor.stop()
                runCurrent()
                assertTrue(shadow.networkCallbacks.isEmpty())
                assertTrue(first.isCompleted)
                val second =
                    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                        monitor.updates().collect { }
                    }
                runCurrent()
                assertNotSame(oldCallback, shadow.networkCallbacks.single())
                monitor.stop()
                runCurrent()
                assertTrue(shadow.networkCallbacks.isEmpty())
                assertTrue(second.isCompleted)
            } finally {
                monitor.stop()
            }
        }
}
