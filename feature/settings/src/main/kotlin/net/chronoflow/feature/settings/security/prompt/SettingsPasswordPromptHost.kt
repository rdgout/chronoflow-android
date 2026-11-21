@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.feature.settings.security.prompt

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.generated.SettingsTokens
import net.chronoflow.core.designsystem.layout.BoardMetrics
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.layout.WindowPolicy
import net.chronoflow.core.designsystem.system.CfModalBottomSheet
import net.chronoflow.core.designsystem.theme.CfTheme

@BoardMetrics("PasswordPromptWide")
internal object PasswordPromptMetrics {
    val width = 540.dp
    const val HEIGHT_FRACTION = 0.9f
    val lockSize = 18.dp
    val fieldGap = 10.dp
}

@Composable
internal fun SettingsPasswordPromptHost(
    visible: Boolean,
    busy: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (!visible) return
    val checking by rememberUpdatedState(busy)
    val currentContent by rememberUpdatedState(content)
    val body = remember { movableContentOf { currentContent() } }
    val wide = currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(WindowPolicy.wideBreakpointDp)
    if (wide) {
        val pixels = LocalWindowInfo.current.containerSize
        val density = LocalDensity.current
        val width = with(density) { pixels.width.toDp() }
        val height = with(density) { pixels.height.toDp() }
        Dialog(
            onDismissRequest = { if (!checking) onDismiss() },
            properties =
                DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = !busy,
                    dismissOnClickOutside = !busy,
                ),
        ) {
            Surface(
                Modifier
                    .width(minOf(PasswordPromptMetrics.width, width - PlanUITokens.space8.dp))
                    .heightIn(max = height * PasswordPromptMetrics.HEIGHT_FRACTION)
                    .testTag("settings.security.prompt.dialog")
                    .semantics { testTagsAsResourceId = true },
                shape = RoundedCornerShape(SettingsTokens.sheetRadius.dp),
                color = CfTheme.colors.card,
                contentColor = CfTheme.colors.cardForeground,
            ) {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = PlanUITokens.space5.dp)
                        .padding(top = PasswordPromptMetrics.fieldGap, bottom = PlanUITokens.space8.dp),
                ) { body() }
            }
        }
    } else {
        val sheet =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { it != SheetValue.Hidden || !checking },
            )
        CfModalBottomSheet(
            onDismissRequest = { if (!checking) onDismiss() },
            modifier = Modifier.testTag("settings.security.prompt.sheet"),
            sheetState = sheet,
            properties = ModalBottomSheetProperties(shouldDismissOnBackPress = !busy),
        ) {
            Column(
                Modifier
                    .windowInsetsPadding(
                        LocalSafeInsets.current
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                            .union(WindowInsets.ime),
                    ).verticalScroll(rememberScrollState())
                    .padding(horizontal = PlanUITokens.space5.dp)
                    .padding(bottom = PlanUITokens.space8.dp),
            ) { body() }
        }
    }
}
