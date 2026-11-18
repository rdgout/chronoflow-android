package net.chronoflow.core.model.session

import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.AccountID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SyncEpochTests {
    private fun epoch() = SyncEpoch(AccountID.of("019a562b-8000-7000-8000-000000000001"), 1)

    @Test fun invalidEpochRefusesAndChildFollowsParent() {
        val parent = epoch()
        val child = parent.child()
        child.whileValid { }
        parent.invalidate()
        assertThrows(StaleSessionException::class.java) { parent.whileValid { } }
        assertThrows(StaleSessionException::class.java) { child.whileValid { } }
        assertFalse(parent.isValid)
        assertFalse(child.isValid)
        val other = epoch()
        other.child().invalidate()
        assertTrue(other.isValid)
    }

    @Test fun invalidateWaitsForAnAdmittedOperation() {
        val epoch = epoch()
        assertInvalidationWaits({ epoch.whileValid(it) }, epoch::invalidate)
        assertThrows(StaleSessionException::class.java) { epoch.whileValid { } }
    }

    @Test fun nullableResultsPassThrough() {
        var calls = 0
        assertNull(
            epoch().child().whileValid<String?> {
                calls++
                null
            },
        )
        assertEquals(1, calls)
    }

    @Test fun checkThrowsWhenTheCallerIsCancelled() =
        runTest(timeout = 60.seconds) {
            val epoch = epoch()
            var reachedCheck = false
            var returned = false
            val job =
                launch {
                    coroutineContext.job.cancel()
                    reachedCheck = true
                    epoch.check()
                    returned = true
                }
            job.join()
            assertTrue(reachedCheck)
            assertFalse("check returned after cancellation", returned)
            assertTrue(epoch.isValid)
        }
}
