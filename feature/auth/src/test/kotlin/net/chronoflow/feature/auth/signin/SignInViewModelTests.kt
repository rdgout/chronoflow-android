package net.chronoflow.feature.auth.signin

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.feature.auth.testing.AUTH_DEVICE_ID
import net.chronoflow.feature.auth.testing.AuthTestFixture
import net.chronoflow.feature.auth.testing.FakeSecretField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTests {
    @Test fun requestLockCoversCommandsBeforeTheModelStarts() =
        runTest(timeout = 60.seconds) {
            val fixture = AuthTestFixture(StandardTestDispatcher(testScheduler))
            val vm = SignInViewModel(fixture.model, backgroundScope)
            vm.editEmail("demo@example.com")
            val secret = FakeSecretField()
            vm.submitPassword(secret) {}
            vm.submitPassword(secret) {}
            vm.submitPasskey(secret)
            runCurrent()
            assertEquals(1, fixture.session.accepted.size)
            assertTrue(secret.cleared)
            assertFalse(vm.uiState.value.form.busy)
        }

    @Test fun leavingCancelsTheRequestAndAllowsAnotherSubmit() =
        runTest(timeout = 60.seconds) {
            val gate = CompletableDeferred<String>()
            val fixture = AuthTestFixture(StandardTestDispatcher(testScheduler), identity = { gate.await() })
            val vm = SignInViewModel(fixture.model, backgroundScope)
            vm.editEmail("demo@example.com")
            val secret = FakeSecretField()
            vm.submitPassword(secret) {}
            runCurrent()
            assertTrue(vm.uiState.value.form.busy)
            vm.leave()
            runCurrent()
            assertTrue(secret.cleared)
            assertFalse(vm.uiState.value.form.busy)
            gate.complete(AUTH_DEVICE_ID)
            vm.submitPassword(FakeSecretField()) {}
            runCurrent()
            assertEquals(1, fixture.session.accepted.size)
        }

    @Test fun emailEditsDoNotRepublishAnUnchangedCardState() =
        runTest(timeout = 60.seconds) {
            val fixture = AuthTestFixture(StandardTestDispatcher(testScheduler))
            val vm = SignInViewModel(fixture.model, backgroundScope)
            var emissions = 0
            backgroundScope.launch { vm.uiState.collect { emissions++ } }
            runCurrent()
            val initial = emissions
            vm.editEmail("a")
            runCurrent()
            vm.editEmail("ab")
            runCurrent()
            assertEquals(initial, emissions)
            assertEquals("ab", vm.email)
        }
}
