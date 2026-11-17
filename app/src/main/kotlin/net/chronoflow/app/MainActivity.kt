package net.chronoflow.app

import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import net.chronoflow.app.perf.PerfJank
import net.chronoflow.app.perf.TripPageEntry
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.designsystem.theme.ReduceMotion
import net.chronoflow.core.designsystem.theme.warmFontsAfterFirstFrame
import net.chronoflow.feature.trip.timeline.LocalTimelineJankTagger

class MainActivity : ComponentActivity() {
    private var reduceMotion by mutableStateOf(false)
    private val firstCommit = CompletableDeferred<Unit>()

    override fun onResume() {
        super.onResume()
        reduceMotion = ReduceMotion.resolve(ValueAnimator.areAnimatorsEnabled(), RootOverrides.reduceMotion(intent))
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        applyOverrideConfiguration(ConfigurationPolicy.overrideConfiguration(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reduceMotion = ReduceMotion.resolve(ValueAnimator.areAnimatorsEnabled(), RootOverrides.reduceMotion(intent))
        val entry = TripPageEntry.content(intent, (application as ChronoFlowApp).graph)
        val jankTagger = PerfJank.tagger(this)
        setContent {
            val resolver = LocalFontFamilyResolver.current
            LaunchedEffect(resolver) {
                withFrameNanos { }
                warmFontsAfterFirstFrame(firstCommit, resolver::preload, Dispatchers.IO)
            }
            ChronoFlowTheme(reduceMotion = reduceMotion) {
                CompositionLocalProvider(
                    LocalSafeInsets provides WindowInsets.safeDrawing,
                    LocalLayoutDirection provides LayoutDirection.Ltr,
                    LocalTimelineJankTagger provides jankTagger,
                ) {
                    Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }.testTag("app.root")) {
                        entry?.invoke()
                    }
                }
            }
        }
        PerfJank.install(this)
        window.decorView.viewTreeObserver.registerFrameCommitCallback {
            firstCommit.complete(Unit)
            (application as ChronoFlowApp).launchPath.end()
        }
    }
}
