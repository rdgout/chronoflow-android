@file:Suppress("ktlint:compose:modifier-missing-check")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.designsystem.system

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.layout.BoardMetrics
import net.chronoflow.core.designsystem.theme.CfTheme

@BoardMetrics("ItemDetail", "SheetStates")
private object SheetMetrics {
    // Native counterparts of these sheet panels use plan.md's 640 dp window-policy cap.
    val maxWidth = 640.dp
}

@Composable
fun CfModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest,
        modifier,
        sheetState = sheetState,
        sheetMaxWidth = SheetMetrics.maxWidth,
        shape = CfTheme.shapes.sheetTop,
        containerColor = CfTheme.colors.card,
        contentColor = CfTheme.colors.cardForeground,
        dragHandle = { BottomSheetDefaults.DragHandle(Modifier.minimumInteractiveComponentSize()) },
        scrimColor = BottomSheetDefaults.ScrimColor,
        content = content,
    )
}

data class CfDialogAction(
    val label: String,
    val onClick: () -> Unit,
    val destructive: Boolean = false,
)

@Composable
internal fun DialogAction(
    action: CfDialogAction,
    tag: String,
) {
    TextButton(
        action.onClick,
        Modifier.testTag(tag),
        colors =
            ButtonDefaults.textButtonColors(
                contentColor = if (action.destructive) CfTheme.colors.destructive else CfTheme.colors.dawnText,
            ),
    ) {
        Text(action.label)
    }
}

@Composable
fun CfAlertDialog(
    title: String,
    text: String?,
    confirm: CfDialogAction,
    onDismissRequest: () -> Unit,
    dismiss: CfDialogAction? = null,
) {
    AlertDialog(
        onDismissRequest,
        confirmButton = { DialogAction(confirm, "cf.dialog.confirm") },
        dismissButton = dismiss?.let { { DialogAction(it, "cf.dialog.dismiss") } },
        title = { Text(title) },
        text = text?.let { { Text(it) } },
        containerColor = CfTheme.colors.popover,
        titleContentColor = CfTheme.colors.foreground,
        textContentColor = CfTheme.colors.mutedForeground,
    )
}

@Suppress("ktlint:compose:parameter-naming")
@Composable
fun CfSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onPresented: (SnackbarData) -> Unit = {},
) {
    val presented by rememberUpdatedState(onPresented)
    SnackbarHost(hostState, modifier) { data ->
        LaunchedEffect(data) { presented(data) }
        Snackbar(
            data,
            containerColor = CfTheme.colors.foreground,
            contentColor = CfTheme.colors.background,
            actionColor = CfTheme.colors.background,
            actionContentColor = CfTheme.colors.background,
            dismissActionContentColor = CfTheme.colors.background,
        )
    }
}
