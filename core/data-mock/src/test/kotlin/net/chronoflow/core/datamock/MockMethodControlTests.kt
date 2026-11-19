package net.chronoflow.core.datamock

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

/** Every public method obeys the same request controls (new). */
@RunWith(Parameterized::class)
class MockMethodControlTests(
    private val method: MockMethod,
) {
    companion object {
        @JvmStatic @BeforeClass
        fun warmFixtures() {
            warmMockFixtures()
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun rows(): List<Array<MockMethod>> = MockMethod.entries.map { arrayOf(it) }
    }

    @Test fun failNextIsOneShot() =
        runTest(timeout = 60.seconds) {
            val source = mockSource()
            val row = MOCK_METHOD_CALLS.getValue(method)
            row.setup(source)
            source.failNext(method, DataFailure.Server(599))
            assertEquals(DataFailure.Server(599), failure { row.call(source) })
            val next =
                try {
                    row.call(source)
                    null
                } catch (failure: DataFailure) {
                    failure
                }
            assertNotEquals(DataFailure.Server(599), next)
        }

    @Test fun offlineFailsBeforeFailNext() =
        runTest(timeout = 60.seconds) {
            val source = mockSource()
            val row = MOCK_METHOD_CALLS.getValue(method)
            row.setup(source)
            source.failNext(method, DataFailure.Server(599))
            source.setOffline(true)
            assertEquals(DataFailure.Offline, failure { row.call(source) })
            source.setOffline(false)
            assertEquals(DataFailure.Server(599), failure { row.call(source) })
        }

    @Test fun mutationsOfflineBlocksOnlySend() =
        runTest(timeout = 60.seconds) {
            val source = mockSource(mutationsOffline = true)
            val row = MOCK_METHOD_CALLS.getValue(method)
            row.setup(source)
            val observed =
                try {
                    row.call(source)
                    null
                } catch (failure: DataFailure) {
                    failure
                }
            assertEquals(method == MockMethod.Send, observed == DataFailure.Offline)
        }
}
