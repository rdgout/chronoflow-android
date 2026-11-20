package net.chronoflow.core.wizard.generation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.testing.wizard.GenerationGate
import net.chronoflow.core.wizard.model.RecoveryRejected
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class TripGenerationDuplicateTests(
    private val failure: Exception,
) {
    @Test fun didCreateFailureRetryReusesCreatedTrip() =
        runTest(timeout = 60.seconds) {
            withGeneration { source, _, service ->
                val initial = generationState()
                val failed = service.save(initial) { throw failure }
                assertEquals(TripGenerationSaveResult.Outcome.Failed(DataFailure.Server(null)), failed.outcome)
                assertEquals("created", failed.state.savedTripId)
                assertEquals(TripGenerationSaveResult.Outcome.Saved("created"), service.save(failed.state).outcome)
                assertEquals(TripGenerationSaveResult.Outcome.Saved("created"), service.save(initial).outcome)
                assertEquals(1, source.count("save"))
                assertEquals(0, source.count("update"))

                val edit = generationState("edit", TripEditContext("trip", "revision"))
                val editFailure = service.save(edit) { throw failure }
                assertEquals("trip", editFailure.state.savedTripId)
                assertEquals(TripGenerationSaveResult.Outcome.Failed(DataFailure.Server(null)), editFailure.outcome)
                service.save(editFailure.state)
                service.save(edit)
                assertEquals(1, source.count("update"))
                assertEquals(1, source.count("save"))

                val started = GenerationGate()
                val cancelled =
                    backgroundScope.async {
                        service.save(generationState("cancelled")) {
                            started.open()
                            awaitCancellation()
                        }
                    }
                started.wait()
                cancelled.cancelAndJoin()
                assertTrue(generationFailure { cancelled.await() } is CancellationException)
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun failures(): List<Array<Any>> =
            listOf(StoreFailure.Unavailable(), RecoveryRejected(), StaleSessionException()).map {
                arrayOf(it)
            }
    }
}
