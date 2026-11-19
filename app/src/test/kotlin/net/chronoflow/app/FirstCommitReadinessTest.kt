package net.chronoflow.app

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.launch.SignedOutReadiness
import net.chronoflow.core.model.session.SessionPresence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FirstCommitReadinessTest {
    @Test fun headlessRestoreCannotConstructServicesBeforeFirstCommit() =
        runTest(timeout = 60.seconds) {
            var constructions = 0
            val frame = CompletableDeferred<Unit>()
            val source =
                FirstCommitReadiness(frame, {
                    constructions++
                    SignedOutReadiness
                }, StandardTestDispatcher(testScheduler))
            val restore = backgroundScope.async { source.session() }
            runCurrent()
            assertEquals(0, constructions)
            assertFalse(restore.isCompleted)
            frame.complete(Unit)
            assertEquals(SessionPresence.SignedOut, restore.await())
            assertEquals(1, constructions)
        }
}
