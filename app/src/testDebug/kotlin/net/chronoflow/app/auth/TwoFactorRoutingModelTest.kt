package net.chronoflow.app.auth

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.chronoflow.app.AppDispatchers
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import net.chronoflow.feature.auth.model.SessionAcceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TwoFactorRoutingModelTest {
    private class Fixture(
        scope: TestScope,
    ) {
        val navigator = RootNavigator().apply { navigate(Route.SignIn) }
        private val dispatcher = StandardTestDispatcher(scope.testScheduler)
        val services =
            AuthServices(
                navigator,
                scope.backgroundScope,
                AppDispatchers(dispatcher, dispatcher, dispatcher),
                twoFactorTestSource(dispatcher),
                SessionAcceptor {},
                { "0199a3c0-0000-7000-8000-000000000001" },
                { "test" },
                { navigator.navigate(Route.Today) },
            )

        suspend fun enter() {
            services.signIn.editEmail("twofactor@example.com")
            services.signIn.submitPassword(TwoFactorTestSecret()) {}
        }
    }

    @Test fun twoFactorReplacesSignInWithoutAChallengeInTheRoute() =
        runTest(timeout = 60.seconds) {
            val f = Fixture(this)
            f.enter()
            assertEquals(Route.TwoFactor, f.navigator.state.value.route)
            assertFalse(
                f.navigator.state.value
                    .toString()
                    .contains(checkNotNull(f.services.signIn.navigation.challengeID)),
            )
        }

    @Test fun backDiscardsChallengeAndKeepsTheEmail() =
        runTest(timeout = 60.seconds) {
            val f = Fixture(this)
            f.enter()
            val old = checkNotNull(f.services.signIn.twoFactor.value)
            old.cancel()
            assertEquals(Route.SignIn, f.navigator.state.value.route)
            assertNull(f.services.signIn.navigation.challengeID)
            assertEquals("twofactor@example.com", f.services.signIn.email.value)
            old.editCode("123")
            assertEquals("", old.code)
        }

    @Test fun reenteringGetsAFreshModel() =
        runTest(timeout = 60.seconds) {
            val f = Fixture(this)
            f.enter()
            val old = checkNotNull(f.services.signIn.twoFactor.value)
            old.cancel()
            f.enter()
            assertNotSame(old, f.services.signIn.twoFactor.value)
            assertEquals("", checkNotNull(f.services.signIn.twoFactor.value).code)
        }
}
