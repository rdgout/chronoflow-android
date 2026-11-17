package net.chronoflow.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.designsystem.theme.warmFontsAfterFirstFrame
import net.chronoflow.feature.launch.LaunchOverlay
import net.chronoflow.feature.trip.timeline.LocalTimelineJankTagger
import net.chronoflow.feature.trip.timeline.TimelineJankTagger

@Composable
internal fun CfRoot(
    graph: AppGraph,
    claimed: Boolean,
    reduceMotion: Boolean,
    firstCommit: Deferred<Unit>,
    jankTagger: TimelineJankTagger?,
    content: @Composable () -> Unit,
) {
    val resolver = LocalFontFamilyResolver.current
    LaunchedEffect(resolver) {
        withFrameNanos { }
        warmFontsAfterFirstFrame(firstCommit, { graph.fontPreloader.preload(resolver, it) }, Dispatchers.IO)
    }
    ChronoFlowTheme(reduceMotion = reduceMotion) {
        CompositionLocalProvider(
            LocalSafeInsets provides WindowInsets.safeDrawing,
            LocalLayoutDirection provides LayoutDirection.Ltr,
            LocalTimelineJankTagger provides jankTagger,
        ) {
            var contentReady by remember { mutableStateOf(!claimed) }
            LaunchedEffect(Unit) {
                withFrameNanos { }
                contentReady = true
            }
            Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }.testTag("app.root")) {
                if (contentReady) {
                    Box(
                        if (claimed) Modifier.semantics { hideFromAccessibility() } else Modifier,
                    ) { content() }
                }
                if (claimed) LaunchOverlay(graph.launch)
            }
        }
    }
}
