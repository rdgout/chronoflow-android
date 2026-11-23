package net.chronoflow.app

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import net.chronoflow.app.perf.PerfJank
import net.chronoflow.app.perf.TripPageEntry
import net.chronoflow.core.designsystem.theme.ReduceMotion
import net.chronoflow.core.model.config.ThemeChoice
import net.chronoflow.feature.launch.LaunchController
import net.chronoflow.feature.launch.model.LaunchKind

class MainActivity : ComponentActivity() {
    private var reduceMotion by mutableStateOf(false)
    private val firstCommit = CompletableDeferred<Unit>()
    private lateinit var launch: LaunchController
    private var initialTheme = ThemeChoice.System

    override val defaultViewModelCreationExtras: androidx.lifecycle.viewmodel.CreationExtras
        get() =
            androidx.lifecycle.viewmodel.MutableCreationExtras(super.defaultViewModelCreationExtras).apply {
                set(AppGraphKey, (application as ChronoFlowApp).graph)
            }

    override fun onResume() {
        super.onResume()
        reduceMotion = ReduceMotion.resolve(ValueAnimator.areAnimatorsEnabled(), RootOverrides.reduceMotion(intent))
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        (newBase.applicationContext as ChronoFlowApp).graph.launch.captureAppearance(newBase.resources.configuration)
        val configuration = ConfigurationPolicy.forLaunch(newBase)
        initialTheme = configuration.initialTheme
        applyOverrideConfiguration(configuration.configuration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val graph = (application as ChronoFlowApp).graph
        launch = graph.launch
        val freshLaunch =
            savedInstanceState == null && (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0
        graph.pendingLaunchIntents.capture(intent, restored = savedInstanceState != null)
        setIntent(
            net.chronoflow.app.reminders
                .sanitizedLaunchIntent(intent),
        )
        graph.authLinks.deliver()
        val overrides = LaunchPlatform.overrides(intent, graph)
        val kind = launch.onActivityCreated(savedInstanceState, overrides.skipOverlay, overrides)
        val splash = installSplashScreen()
        super.onCreate(if (kind == LaunchKind.ProcessDeathRestore) null else savedInstanceState)
        graph.passkeyAnchor.attach(this)
        reduceMotion = ReduceMotion.resolve(ValueAnimator.areAnimatorsEnabled(), overrides.reduceMotion)
        launch.captureReduceMotion(ValueAnimator.areAnimatorsEnabled(), overrides.reduceMotion)
        val claimed = launch.overlayClaimedByCurrentActivity
        applySystemBars(isNight(resources.configuration), launch.overlayShowing, false)
        window.attributes =
            window.attributes.apply {
                layoutInDisplayCutoutMode =
                    if (Build.VERSION.SDK_INT >=
                        30
                    ) {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    } else {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
            }
        val entry = TripPageEntry.content(intent, graph)
        val jankTagger = PerfJank.tagger(this)
        setContent {
            CfRoot(
                graph,
                claimed,
                reduceMotion,
                firstCommit,
                jankTagger,
                onSystemBars = ::applySystemBars,
                initialTheme = initialTheme,
                content = entry,
            )
        }
        PerfJank.install(this)
        window.decorView.viewTreeObserver.registerFrameCommitCallback {
            firstCommit.complete(Unit)
            launch.onFrameCommitted(claimed)
            (application as ChronoFlowApp).launchPath.end()
            if (freshLaunch) graph.appScope.launch { LaunchPlatform.routeScreen(graph, intent) }
        }
        splash.setKeepOnScreenCondition { launch.holdsSystemSplash() }
        splash.setOnExitAnimationListener { provider -> launch.onSystemSplashExit(provider, kind) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val graph = (application as ChronoFlowApp).graph
        graph.pendingLaunchIntents.capture(intent, restored = false)
        graph.authLinks.deliver()
        graph.appScope.launch { graph.handlePendingLaunchIntents() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(LaunchController.KEY_PROCESS_TOKEN, launch.processToken)
    }

    override fun onDestroy() {
        (application as ChronoFlowApp).graph.passkeyAnchor.detach(this)
        if (::launch.isInitialized) launch.onActivityDestroyed(isFinishing, isChangingConfigurations)
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val launch = (application as ChronoFlowApp).graph.launch
        applySystemBars(isNight(newConfig), launch.overlayShowing, false)
    }

    private fun isNight(configuration: Configuration): Boolean =
        configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    private fun applySystemBars(
        night: Boolean,
        overlayShowing: Boolean,
        onSky: Boolean,
    ) {
        fun style(appearance: IconAppearance): SystemBarStyle =
            when (appearance) {
                IconAppearance.LightIcons -> SystemBarStyle.dark(Color.Transparent.toArgb())
                IconAppearance.DarkIcons -> SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb())
            }
        enableEdgeToEdge(
            statusBarStyle = style(SystemBarStyles.statusBar(night, overlayShowing, onSky)),
            navigationBarStyle = style(SystemBarStyles.navigationBar(night)),
        )
        window.isNavigationBarContrastEnforced = false
    }
}
