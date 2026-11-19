package net.chronoflow.app

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.feature.trip.timeline.LocalTimelineJankTagger

@Composable
internal fun RootContentTestHost(content: @Composable () -> Unit) {
    ChronoFlowTheme {
        CompositionLocalProvider(
            LocalSafeInsets provides WindowInsets(0),
            LocalLayoutDirection provides LayoutDirection.Ltr,
            LocalTimelineJankTagger provides null,
            content = content,
        )
    }
}
