package net.chronoflow.app

import android.content.Intent
import androidx.compose.runtime.Composable
import net.chronoflow.app.perf.LaunchDiagnosticsParser
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.feature.launch.LaunchOverrides

internal object LaunchPlatform {
    val startTripAtSummary = false
    val freezePlanClock = false
    val shellStart: () -> net.chronoflow.core.navigation.shell.ShellStart? = { null }
    val refreshProbe: net.chronoflow.core.sync.refresh.RefreshDebugProbe =
        net.chronoflow.core.sync.refresh.RefreshDebugProbe.None

    val passkeyCapability: net.chronoflow.core.model.passkey.PasskeyCapability
        get() = net.chronoflow.core.model.passkey.PasskeyCapability.Shipped

    fun mockSources(io: kotlinx.coroutines.CoroutineDispatcher): MockSources? = platformMockSources(io)

    @Suppress("UNUSED_PARAMETER")
    fun verificationMail(graph: AppGraph): net.chronoflow.feature.auth.verify.VerificationMail? = null

    fun welcomeDestinations(navigator: RootNavigator): WelcomeDestinations = RoutedWelcomeDestinations(navigator)

    val ioViolationSink: (Boolean, List<String>) -> Unit = { _, _ -> }
    val routeLabel: (@Composable (String) -> Unit)? = null
    private var current: net.chronoflow.feature.launch.LaunchController? = null

    fun controller(graph: AppGraph): net.chronoflow.feature.launch.LaunchController =
        current ?: newLaunchController(graph).also { current = it }

    fun overrides(
        intent: Intent?,
        graph: AppGraph? = null,
    ): LaunchOverrides =
        if (BuildConfig.PERF_ENTRY &&
            intent?.hasExtra("cf.trip-page") == true
        ) {
            LaunchDiagnosticsParser
                .overrides(
                    launchExtras(intent),
                    graph?.launchReadiness ?: net.chronoflow.core.model.launch.SignedOutReadiness,
                ).copy(skipOverlay = true)
        } else {
            LaunchDiagnosticsParser.overrides(
                launchExtras(intent),
                graph?.launchReadiness ?: net.chronoflow.core.model.launch.SignedOutReadiness,
            )
        }

    @Suppress("UNUSED_PARAMETER")
    fun observeRoutes(graph: AppGraph) = Unit

    @Suppress("UNUSED_PARAMETER")
    suspend fun routeScreen(
        graph: AppGraph,
        intent: Intent?,
    ) = Unit
}
