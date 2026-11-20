package net.chronoflow.feature.onboarding.terms

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.feature.onboarding.SessionChangedException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TermsUpdateModelTests {
    @Test fun canAcceptRequiresLoadedUnacceptedPoliciesAndIdle() =
        runTest(timeout = 60.seconds) {
            val gate = CompletableDeferred<Unit>()
            val entered = CompletableDeferred<Unit>()
            val model =
                TermsUpdateModel {
                    entered.complete(Unit)
                    gate.await()
                }
            assertFalse(model.state.value.canAccept)
            model.update(AccountPolicies("1", "1", "1"))
            assertFalse(model.state.value.canAccept)
            model.update(AccountPolicies("1", "1"))
            assertTrue(model.state.value.canAccept)
            val job = launch { model.accept() }
            entered.await()
            assertFalse(model.state.value.canAccept)
            gate.complete(Unit)
            job.join()
            assertTrue(model.state.value.completed)
            assertFalse(model.state.value.canAccept)
        }

    @Test fun newVersionResetsCompletedAndFailed() =
        runTest(timeout = 60.seconds) {
            var fail = true
            val model = TermsUpdateModel(AccountPolicies("1", "1")) { if (fail) error("save") }
            model.accept()
            assertTrue(model.state.value.failed)
            model.update(AccountPolicies("2", "2"))
            assertFalse(model.state.value.failed)
            fail = false
            model.accept()
            assertTrue(model.state.value.completed)
            model.update(AccountPolicies("3", "3"))
            assertFalse(model.state.value.completed)
            assertFalse(model.state.value.failed)
        }

    @Test fun sessionChangedIsSilent() =
        runTest(timeout = 60.seconds) {
            val model = TermsUpdateModel(AccountPolicies("1", "1")) { throw SessionChangedException() }
            model.accept()
            assertFalse(model.state.value.failed)
            assertFalse(model.state.value.completed)
        }

    @Test fun cancellationIsRethrownAndBusyResets() =
        runTest(timeout = 60.seconds) {
            val entered = CompletableDeferred<Unit>()
            val model =
                TermsUpdateModel(AccountPolicies("1", "1")) {
                    entered.complete(Unit)
                    awaitCancellation()
                }
            val job =
                launch {
                    model.accept()
                    fail("caller cancellation returned normally")
                }
            entered.await()
            assertTrue(model.state.value.busy)
            job.cancelAndJoin()
            assertFalse(model.state.value.busy)
            assertFalse(model.state.value.failed)
            assertFalse(model.state.value.completed)
        }

    @Test fun stalePermitIsSilent() =
        runTest(timeout = 60.seconds) {
            val model = TermsUpdateModel(AccountPolicies("1", "1")) { throw StaleSessionException() }
            model.accept()
            assertFalse(model.state.value.failed)
            assertFalse(model.state.value.completed)
        }
}
