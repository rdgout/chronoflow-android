@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.app

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.network.ApiDataSource
import net.chronoflow.core.network.DataSourceFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class DataSourceSelectionTests {
    @Test fun defaultsAndIsolation() {
        val mock = DataSourceFactory("mock", release = false, testing = false)
        val api = DataSourceFactory("api", release = true, testing = false)
        assertEquals(StorageNamespace.Mock, mock.namespace)
        assertEquals(StorageNamespace.Api, api.namespace)
        assertNotEquals(mock.namespace.directoryName, api.namespace.directoryName)
        assertNotEquals(mock.namespace.keychainService, api.namespace.keychainService)
    }

    @Test fun apiFactoryNeverConstructsMockSeed() =
        runTest(timeout = 60.seconds) {
            var called = false
            val sources =
                SessionDataSources(
                    DataSourceFactory("api", release = true, testing = false),
                    { error("API selection requested mock storage") },
                    GraphOverrides.None,
                    MockSources { _, _, _ ->
                        called = true
                        error("API selection constructed mock")
                    },
                    { null },
                    StandardTestDispatcher(testScheduler),
                )
            assertTrue(
                sources.forSession(StoredSession("test", "019a562b-8000-7000-8000-000000000001")) is ApiDataSource,
            )
            assertEquals(false, called)
        }
}
