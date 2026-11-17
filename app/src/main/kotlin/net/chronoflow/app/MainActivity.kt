package net.chronoflow.app

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
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
import net.chronoflow.app.perf.PerfJank
import net.chronoflow.app.perf.TripPageEntry
import net.chronoflow.core.designsystem.theme.ReduceMotion
import net.chronoflow.feature.launch.LaunchController
import net.chronoflow.feature.launch.LaunchTrace

class MainActivity : ComponentActivity() {
    private var reduceMotion by mutableStateOf(false)
    private val firstCommit = CompletableDeferred<Unit>()
    private lateinit var launch: LaunchController

    override fun onResume() {
        super.onResume()
        reduceMotion = ReduceMotion.resolve(ValueAnimator.areAnimatorsEnabled(), RootOverrides.reduceMotion(intent))
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        (newBase.applicationContext as ChronoFlowApp).graph.launch.captureAppearance(newBase.resources.configuration)
        applyOverrideConfiguration(ConfigurationPolicy.overrideConfiguration(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        LaunchTrace.beginFirstCommit()
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val graph = (application as ChronoFlowApp).graph
        launch = graph.launch
        launch.onActivityCreated(restored = savedInstanceState != null)
        val overrides = LaunchPlatform.overrides(intent)
        reduceMotion = ReduceMotion.resolve(ValueAnimator.areAnimatorsEnabled(), overrides.reduceMotion)
        launch.captureReduceMotion(ValueAnimator.areAnimatorsEnabled(), overrides.reduceMotion)
        val claimed = launch.claimOverlay(overrides)
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
            ) { entry?.invoke() }
        }
        PerfJank.install(this)
        window.decorView.viewTreeObserver.registerFrameCommitCallback {
            firstCommit.complete(Unit)
            launch.onFrameCommitted(claimed)
            (application as ChronoFlowApp).launchPath.end()
        }
        splash.setKeepOnScreenCondition { launch.holdsSystemSplash() }
        splash.setOnExitAnimationListener { provider ->
            val icon = provider.iconView
            val location = IntArray(2)
            icon.getLocationOnScreen(location)
            launch.systemIconBoundsPx =
                Rect(
                    location[0],
                    location[1],
                    location[0] + icon.width,
                    location[1] + icon.height,
                )
            provider.remove()
        }
    }

    override fun onDestroy() {
        if (::launch.isInitialized) launch.onActivityDestroyed()
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
