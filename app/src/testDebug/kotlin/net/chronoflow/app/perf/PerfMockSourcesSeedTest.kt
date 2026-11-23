package net.chronoflow.app.perf

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.app.GraphOverrides
import net.chronoflow.core.datamock.FileMockStateStore
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockServerState
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.testing.junit.measureClassWarmup
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class PerfMockSourcesSeedTest {
    @get:Rule val temporary = TemporaryFolder()
    private val credentials get() =
        PasswordSignInInput(
            "demo@example.com",
            "password",
            "019a562b-8000-7000-8000-000000000013",
            "perf test",
        )

    @After fun resetExtras() {
        PerfJourneyState.current = PerfJourneyExtras.None
    }

    @Test fun freshDemoAccountCanSignIn() =
        runTest(timeout = 60.seconds) {
            val io = StandardTestDispatcher(testScheduler)
            PerfJourneyState.current = PerfJourneyExtras(true, null)
            val source =
                PerfMockSources(
                    io,
                    debugAppSeed = false,
                ).dataSource(temporary.root, GraphOverrides(mockDelay = Duration.ZERO)) {
                    null
                }
            assertTrue(source.signIn(credentials) is SignInResult.Session)
        }

    @Test fun freshPerfMockHasNoAccountWithoutTheFlag() =
        runTest(timeout = 60.seconds) {
            val io = StandardTestDispatcher(testScheduler)
            PerfJourneyState.current = PerfJourneyExtras.None
            val source =
                PerfMockSources(
                    io,
                    debugAppSeed = false,
                ).dataSource(temporary.root, GraphOverrides(mockDelay = Duration.ZERO)) {
                    null
                }
            var refused = false
            try {
                source.signIn(credentials)
            } catch (_: DataFailure.Validation) {
                refused = true
            }
            assertTrue(refused)
        }

    @Test fun existingStateIsNeverReseeded() =
        runTest(timeout = 60.seconds) {
            val io = StandardTestDispatcher(testScheduler)
            withContext(io) { FileMockStateStore(temporary.root).save(MockServerState()) }
            PerfJourneyState.current = PerfJourneyExtras(true, null)
            val source =
                PerfMockSources(
                    io,
                    debugAppSeed = false,
                ).dataSource(temporary.root, GraphOverrides(mockDelay = Duration.ZERO)) {
                    null
                }
            var refused = false
            try {
                source.signIn(credentials)
            } catch (_: DataFailure.Validation) {
                refused = true
            }
            assertTrue(refused)
        }

    companion object {
        @BeforeClass @JvmStatic
        fun warmSeedOnce() {
            measureClassWarmup(PerfMockSourcesSeedTest::class.java, 0, "mock-app-seed") {
                MockAppSeed.make(Instant.parse("2027-02-15T12:00:00Z"), false, true, false)
            }
        }
    }
}
