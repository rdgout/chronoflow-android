package net.chronoflow.app

import android.content.Intent
import androidx.compose.runtime.Composable
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.debug.DebugLaunchOverrides
import net.chronoflow.debug.DebugRouteLabel
import net.chronoflow.feature.launch.LaunchController

internal object LaunchPlatform {
    val deleteSheetSeed: Boolean get() =
        net.chronoflow.debug.DebugSeedRouting.current
            ?.settingsRoute == "delete"
    val passwordPromptSeed: String? get() =
        if (net.chronoflow.debug.DebugSeedRouting.current
                ?.settingsRoute == "twoFactorSetup"
        ) {
            net.chronoflow.core.datamock.MockScenarioValues.password
        } else {
            null
        }

    private val wizardSeeds by lazy { java.util.WeakHashMap<AppGraph, net.chronoflow.debug.WizardDebugSeeds>() }

    fun prepareWizard(
        graph: AppGraph,
        epoch: Long,
        locale: java.util.Locale,
    ): Boolean {
        val prepared =
            wizardSeeds
                .getOrPut(graph) { net.chronoflow.debug.WizardDebugSeeds() }
                .prepare(net.chronoflow.debug.DebugSeedRouting.current, epoch, locale)
        graph.wizardStartOverride = prepared.override
        return prepared.open
    }

    val wizardBoardCapture: String? get() =
        net.chronoflow.debug.DebugSeedRouting.current
            ?.wizardBoardCapture
    val frozenTripInstant: java.time.Instant? get() =
        net.chronoflow.app.perf.perfClockInstant(
            net.chronoflow.debug.DebugSeedRouting.current
                ?.freezeAtRaw,
        )
    val initialTripSheet: net.chronoflow.core.navigation.TripSheet? get() =
        net.chronoflow.debug.DebugSeedRouting.current?.let {
            it.planSheet
                ?: if (it.wizardBoardCapture ==
                    "TripActions"
                ) {
                    net.chronoflow.core.navigation.TripSheet.TripActions
                } else {
                    null
                }
        }
    val startTripInList: Boolean get() =
        (
            net.chronoflow.debug.DebugSeedRouting.current
                ?.planRoute as? net.chronoflow.debug.PlanRouteSeed.Trip
        )?.list ==
            true
    val startTripAtSummary: Boolean get() =
        net.chronoflow.debug.DebugSeedRouting.current
            ?.planSummary == true
    val freezePlanClock: Boolean get() =
        net.chronoflow.debug.DebugSeedRouting.current
            ?.freezePlanClock == true
    val shellStart: () -> net.chronoflow.core.navigation.shell.ShellStart? = net.chronoflow.debug.DebugShellStart::take
    val refreshProbe: net.chronoflow.core.sync.refresh.RefreshDebugProbe = net.chronoflow.debug.DebugRefreshProbe

    val passkeyCapability: net.chronoflow.core.model.passkey.PasskeyCapability
        get() =
            net.chronoflow.core.model.passkey.PasskeyCapability(
                net.chronoflow.debug.DebugSeedRouting.current
                    ?.passkeyCapability
                    ?: net.chronoflow.core.model.passkey.PasskeyCapability.SERVER_READY,
            )

    fun mockSources(io: kotlinx.coroutines.CoroutineDispatcher): MockSources? = platformMockSources(io)

    fun verificationMail(graph: AppGraph): net.chronoflow.feature.auth.verify.VerificationMail? =
        if (graph.selection.source == net.chronoflow.core.network.DataSourceFactory.Source.Mock) {
            graph.mockSources?.let {
                net.chronoflow.debug.MockVerificationMail(it, { graph.dataSources.public() }, graph.dispatchers.io)
            }
        } else {
            null
        }

    val guestRecoveryAllowed: Boolean get() =
        wizardBoardCapture == null ||
            net.chronoflow.debug.DebugSeedRouting.current
                ?.guestRecoveryBoard != null

