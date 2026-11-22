package net.chronoflow.feature.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.system.CfSheetDragHandle
import net.chronoflow.core.designsystem.system.CfSheetMetrics
import net.chronoflow.core.designsystem.theme.CfTheme

@Composable
internal fun RemindersSheetFixture(state: RemindersUiState) {
    Box(
        Modifier.fillMaxSize().background(CfTheme.colors.background).windowInsetsPadding(LocalSafeInsets.current),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .widthIn(
                    max = CfSheetMetrics.maxWidth,
                ).fillMaxWidth()
                .clip(CfTheme.shapes.sheetTop)
                .background(CfTheme.colors.card),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CfSheetDragHandle()
            FirstTripRemindersSheetContent(state, {}, {})
        }
    }
}
