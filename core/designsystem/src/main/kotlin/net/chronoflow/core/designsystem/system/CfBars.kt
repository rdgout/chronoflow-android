@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.designsystem.system

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import net.chronoflow.core.designsystem.icon.MaterialSymbols
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.semantics.ellipsizable
import net.chronoflow.core.designsystem.theme.CfTheme

@Composable
private fun appBarColors() =
    TopAppBarDefaults.topAppBarColors(
        containerColor = CfTheme.colors.background,
        scrolledContainerColor = CfTheme.colors.popover,
        navigationIconContentColor = CfTheme.colors.foreground,
        titleContentColor = CfTheme.colors.foreground,
        actionIconContentColor = CfTheme.colors.foreground,
    )

@Composable
fun CfTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        title = {
            Text(
                title,
                Modifier.ellipsizable(),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
        navigationIcon = { navigationIcon?.invoke() },
        actions = actions,
        windowInsets = LocalSafeInsets.current.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        colors = appBarColors(),
        scrollBehavior = scrollBehavior,
    )
}

@Composable
fun CfLargeTopAppBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    LargeTopAppBar(
        title = {
            Text(
                title,
                Modifier.ellipsizable(),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
        navigationIcon = { navigationIcon?.invoke() },
        actions = actions,
        windowInsets = LocalSafeInsets.current.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        colors = appBarColors(),
        scrollBehavior = scrollBehavior,
    )
}

@Composable
fun CfBackButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick,
        modifier,
    ) { Icon(MaterialSymbols.ArrowBack, contentDescription, tint = CfTheme.colors.foreground) }
}
