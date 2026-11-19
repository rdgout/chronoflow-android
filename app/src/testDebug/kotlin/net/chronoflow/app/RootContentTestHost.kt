package net.chronoflow.app

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.ui.window.LocalWindowSize
import net.chronoflow.feature.trip.timeline.LocalTimelineJankTagger

@Composable
internal fun RootContentTestHost(content: @Composable () -> Unit) {
    val size = LocalWindowInfo.current.containerSize
    val window = with(LocalDensity.current) { DpSize(size.width.toDp(), size.height.toDp()) }
    ChronoFlowTheme {
        CompositionLocalProvider(
            LocalSafeInsets provides WindowInsets(0),
            LocalWindowSize provides window,
            LocalLayoutDirection provides LayoutDirection.Ltr,
            LocalTimelineJankTagger provides null,
            content = content,
        )
    }
}
