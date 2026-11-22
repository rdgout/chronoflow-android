package net.chronoflow.app

import assertk.assertThat
import assertk.assertions.isFalse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataSource
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
import net.chronoflow.core.wizard.generation.TripGenerationService
import net.chronoflow.core.wizard.model.AirportSearchService
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.TripWizardAction
import net.chronoflow.core.wizard.model.WizardDraftStore
import net.chronoflow.feature.wizard.TripWizardDependencies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ReleaseReadinessTest {
    @Test fun guestSignupCoordinatorHasNoSeedOrPrefill() =
        runTest(timeout = 60.seconds) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val navigator =
                net.chronoflow.core.navigation
                    .RootNavigator()
            val auth =
                net.chronoflow.app.auth.AuthServices(
                    navigator,
                    backgroundScope,
                    AppDispatchers(dispatcher, dispatcher, dispatcher),
                    net.chronoflow.core.testing.sync
                        .ScriptedSource(),
                    net.chronoflow.feature.auth.model
                        .SessionAcceptor {},
                    { "device" },
                    { "phone" },
                    { _, _ -> },
                )
            val coordinator =
                net.chronoflow.app.guest.GuestSignupCoordinator(
                    auth,
                    navigator,
                    0,
                    LaunchPlatform.guestSignupSeed,
                    LaunchPlatform.guestSignupPrefill,
                )
            org.junit.Assert.assertNull(coordinator.seed)
            org.junit.Assert.assertNull(coordinator.prefill)
            coordinator.clear()
        }

    @Test fun wizardCaptureCodeAndStartOverrideAreAbsent() {
        missing("net.chronoflow.feature.wizard.debug.WizardBoardCaptureSeed")
        missing("net.chronoflow.feature.wizard.debug.GuestWizardBoardSeed")
        missing("net.chronoflow.debug.reminders.MockReminderCenter")
        missing("net.chronoflow.debug.guest.GuestSignupSeed")
        missing("net.chronoflow.debug.guest.GuestSignupPrefill")
        org.junit.Assert.assertNull(LaunchPlatform.guestSignupSeed)
        org.junit.Assert.assertNull(LaunchPlatform.guestSignupPrefill)
        missing("net.chronoflow.debug.GuestWizardDebugSecrets")
        org.junit.Assert.assertNull(LaunchPlatform.guestWizardBoard)
        org.junit.Assert.assertNull(LaunchPlatform.guestWizardInstant)
        org.junit.Assert.assertNull(LaunchPlatform.guestWizardUiOverride)
        missing("net.chronoflow.feature.wizard.debug.FixedAirportSearch")
        missing("net.chronoflow.feature.wizard.debug.WizardCompletionBoardSeed")
        missing("net.chronoflow.feature.wizard.debug.TripWizardActionsBoardSeed")
        missing("net.chronoflow.feature.wizard.debug.WizardSaveJourney")
        missing("net.chronoflow.feature.wizard.debug.WizardCompletionPrompts")
        missing("net.chronoflow.feature.wizard.debug.FrozenCheckingClock")
        val deps =
            object : TripWizardDependencies {
                override val store: WizardDraftStore get() = error("unused")
                override val airportSearch: AirportSearchService get() = error("unused")
                override val source: DataSource get() = error("unused")
                override val now = { java.time.Instant.EPOCH }
                override val writerScope: CoroutineScope get() = error("unused")

                override suspend fun start(): MemberWizardDraft = error("unused")

                override suspend fun startTrip(
                    id: String,
                    action: TripWizardAction,
                ): MemberWizardDraft = error("unused")

                override fun newGeneration(): TripGenerationService = error("unused")
            }
        org.junit.Assert.assertNull(deps.uiOverride)
        missing("net.chronoflow.feature.wizard.debug.WizardStepSeam")
        missing("net.chronoflow.feature.wizard.debug.WizardMemoryDraftStore")
        missing("net.chronoflow.debug.WizardDebugSeeds")
        org.junit.Assert.assertNull(
            net.chronoflow.app.wizard
                .WizardStartOverrides()
                .wizardStartOverride,
        )
        org.junit.Assert.assertNull(LaunchPlatform.wizardBoardCapture)
    }

    @Test fun shellSeedsAndRefreshProbeAreAbsent() {
        missing("net.chronoflow.app.shell.ShellRouteProbe")
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
        val actions = Class.forName("net.chronoflow.feature.trip.actions.TripActionsDebugCounters")
        assertFalse(actions.declaredFields.any { it.name == "failurePresented" })
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
