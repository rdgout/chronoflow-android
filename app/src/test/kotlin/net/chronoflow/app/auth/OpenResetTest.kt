package net.chronoflow.app.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.app.AppDispatchers
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.ResetPasswordInput
import net.chronoflow.core.model.auth.ResetPasswordLink
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.sync.ScriptedSource
import net.chronoflow.feature.auth.model.SecretField
import net.chronoflow.feature.auth.model.SessionAcceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class OpenResetTest {
    @Test fun openResetReplacesModelsAndLeavingCancelsTheCurrentModel() =
        runTest(timeout = 60.seconds) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val navigator = RootNavigator().apply { navigate(Route.SignIn) }
            val services =
                AuthServices(
                    navigator,
                    backgroundScope,
                    AppDispatchers(
                        dispatcher,
                        dispatcher,
                        dispatcher,
                    ),
                    ScriptedSource(),
                    SessionAcceptor {
                    },
                    { "id" },
                    { "test" },
                    {},
                )
            services.router.forgot()
            val forgot = checkNotNull(services.forgotModel)
            val link = ResetPasswordLink("first-secret", "demo@example.com")
            services.openReset(link)
            val first = checkNotNull(services.resetModel)
            assertFalse(forgot.isActive)
            assertNull(services.forgotModel)
            assertEquals(link, first.link)
            assertEquals(Route.Reset(link.email), navigator.state.value.route)
            assertFalse(
                navigator.state.value.route
                    .toString()
                    .contains(link.token),
            )
            val latest = ResetPasswordLink("second-secret", "other@example.com")
            navigator.navigate(Route.SignIn)
            services.openReset(latest)
            val current = checkNotNull(services.resetModel)
            runCurrent()
            assertFalse(first.isActive)
            assertSame(current, services.resetModel)
            assertEquals(latest, current.link)
            navigator.navigate(Route.SignIn)
            runCurrent()
            assertFalse(current.isActive)
            assertFalse(current.recovery.isActive)
            assertNull(services.resetModel)
        }

    @Test fun resetSuccessNeverAcceptsOrRoutesASession() =
        runTest(timeout = 60.seconds) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val navigator = RootNavigator().apply { navigate(Route.SignIn) }
            var accepts = 0
            var acceptedRoutes = 0
            val source =
                object : DataSource by ScriptedSource() {
                    override suspend fun resetPassword(input: ResetPasswordInput) {}
                }
            val services =
                AuthServices(
                    navigator,
                    backgroundScope,
                    AppDispatchers(
                        dispatcher,
                        dispatcher,
                        dispatcher,
                    ),
                    source,
                    SessionAcceptor {
                        accepts++
                    },
                    { "id" },
                    { "test" },
                    { acceptedRoutes++ },
                )
            services.openReset(ResetPasswordLink("secret", "demo@example.com"))
            val model = checkNotNull(services.resetModel)
            model.submit(ResetTestSecret(), ResetTestSecret())
            runCurrent()
            assertEquals(0, accepts)
            assertEquals(0, acceptedRoutes)
            assertEquals(Route.Reset("demo@example.com"), navigator.state.value.route)
            model.backToSignIn()
            assertEquals(Route.SignIn, navigator.state.value.route)
        }
}

private class ResetTestSecret : SecretField {
    override fun read(): String = "NewPassword123!"

    override fun clear() {}

    override fun toString(): String = "ResetTestSecret(<redacted>)"
}
