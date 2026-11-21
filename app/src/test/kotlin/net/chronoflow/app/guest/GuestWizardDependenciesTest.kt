package net.chronoflow.app.guest

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.wizard.generation.GuestWizardGeneration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class GuestWizardDependenciesTest {
    @Test fun startClearsSigningInAndBumpsTheRevision() =
        runTest(timeout = 60.seconds) {
            val f = GuestFixture(this)
            try {
                val initial = f.store.start()
                val signingIn = initial.copy(state = initial.state.copy(signingIn = true, revision = 4))
                f.store.persist(signingIn)
                val result = f.dependencies.start()
                assertEquals(false, result.state.signingIn)
                assertEquals(5, result.state.revision)
                assertEquals(result, f.store.recover())
            } finally {
                f.root.deleteRecursively()
            }
        }

    @Test fun startAbortsWhenSignedIn() =
        runTest(timeout = 60.seconds) {
            val f = GuestFixture(this)
            try {
                f.presence = SessionPresence.SignedIn("account")
                try {
                    f.dependencies.start()
                    fail("signed-in guest start accepted")
                } catch (_: CancellationException) {
                }
                assertFalse(f.file.exists())
            } finally {
                f.root.deleteRecursively()
            }
        }

    @Test fun generationIsGuestAndSourceIsPublic() =
        runTest(timeout = 60.seconds) {
            val f = GuestFixture(this)
            val generation: net.chronoflow.core.wizard.generation.MemberWizardGeneration =
                f.dependencies
                    .newGeneration()
            try {
                assertTrue(generation is GuestWizardGeneration)
                f.source.guestHandler = { generationReview() }
                generation.review("guest", ApiTripReviewInput(generationInput(), profile = JsonObject(emptyMap())))
                assertEquals(1, f.publicLoads)
                assertEquals(1, f.source.guestInputs.size)
                assertTrue(f.source.reviewInputs.isEmpty())
            } finally {
                withContext(NonCancellable) {
                    generation.cancel()
                    f.root.deleteRecursively()
                }
            }
        }
}
