package net.chronoflow.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.LayoutDirection
import net.chronoflow.app.perf.TripPageEntry
import net.chronoflow.core.ui.LocalSafeInsets

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        applyOverrideConfiguration(ConfigurationPolicy.overrideConfiguration(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val entry = TripPageEntry.content(intent, (application as ChronoFlowApp).graph)
        setContent {
            CompositionLocalProvider(
                LocalSafeInsets provides WindowInsets.safeDrawing,
                LocalLayoutDirection provides LayoutDirection.Ltr,
            ) {
                Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }.testTag("app.root")) {
                    entry?.invoke()
                }
            }
        }
        window.decorView.viewTreeObserver.registerFrameCommitCallback {
            (application as ChronoFlowApp).launchPath.end()
        }
    }
}
