@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.wizard.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.testing.wizard.CheckingTestClock
import net.chronoflow.core.testing.wizard.FakeWizardGeneration
import net.chronoflow.core.testing.wizard.InMemoryWizardDraftStore
import net.chronoflow.core.testing.wizard.WizardTestClock
import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.wizard.generation.TripGenerationSaveResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

class MemberWizardLifecycleTest {
    @Test fun discardFinishesPendingWriteAndReleasesWriter() =
        runTest(timeout = 60.seconds) {
            withWriter { f ->
                runCurrent()
                assertTrue(f.owner.children.any { it.isActive })
                f.model.go(MemberWizardStep.outbound)
                f.model.discard()
                runCurrent()
                assertEquals(
                    1,
                    f.store.last
                        ?.state
                        ?.revision,
                )
                assertEquals(listOf(f.model.draft.state.instance), f.store.discarded)
                assertFalse(f.owner.children.any { it.isActive })
            }
        }

    @Test fun successfulSaveReleasesWriter() =
        runTest(timeout = 60.seconds) {
            withWriter { f ->
                f.generation.outcomes.add(TripGenerationSaveResult.Outcome.Saved("created"))
                f.model.savePlan()
                runCurrent()
                assertTrue(f.model.closed)
                assertEquals("created", f.model.createdTripID)
                assertEquals(listOf(f.model.draft.state.instance to true), f.store.saves)
                assertFalse(f.owner.children.any { it.isActive })
            }
        }

    @Test fun failedDiscardKeepsWriterAvailableForEditsAndRetry() =
        runTest(timeout = 60.seconds) {
            withWriter { f ->
                f.failDiscard = true
                var failed = false
                try {
                    f.model.discard()
                } catch (_: IOException) {
                    failed = true
                }
                assertTrue(failed)
                assertFalse(f.model.closed)
                f.model.go(MemberWizardStep.outbound)
                f.model.flush()
                assertEquals(
                    1,
                    f.store.last
                        ?.state
                        ?.revision,
                )
                assertTrue(f.owner.children.any { it.isActive })
                f.failDiscard = false
                f.model.discard()
                runCurrent()
                assertFalse(f.owner.children.any { it.isActive })
            }
        }
}

private class WriterFixture(
    scope: TestScope,
) {
    val owner = Job(scope.backgroundScope.coroutineContext.job)
    val store = InMemoryWizardDraftStore()
    val generation = FakeWizardGeneration()
    var failDiscard = false
    val model: MemberWizardModel

    init {
        val clock = WizardTestClock()
        val review = generationReview()
        val initial =
            MemberWizardDraft(
                form = WizardForm.from(generationInput()).copy(identity = review.identity),
                state =
                    MemberWizardState(
                        step = MemberWizardStep.ready,
                        review = review,
                        reviewedAt = clock.now,
                        sleepNeeded = false,
                    ),
            )
        val draft = initial.copy(state = initial.state.copy(checkedInput = initial.inputFingerprint))
        val recovery =
            object : WizardDraftStore by store {
                override suspend fun discard(instance: String) {
                    if (failDiscard) throw IOException()
                    store.discard(instance)
                }
            }
        model =
            MemberWizardModel(
                draft,
                recovery,
                scope.backgroundScope,
                { clock.now },
                generation,
                CheckingTestClock(scope.testScheduler).clock,
                writerScope = CoroutineScope(scope.backgroundScope.coroutineContext + owner),
            )
    }
}

private suspend fun TestScope.withWriter(block: suspend TestScope.(WriterFixture) -> Unit) {
    val fixture = WriterFixture(this)
    try {
        block(fixture)
    } finally {
        withContext(NonCancellable) { fixture.owner.cancelAndJoin() }
    }
}
