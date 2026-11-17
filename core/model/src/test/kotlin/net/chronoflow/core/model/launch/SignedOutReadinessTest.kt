package net.chronoflow.core.model.launch

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SignedOutReadinessTest {
    @Test fun signedOutDefaultsMatchIos() =
        runTest(timeout = 60.seconds) {
            assertEquals(SessionPresence.SignedOut, SignedOutReadiness.session())
            assertEquals(Gate.App, SignedOutReadiness.gate())
            assertEquals(true, SignedOutReadiness.hasStoredData())
            assertEquals(listOf(Gate.App), SignedOutReadiness.gateUpdates().toList())
            assertEquals(listOf(true), SignedOutReadiness.storedDataUpdates().toList())
            SignedOutReadiness.allowFirstLoad()
        }
}