    val guestWizardSeedEnabled: Boolean get() =
        net.chronoflow.debug.DebugSeedRouting.current?.let {
            (it.guestWizard || it.guestWizardJourney) && it.wizardBoardCapture != null
        } == true
    val guestWizardBoard: String? get() =
        net.chronoflow.debug.DebugSeedRouting.current?.let {
            if (it.guestWizard || it.guestWizardJourney) it.wizardBoardCapture else it.guestRecoveryBoard
        }
    val guestWizardInstant: java.time.Instant? get() =
        guestWizardBoard?.takeIf { it != "WizardGuestJourney" }?.let {
            net.chronoflow.feature.wizard.debug.WizardBoardCaptureSeed.now
        }
    val guestWizardUiOverride: net.chronoflow.feature.wizard.shell.WizardUiOverride? get() =
        guestWizardBoard?.let {
            net.chronoflow.feature.wizard.debug.GuestWizardBoardSeed
                .uiOverride(it)
        }
    val guestWizardPresentation: net.chronoflow.feature.wizard.ui.WizardInitialPresentation get() =
        guestWizardBoard?.let {
            net.chronoflow.feature.wizard.debug.GuestWizardBoardSeed
                .presentation(it)
        } ?: net.chronoflow.feature.wizard.ui
            .WizardInitialPresentation()

    fun seedGuestWizard(
        draft: net.chronoflow.core.wizard.model.MemberWizardDraft,
        locale: java.util.Locale,
        now: java.time.Instant,
    ): net.chronoflow.core.wizard.model.MemberWizardDraft {
        val seed = net.chronoflow.debug.DebugSeedRouting.current
        val board = seed?.wizardBoardCapture
        return if (board != null && (seed.guestWizard || seed.guestWizardJourney)) {
            net.chronoflow.feature.wizard.debug.GuestWizardBoardSeed
                .draft(draft, board, locale, now)
        } else {
            draft
        }
    }

    val guestSignUpRequested: () -> Unit = net.chronoflow.debug.GuestWizardDebugCounters::requested

    fun welcomeDestinations(navigator: RootNavigator): WelcomeDestinations = RoutedWelcomeDestinations(navigator)

    val ioViolationSink: (Boolean, List<String>) -> Unit = net.chronoflow.debug.StrictModeViolationCounter::record
    val routeLabel: (@Composable (String) -> Unit)? = { DebugRouteLabel(it) }
    private var current: LaunchController? = null

    fun controller(graph: AppGraph): LaunchController = current ?: newLaunchController(graph).also { current = it }

    fun overrides(
        intent: Intent?,
        graph: AppGraph? = null,
    ) = DebugLaunchOverrides.fromIntent(
        intent,
        graph?.launchReadiness ?: net.chronoflow.core.model.launch.SignedOutReadiness,
    )

    fun observeRoutes(graph: AppGraph) =
        net.chronoflow.debug.RouteProbe
            .observe(graph)

    suspend fun routeScreen(
        graph: AppGraph,
        intent: Intent?,
    ) {
        val seed =
            net.chronoflow.debug.TestSeed
                .fromIntent(intent)
        if (seed.profile == net.chronoflow.debug.UiTestProfile.Screen) {
            net.chronoflow.debug.ScreenSessionFixture
                .route(graph, seed.raw["CF_AUTH_TEST_AUDIT_SCREEN"], seed.raw["CF_AUTH_TEST_AUDIT_STATE"])
            // A screen seed skips the overlay that normally completes the recovery hand-off.
            if (seed.guestRecoveryBoard != null) graph.launch.onExitFrame(0)
            if (seed.guestWizard &&
                graph.controller.presence == net.chronoflow.core.model.session.SessionPresence.SignedOut
            ) {
                graph.rootNavigator.navigate(net.chronoflow.core.navigation.Route.GuestWizard)
            }
        }
    }

    fun reset(graph: AppGraph) {
        check(BuildConfig.DEBUG)
        current?.onActivityDestroyed()
        current = newLaunchController(graph)
    }
}

internal fun AppGraph.resetLaunchForTest() {
    check(BuildConfig.DEBUG)
    LaunchPlatform.reset(this)
}

internal fun AppGraph.resetIoViolationsForTest() =
    net.chronoflow.debug.StrictModeViolationCounter
        .reset()

internal val AppGraph.ioViolationsForTest: Int get() = net.chronoflow.debug.StrictModeViolationCounter.count
