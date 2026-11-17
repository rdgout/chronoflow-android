package net.chronoflow.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.CompletableDeferred
import net.chronoflow.app.perf.PerfJank
import net.chronoflow.app.perf.TripPageEntry
import net.chronoflow.core.designsystem.theme.ReduceMotion
import net.chronoflow.feature.launch.LaunchTrace

class MainActivity : ComponentActivity() {
    private var reduceMotion by mutableStateOf(false)
    private val firstCommit = CompletableDeferred<Unit>()

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
        val launch = graph.launch
        launch.onActivityCreated(restored = savedInstanceState != null)
        val overrides = LaunchPlatform.overrides(intent)
        reduceMotion = ReduceMotion.resolve(ValueAnimator.areAnimatorsEnabled(), overrides.reduceMotion)
        launch.captureReduceMotion(ValueAnimator.areAnimatorsEnabled(), overrides.reduceMotion)
        val claimed = launch.claimOverlay(overrides)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle =
                if (launch.appearance.night) {
                    SystemBarStyle.dark(
                        Color.TRANSPARENT,
                    )
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                },
        )
        window.isNavigationBarContrastEnforced = false
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
        setContent { CfRoot(graph, claimed, reduceMotion, firstCommit, jankTagger) { entry?.invoke() } }
        PerfJank.install(this)
        window.decorView.viewTreeObserver.registerFrameCommitCallback {
            firstCommit.complete(Unit)
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
}
