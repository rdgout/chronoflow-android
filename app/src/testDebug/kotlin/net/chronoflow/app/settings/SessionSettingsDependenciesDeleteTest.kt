@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.settings

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.app.AppGraph
import net.chronoflow.app.AppSessionFixture
import net.chronoflow.app.closeAndJoinForTest
import net.chronoflow.core.datamock.MockScenarioValues
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.debug.ScreenSessionFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class SessionSettingsDependenciesDeleteTest {
    @Test fun successSignsOutDiscardingPendingAndOpensWelcome() =
        runTest(timeout = 60.seconds) {
            withSession { graph, deps, root ->
                val session = checkNotNull(graph.sessionGraphs.current.value)
                session.outbox.enqueue(
                    OutboxChange.Settings(
                        SettingsSection.BodyClock,
                        JsonObject(mapOf("age_band" to JsonPrimitive("65_plus"))),
                    ),
                )
                assertEquals(1, session.outbox.pending().size)
                val directory = File(root, "ChronoFlowMock/Accounts/${deps.snapshot.session.accountId}")
                withContext(Dispatchers.IO) { assertTrue(directory.isDirectory) }
                deps.deleteAccount("password")
                runCurrent()
                assertEquals(SessionPresence.SignedOut, graph.controller.presence)
                assertEquals(Route.Welcome, graph.rootNavigator.state.value.route)
                withContext(Dispatchers.IO) {
                    assertFalse(directory.exists())
                    assertFalse(File(root, "ChronoFlowMock/session.bin").exists())
                }
            }
        }

    @Test fun wrongPasswordKeepsTheSessionAndSendsNoSignOut() =
        runTest(timeout = 60.seconds) {
            withSession { graph, deps, _ ->
                try {
                    deps.deleteAccount("wrong")
                    fail("Expected password refusal")
                } catch (
                    failure: DataFailure.Validation,
                ) {
                    assertNotNull(failure.fields["password"])
                }
                runCurrent()
                assertEquals(deps.snapshot, graph.controller.activeSession())
                assertEquals(Route.Today, graph.rootNavigator.state.value.route)
            }
        }

    @Test fun staleSessionThrowsStaleSessionExceptionAndSendsNothing() =
        runTest(timeout = 60.seconds) {
            withSession { graph, deps, _ ->
                graph.controller.signOut(deps.snapshot, discardPending = true)
                runCurrent()
                try {
                    deps.deleteAccount("password")
                    fail("Expected stale session")
                } catch (_: StaleSessionException) {
                }
                assertEquals(SessionPresence.SignedOut, graph.controller.presence)
                assertEquals(Route.SignIn, graph.rootNavigator.state.value.route)
            }
        }

    private suspend fun TestScope.withSession(
        block: suspend TestScope.(AppGraph, SessionSettingsDependencies, File) -> Unit,
    ) {
        val root = withContext(Dispatchers.IO) { Files.createTempDirectory("settings-delete").toFile() }
        val graph =
            AppSessionFixture.graph(
                ApplicationProvider.getApplicationContext(),
                root,
                StandardTestDispatcher(testScheduler),
            )
        try {
            graph.sessionSync.setEnabled(false)
            val result =
                graph.dataSources.public().signIn(
                    PasswordSignInInput(
                        MockScenarioValues.demoEmail,
                        MockScenarioValues.password,
                        "0199a3c0-0000-7000-8000-000000000001",
                        "test",
                    ),
                ) as SignInResult.Session
            graph.controller.accept(result.result)
            ScreenSessionFixture.route(graph)
            graph.gateRouter.start()
            runCurrent()
            val session = checkNotNull(graph.sessionGraphs.current.value)
            graph.sessionSync.setEnabled(true)
            block(graph, SessionSettingsDependencies(graph, session), root)
        } finally {
            withContext(NonCancellable) {
                graph.closeAndJoinForTest()
                withContext(Dispatchers.IO) { check(root.deleteRecursively()) }
            }
        }
    }
}
