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

enum class CfNavLayout { Bar, Rail, None }

data class CfNavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val tag: String,
)

/** Uses explicit native layout types; the selected entry determines whether chrome is present. */
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
            modifier =
                Modifier.testTag(
                    when (layout) {
                        CfNavLayout.Bar -> "cf.nav.bar"
                        CfNavLayout.Rail -> "cf.nav.rail"
                        CfNavLayout.None -> "cf.nav.none"
                    },
                ),
            layoutType =
                when (layout) {
                    CfNavLayout.Bar -> NavigationSuiteType.NavigationBar
                    CfNavLayout.Rail -> NavigationSuiteType.NavigationRail
                    CfNavLayout.None -> NavigationSuiteType.None
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
