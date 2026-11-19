package net.chronoflow.app

import android.content.Intent
import androidx.compose.runtime.Composable
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.debug.DebugLaunchOverrides
import net.chronoflow.debug.DebugRouteLabel
import net.chronoflow.feature.launch.LaunchController

internal object LaunchPlatform {
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

    fun welcomeDestinations(navigator: RootNavigator): WelcomeDestinations =
        net.chronoflow.debug.DebugWelcomeDestinations(navigator)

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
        }
    }

    fun reset(graph: AppGraph) {
        check(BuildConfig.DEBUG)
        current?.onActivityDestroyed()
        current = newLaunchController(graph)
        graph.welcomeDestinations.closeSeam()
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
