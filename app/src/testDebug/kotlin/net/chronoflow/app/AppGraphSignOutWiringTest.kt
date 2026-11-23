package net.chronoflow.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.datamock.FileMockStateStore
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.datamock.MockLatency
import net.chronoflow.core.datamock.MockScenarioValues
import net.chronoflow.core.datamock.MockServerAccount
import net.chronoflow.core.datamock.MockServerState
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.AccountSettings
import net.chronoflow.core.model.api.AccountSnapshot
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.sync.snapshot
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.debug.ScreenSessionFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class AppGraphSignOutWiringTest(
    private val unauthorized: Boolean,
) {
    @Test fun signOutNavigatesErasesAccountFilesAndRejectsTheOldToken() =
        runTest(timeout = 60.seconds) {
            val app = ApplicationProvider.getApplicationContext<Application>()
            val dispatcher = StandardTestDispatcher(testScheduler)
            val root = withContext(Dispatchers.IO) { Files.createTempDirectory("graph-sign-out").toFile() }
            val graph =
                AppGraph(
                    app,
                    overrides =
                        GraphOverrides(
                            storageDir = root,
                            mockDelay = Duration.ZERO,
                            mockSeed = MockSeedFlags(unverified = false, walkthroughSeen = true, termsPending = false),
                            sessionCipher = SoftwareSessionCipher(),
                            dispatchers = AppDispatchers(dispatcher, dispatcher, dispatcher),
                        ),
                )
            try {
                // Account teardown needs a session and account DB, not the UI seed's full trip/plan synchronization.
                graph.sessionSync.setEnabled(false)
                val store = FileMockStateStore(root)
                withContext(Dispatchers.IO) {
                    store.save(
                        MockServerState(
                            listOf(
                                MockServerAccount(
                                    snapshot(MockScenarioValues.demoID).let { value ->
                                        AccountSnapshot(
                                            value.id,
                                            value.name,
                                            MockScenarioValues.demoEmail,
                                            value.hasVerifiedEmail,
                                            AccountSettings(
                                                value.settings.bodyClock,
                                                value.settings.sleepAnswered,
                                                value.settings.preferences,
                                                value.settings.reminders,
                                                value.settings.planCredits,
                                                value.settings.policies.copy(
                                                    acceptedTermsVersion = value.settings.policies.termsVersion,
                                                ),
                                            ),
                                            value.readiness,
                                            value.billing,
                                            value.security,
                                        )
                                    },
                                    MockScenarioValues.password,
                                    walkthroughSeen = true,
                                ),
                            ),
                        ),
                    )
                }

                fun source(bearer: suspend () -> String?) =
                    MockDataSource(
                        store = FileMockStateStore(root),
                        bearer = bearer,
                        debugAppSeed = false,
                        delay = { 0.0 },
                        sleep = MockLatency.exact(0.0),
                        io = dispatcher,
                    )
                val result =
                    source { null }.signIn(
                        PasswordSignInInput(
                            MockScenarioValues.demoEmail,
                            MockScenarioValues.password,
                            "0199a3c0-0000-7000-8000-000000000001",
                            "test",
                        ),
                    ) as SignInResult.Session
                graph.controller.accept(result.result)
                ScreenSessionFixture.route(graph)
                runCurrent()
                val old = checkNotNull(graph.controller.activeSession())
                assertEquals(Route.Today, graph.rootNavigator.state.value.route)
                // The router also observes sign-out; isolate the teardown's own detach/navigation callback.
                graph.gateRouter.stop()
                runCurrent()
                val account = File(root, "ChronoFlowMock/Accounts/${old.session.accountId}")
                withContext(Dispatchers.IO) { assertTrue(account.isDirectory) }

                val guestFile = File(root, "ChronoFlowMock/guest-wizard.json")
                graph.guestSecrets.set("guest-before-sign-out", "guest-draft")
                withContext(Dispatchers.IO) { guestFile.writeText("{}") }

                fun freshSource() = source { old.session.token }
                assertEquals(old.session.accountId, freshSource().fetchIdentity().id)
                if (unauthorized) {
                    // A 401 is already revoked server-side; this path deliberately skips a redundant logout.
                    freshSource().signOut()
                    graph.controller.unauthorized(old.epoch)
                } else {
                    graph.controller.signOut(old, discardPending = true)
                }
                runCurrent()
                assertEquals(SessionPresence.SignedOut, graph.controller.presence)
                assertEquals(Route.SignIn, graph.rootNavigator.state.value.route)
                withContext(Dispatchers.IO) {
                    assertFalse(account.exists())
                    assertFalse(guestFile.exists())
                }
                assertEquals(null, graph.guestSecrets.get("guest-draft"))
                val status =
                    try {
                        freshSource().fetchIdentity()
                        null
                    } catch (failure: DataFailure.Unauthorized) {
                        failure.status
                    }
                assertEquals(401, status)
            } finally {
                withContext(NonCancellable) {
                    val jobs = graph.ownedTestJobs()
                    graph.closeAndJoinForTest()
                    assertTrue("all owned graph scopes must finish before the next test", jobs.all { it.isCompleted })
                    withContext(Dispatchers.IO) { check(root.deleteRecursively()) }
                }
            }
        }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "unauthorized={0}")
        fun cases(): List<Array<Any>> = listOf(arrayOf(true), arrayOf(false))
    }
}
