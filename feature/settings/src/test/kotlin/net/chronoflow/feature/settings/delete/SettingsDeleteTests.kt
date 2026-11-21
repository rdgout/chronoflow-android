@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.delete

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.feature.settings.testing.FakeSecretField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class SettingsDeleteTests {
    @Test fun deletionWorksAgainAfterStoppingAndReturning() =
        runTest(timeout = 60.seconds) {
            var calls = 0
            val model = SettingsDeleteModel({ calls++ })
            model.confirmDeletion()
            model.submit(FakeSecretField(""))
            assertEquals("Enter your password.", model.state.value.errorKey)
            model.stop()
            assertEquals(SettingsDeleteModel.Stage.Confirmation, model.state.value.stage)
            assertNull(model.state.value.errorKey)
            assertFalse(model.state.value.busy)
            assertEquals(0, calls)
            model.confirmDeletion()
            model.submit(FakeSecretField("pw"))
            assertEquals(1, calls)
            assertEquals(SettingsDeleteModel.Stage.Deleted, model.state.value.stage)
            assertFalse(model.state.value.busy)
        }

    @Test fun lateDeletionCannotFinishANewRequest() =
        runTest(timeout = 60.seconds) {
            val previousResponse = CompletableDeferred<Unit>()
            val currentResponse = CompletableDeferred<Unit>()
            var calls = 0
            val model = SettingsDeleteModel({ if (calls++ == 0) previousResponse.await() else currentResponse.await() })
            model.confirmDeletion()
            val previous = launch { model.submit(FakeSecretField("pw")) }
            runCurrent()
            model.stop()
            assertEquals(SettingsDeleteModel.Stage.Confirmation, model.state.value.stage)
            assertFalse(model.state.value.busy)
            model.confirmDeletion()
            val current = launch { model.submit(FakeSecretField("pw")) }
            runCurrent()
            previousResponse.complete(Unit)
            previous.join()
            assertEquals(SettingsDeleteModel.Stage.Password, model.state.value.stage)
            assertTrue(model.state.value.busy)
            currentResponse.complete(Unit)
            current.join()
            assertEquals(SettingsDeleteModel.Stage.Deleted, model.state.value.stage)
            assertFalse(model.state.value.busy)
        }

    @Test fun exactConfirmationAndPasswordAreRequiredAndCredentialsClearAfterRequest() =
        runTest(timeout = 60.seconds) {
            val sent = mutableListOf<String>()
            val model = SettingsDeleteModel({ sent += it })
            val password = FakeSecretField("password")
            model.submit(password)
            assertTrue(sent.isEmpty())
            assertFalse(password.cleared)
            model.confirmDeletion()
            model.submit(FakeSecretField(""))
            assertEquals("Enter your password.", model.state.value.errorKey)
            assertTrue(sent.isEmpty())
            model.submit(password)
            assertEquals(listOf("password"), sent)
            assertTrue(password.cleared)
            assertEquals(SettingsDeleteModel.Stage.Deleted, model.state.value.stage)
        }

    @Test fun throttleBlocksSubmitUntilRetryAtPasses() =
        runTest(timeout = 60.seconds) {
            var now = Instant.ofEpochSecond(1000)
            var calls = 0
            val model = SettingsDeleteModel({ if (calls++ == 0) throw DataFailure.Throttled(30.0, 429) }, { now })
            model.confirmDeletion()
            model.submit(FakeSecretField("pw"))
            assertEquals(now.plusSeconds(30), model.state.value.retryAt)
            assertEquals("Too many attempts.", model.state.value.errorKey)
            now = now.plusSeconds(29)
            model.submit(FakeSecretField("pw"))
            assertEquals(1, calls)
            now = now.plusSeconds(1)
            model.submit(FakeSecretField("pw"))
            assertEquals(2, calls)
            assertEquals(SettingsDeleteModel.Stage.Deleted, model.state.value.stage)
        }

    @Test fun cancellationIsRethrownWithoutAKey() =
        runTest(timeout = 60.seconds) {
            val model = SettingsDeleteModel({ throw CancellationException() })
            model.confirmDeletion()
            try {
                model.submit(FakeSecretField("pw"))
                fail("Expected cancellation")
            } catch (_: CancellationException) {
            }
            assertNull(model.state.value.errorKey)
            assertFalse(model.state.value.busy)
        }

    @Test fun staleSessionEndsQuietlyWithoutAKey() =
        runTest(timeout = 60.seconds) {
            val model = SettingsDeleteModel({ throw StaleSessionException() })
            model.confirmDeletion()
            model.submit(FakeSecretField("pw"))
            assertNull(model.state.value.errorKey)
            assertFalse(model.state.value.busy)
            assertEquals(SettingsDeleteModel.Stage.Password, model.state.value.stage)
        }
}
