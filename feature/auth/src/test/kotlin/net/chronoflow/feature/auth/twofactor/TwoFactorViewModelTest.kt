package net.chronoflow.feature.auth.twofactor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.feature.auth.model.challenged
import net.chronoflow.feature.auth.model.twoFactorSource
import net.chronoflow.feature.auth.testing.FakeSecretField
import net.chronoflow.feature.auth.testing.RecordingRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class TwoFactorViewModelTest {
    @Test fun secondVisitUsesTheCurrentModel() =
        runTest(timeout = 60.seconds) {
            val routes = RecordingRoutes()
            val signIn = challenged(twoFactorSource(StandardTestDispatcher(testScheduler)), routes = routes)
            val vm = TwoFactorViewModel(signIn, routes, backgroundScope)
            val old = signIn.twoFactor.value!!
            vm.onCodeEdited("123")
            runCurrent()
            assertEquals("123", vm.uiState.value?.code)
            vm.back()
            signIn.submitPassword(FakeSecretField("password")) {}
            assertNotSame(old, signIn.twoFactor.value)
            runCurrent()
            assertEquals("", vm.uiState.value?.code)
            vm.onCodeEdited("456")
            runCurrent()
            assertEquals("456", signIn.twoFactor.value?.code)
            assertEquals("", old.code)
            assertFalse(checkNotNull(vm.uiState.value).toString().contains("456"))
        }

    @Test fun backClearsTheChallengeAndDisablesTheOldModel() =
        runTest(timeout = 60.seconds) {
            val routes = RecordingRoutes()
            val signIn = challenged(twoFactorSource(StandardTestDispatcher(testScheduler)), routes = routes)
            val vm = TwoFactorViewModel(signIn, routes, backgroundScope)
            vm.onCodeEdited("123")
            vm.back()
            vm.onCodeEdited("456")
            assertNull(signIn.navigation.challengeID)
            assertEquals("", signIn.twoFactor.value?.code)
        }
}
