package net.chronoflow.app

import assertk.assertThat
import assertk.assertions.isFalse
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.launch.SignedOutReadiness
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.sync.session.AccountReadinessSource
import net.chronoflow.core.sync.session.MemorySessionStore
import net.chronoflow.core.sync.session.RecordingAccountStore
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionTeardown
import net.chronoflow.core.sync.session.StoredDataReadiness
import net.chronoflow.core.testing.sync.syncAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ReleaseReadinessTest {
    @Test fun shellSeedsAndRefreshProbeAreAbsent() {
        org.junit.Assert.assertNull(LaunchPlatform.shellStart())
        org.junit.Assert.assertSame(
            net.chronoflow.core.sync.refresh.RefreshDebugProbe.None,
            LaunchPlatform.refreshProbe,
        )
        missing("net.chronoflow.debug.DebugRefreshProbe")
        missing("net.chronoflow.debug.DebugShellStart")
    }

    @Test fun tripSheetSeedsAndRefusalCounterAreAbsent() {
        org.junit.Assert.assertNull(LaunchPlatform.initialTripSheet)
        org.junit.Assert.assertNull(LaunchPlatform.frozenTripInstant)
        missing("net.chronoflow.app.perf.FixtureTripPageSource")
        val feedback = Class.forName("net.chronoflow.core.ui.plan.RefusedChangeDebug")
        assertFalse(feedback.declaredFields.any { it.name == "presentedCount" })
    }

    @Test fun passkeyCapabilityIsShippedOff() {
        val message = "Flip only with S1–S3 evidence (docs/passkeys.md)"
        assertFalse(message, net.chronoflow.core.model.passkey.PasskeyCapability.SERVER_READY)
        assertFalse(message, net.chronoflow.core.model.passkey.PasskeyCapability.Shipped.enabled)
        assertFalse(message, LaunchPlatform.passkeyCapability.enabled)
    }

    private fun missing(name: String) {
        try {
            Class.forName(name)
            fail("$name is on the release classpath")
        } catch (_: ClassNotFoundException) {
        }
    }

    @Test fun sessionSeedCodeIsAbsent() {
        missing("net.chronoflow.debug.MockVerificationMail")
        missing("net.chronoflow.feature.auth.verify.VerifyCompositionProbe")
        missing("net.chronoflow.debug.MockSessionSeed")
        missing("net.chronoflow.debug.RouteProbe")
        missing("net.chronoflow.debug.DebugSeedRouting")
        missing("net.chronoflow.debug.ScreenSessionFixture")
        missing("net.chronoflow.debug.DebugAppHooks")
        missing("net.chronoflow.app.perf.PerfMockSources")
        assertFalse(net.chronoflow.core.ui.R.string::class.java.fields.any { it.name == "app_debug_route_x" })
    }

    @Test fun releaseRestoresNativeSession() =
        runTest(timeout = 60.seconds) {
            val io = StandardTestDispatcher(testScheduler)
            val credentials = MemorySessionStore(StoredSession("t", syncAccount).encode())
            val controller = SessionController(credentials, RecordingAccountStore(), SessionTeardown(sleep = {}), io)
            val readiness = StoredDataReadiness(controller, AccountReadinessSource { emptyFlow() }, io)
            try {
                assertEquals(SessionPresence.SignedIn(syncAccount), readiness.session())
                assertEquals(1, credentials.reads)
                assertEquals(SessionPresence.SignedOut, SignedOutReadiness.session())
                assertEquals(1, credentials.reads)
            } finally {
                readiness.close()
                controller.close()
            }
        }

    @Test
    fun mockDataSourceIsAbsent() {
        missing("net.chronoflow.core.datamock.MockDataSource")
        missing("net.chronoflow.core.datamock.MockAppSeed")
        missing("net.chronoflow.core.datamock.MockServerState")
        missing("net.chronoflow.core.datamock.MockScenarioValues")
    }

    @Test
    fun releaseShipsNoSeedCode() {
        missing("net.chronoflow.debug.SeedReceiverActivity")
        missing("net.chronoflow.debug.TestSeeds")
        missing("net.chronoflow.debug.TestSeed")
        missing("net.chronoflow.debug.UITestTimeline")
    }

    @Test
    fun releaseShipsNoPerfCode() {
        missing("net.chronoflow.app.perf.today.FixtureTodaySource")
        missing("net.chronoflow.app.perf.StubReadinessFactory")
        missing("net.chronoflow.app.perf.PerfEntry")
        missing("net.chronoflow.app.perf.FixtureTripPageSource")
        assertThat(BuildConfig.PERF_ENTRY).isFalse()
    }

    @Test
    fun releaseShipsNoJankStats() {
        missing("androidx.metrics.performance.JankStats")
        missing("net.chronoflow.app.perf.JankStatsInstaller")
    }

    @Test fun releaseShipsNoLaunchDiagnosticsOrStub() {
        missing("net.chronoflow.app.perf.StubReadiness")
        missing("net.chronoflow.debug.DebugRouteLabel")
        missing("net.chronoflow.debug.DebugWelcomeDestinations")
        missing("net.chronoflow.debug.StrictModeViolationCounter")
        missing("net.chronoflow.feature.launch.welcome.WelcomeLaunchDiagnostics")
        missing("net.chronoflow.feature.launch.welcome.WelcomeProbe")
        org.junit.Assert.assertEquals(
            LaunchDiagnostics.None,
            net.chronoflow.app.perf.LaunchDiagnosticsParser.parse(
                mapOf("cf.session" to "in", "cf.mode" to "ready", "cf.mainThreadStall" to "500"),
            ),
        )
    }

    @Test
    fun releaseIsNotDebuggable() {
        assertThat(BuildConfig.DEBUG).isFalse()
    }

    @Test
    fun releaseShipsNoActivityTestHooksOrRecordingFixtures() {
        val names =
            MainActivity::class.java.declaredFields.map { it.name } +
                MainActivity::class.java.declaredMethods.map { it.name }
        val removedHooks = listOf("fontPreloadOverride", "themeProbe")
        assertThat(names.any { name -> removedHooks.any { name.contains(it, ignoreCase = true) } }).isFalse()
        missing("net.chronoflow.app.RootThemeTest")
        missing("net.chronoflow.app.RootThemeTest\$RecordingApplication")
        missing("net.chronoflow.app.RootThemeTest\$RecordingFontPreloader")
    }
}
