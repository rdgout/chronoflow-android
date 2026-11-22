package net.chronoflow.app.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.app.AppDispatchers
import net.chronoflow.app.MockSources
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.network.DataSourceFactory
import net.chronoflow.core.testing.sync.ScriptedSource
import net.chronoflow.feature.auth.model.SessionAcceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotRoutingTest {
    @Test fun forgotCreatesAFreshModelAndLeavingCancelsIt() =
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
                    { _, _ -> },
                )
            services.router.forgot()
            val first = checkNotNull(services.forgotModel)
            assertEquals(Route.Forgot, navigator.state.value.route)
            services.router.forgot()
            val second = checkNotNull(services.forgotModel)
            assertNotSame(first, second)
            assertFalse(first.isActive)
            runCurrent()
            assertEquals(second, services.forgotModel)
            navigator.navigate(Route.SignIn)
            runCurrent()
            assertFalse(second.isActive)
            assertNull(services.forgotModel)
        }

    @Test fun apiNeverGetsMockMailEvenWithASuppliedMockSeam() {
        val source = ScriptedSource()
        assertNull(
            mockResetMail(
                DataSourceFactory.Source.Api,
                MockSources {
                    _,
                    _,
                    _,
                    ->
                    source
                },
                { source },
            ) { error("opened mock link") },
        )
    }
}
