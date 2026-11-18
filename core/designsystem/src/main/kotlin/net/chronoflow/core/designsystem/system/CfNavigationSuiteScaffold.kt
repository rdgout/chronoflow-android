package net.chronoflow.core.designsystem.system

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import net.chronoflow.core.designsystem.theme.CfTheme

enum class CfNavLayout { Bar, Rail }

data class CfNavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val tag: String,
)

/** Uses the 1.4.0 layoutType overload to retain the standard, permanently visible bar and rail. */
@Composable
fun CfNavigationSuiteScaffold(
    layout: CfNavLayout,
    items: List<CfNavItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (CfNavItem, Boolean) -> Unit = { item, active ->
        Icon(if (active) item.selectedIcon else item.icon, contentDescription = null)
    },
    content: @Composable () -> Unit,
) {
    val colors = CfTheme.colors
    val itemColors =
        NavigationSuiteDefaults.itemColors(
            navigationBarItemColors =
                NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.primary,
                    selectedTextColor = colors.primary,
                    indicatorColor = colors.muted,
                    unselectedIconColor = colors.mutedForeground,
                    unselectedTextColor = colors.mutedForeground,
                ),
            navigationRailItemColors =
                NavigationRailItemDefaults.colors(
                    selectedIconColor = colors.primary,
                    selectedTextColor = colors.primary,
                    indicatorColor = colors.muted,
                    unselectedIconColor = colors.mutedForeground,
                    unselectedTextColor = colors.mutedForeground,
                ),
        )
    Box(modifier.testTag("app.tabs")) {
        NavigationSuiteScaffold(
            modifier = Modifier.testTag(if (layout == CfNavLayout.Bar) "cf.nav.bar" else "cf.nav.rail"),
            layoutType =
                if (layout ==
                    CfNavLayout.Bar
                ) {
                    NavigationSuiteType.NavigationBar
                } else {
                    NavigationSuiteType.NavigationRail
                },
            navigationSuiteItems = {
                items.forEachIndexed { index, navItem ->
                    item(
                        selected = selected == index,
                        onClick = { onSelect(index) },
                        icon = { icon(navItem, selected == index) },
                        label = { Text(navItem.label) },
                        alwaysShowLabel = true,
                        colors = itemColors,
                        modifier = Modifier.testTag(navItem.tag),
                    )
                }
            },
            content = content,
        )
    }
}
