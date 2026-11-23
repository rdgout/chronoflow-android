package net.chronoflow.feature.auth.guest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.system.CfSheetDragHandle
import net.chronoflow.core.designsystem.system.CfSheetMetrics
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.feature.auth.signup.PolicyOpener

@Composable
internal fun GuestSignupSheetFrame(
    state: GuestSignupUiState,
    modifier: Modifier = Modifier,
    fields: GuestSignupFields = remember { GuestSignupFields() },
    onEvent: (GuestSignupEvent) -> Unit = {},
) {
    Box(
        modifier
            .fillMaxSize()
            .background(CfTheme.colors.background)
            .windowInsetsPadding(LocalSafeInsets.current.only(WindowInsetsSides.Top)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = CfSheetMetrics.maxWidth)
                .fillMaxWidth()
                .fillMaxHeight()
                .background(CfTheme.colors.card, CfTheme.shapes.sheetTop)
                .testTag("guest-signup.frame"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CfSheetDragHandle()
            GuestSignupContent(state, fields, PolicyOpener {}, onEvent)
        }
    }
}
