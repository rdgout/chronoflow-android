@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.wizard.model

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.wizard.generation.TripGenerationSaveResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class MemberWizardCleanupTest {
    private class CleanupFailure : Exception()

    @Test fun checkedCleanupFailureStillClosesTheCreatedTrip() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardModelFixture(backgroundScope, testScheduler, ready = true)
            val store =
                object : WizardDraftStore by fixture.store {
                    override suspend fun didSave(
                        instance: String,
                        firstTrip: Boolean,
                    ): Unit = throw CleanupFailure()
                }
            fixture.generation.outcomes.add(TripGenerationSaveResult.Outcome.Saved("created"))
            val model =
                MemberWizardModel(
                    fixture.model.draft,
                    store,
                    backgroundScope,
                    { fixture.clock.now },
                    fixture.generation,
                    fixture.checking.clock,
                )
            try {
                model.savePlan()
                runCurrent()
                assertEquals("created", model.createdTripID)
                assertTrue(model.closed)
            } finally {
                fixture.close()
            }
        }
}
