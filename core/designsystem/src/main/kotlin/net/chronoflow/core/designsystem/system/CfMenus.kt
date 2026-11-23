package net.chronoflow.core.designsystem.system

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.theme.CfTheme

@Composable
fun CfDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded,
        onDismissRequest,
        modifier,
        scrollState = scrollState,
        shape = RoundedCornerShape(AuthTokens.cardRadius.dp),
        containerColor = CfTheme.colors.popover,
        content = content,
    )
}

@Composable
fun CfDropdownMenuItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    text: @Composable () -> Unit,
) {
    DropdownMenuItem(
        text,
        onClick,
        modifier,
        contentPadding = contentPadding,
        colors = MenuDefaults.itemColors(textColor = CfTheme.colors.foreground),
    )
}
