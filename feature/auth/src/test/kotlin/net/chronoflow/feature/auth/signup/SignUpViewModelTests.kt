package net.chronoflow.feature.auth.signup

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.api.AuthLanguage
import net.chronoflow.feature.auth.testing.AUTH_DEVICE_ID
import net.chronoflow.feature.auth.testing.AuthTestFixture
import net.chronoflow.feature.auth.testing.FakeSecretField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpViewModelTests {
    @Test fun submitLocksUntilCompletionAndBackCancelsIt() =
        runTest(timeout = 60.seconds) {
            val gate = CompletableDeferred<String>()
            val fixture = AuthTestFixture(StandardTestDispatcher(testScheduler), identity = { gate.await() })
            var backs = 0
            val model = fixture.model.makeSignUpModel { backs++ }
            val vm = SignUpViewModel(model, backgroundScope)
            val password = FakeSecretField()
            val confirmation = FakeSecretField()
            vm.submit(password, confirmation, AuthLanguage.En) {}
            runCurrent()
            vm.submit(password, confirmation, AuthLanguage.En) {}
            assertTrue(vm.uiState.value.form.busy)
            vm.backToSignIn()
            gate.complete(AUTH_DEVICE_ID)
            runCurrent()
            assertEquals(1, backs)
            assertTrue(fixture.session.accepted.isEmpty())
            assertTrue(password.cleared && confirmation.cleared)
            assertFalse(model.form.busy)
        }

    @Test fun clearingTheEntryCancelsTheModel() =
        runTest(timeout = 60.seconds) {
            val fixture = AuthTestFixture(StandardTestDispatcher(testScheduler))
            val model = fixture.model.makeSignUpModel {}
            model.editName("kept")
            val store = ViewModelStore()
            val vm =
                ViewModelProvider.create(
                    store,
                    viewModelFactory {
                        initializer { SignUpViewModel(model, backgroundScope) }
                    },
                )[SignUpViewModel::class]
            vm.togglePassword()
            assertTrue(model.showsPassword.value)
            store.clear()
            model.editName("ignored")
            assertEquals("kept", model.name.value)
            assertFalse(model.showsPassword.value)
        }
}
