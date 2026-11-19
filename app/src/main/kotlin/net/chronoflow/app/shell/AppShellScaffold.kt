package net.chronoflow.app.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreProvider
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import net.chronoflow.core.designsystem.icon.MaterialSymbols
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.layout.WindowPolicy
import net.chronoflow.core.designsystem.system.CfNavItem
import net.chronoflow.core.designsystem.system.CfNavLayout
import net.chronoflow.core.designsystem.system.CfNavigationSuiteScaffold
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.shell.ShellTab
import net.chronoflow.core.navigation.stateKey
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.shell.LocalTabReselect

/** Labels use the generated screens catalog; chrome keeps the native Material bar/rail defaults. */
@Composable
internal fun AppShellScaffold(
    holder: ShellViewModel,
    today: @Composable () -> Unit,
    trips: @Composable () -> Unit,
    settings: @Composable () -> Unit,
    page: @Composable (Route) -> Unit,
    overlay: @Composable (Route) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = holder.state
    val safe = LocalSafeInsets.current
    val layout =
        if (currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(WindowPolicy.wideBreakpointDp)) {
            CfNavLayout.Rail
        } else {
            CfNavLayout.Bar
        }
    val contentInsets =
        safe.only(
            if (layout == CfNavLayout.Bar) {
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal
            } else {
                WindowInsetsSides.Top + WindowInsetsSides.End + WindowInsetsSides.Bottom
            },
        )
    val items =
        listOf(
            CfNavItem(
                stringResource(R.string.screens_today),
                MaterialSymbols.SpaceDashboard,
                MaterialSymbols.SpaceDashboardFill,
                "tab.today",
            ),
            CfNavItem(
                stringResource(R.string.screens_my_trips),
                MaterialSymbols.Flight,
                MaterialSymbols.FlightFill,
                "tab.trips",
            ),
            CfNavItem(
                stringResource(R.string.screens_settings),
                MaterialSymbols.Settings,
                MaterialSymbols.SettingsFill,
                "tab.settings",
            ),
        )
    val saveable = rememberSaveableStateHolder()
    // Lifecycle 2.11 clears entries on pop; remembering every tab's entries keeps a tab switch from becoming a pop.
    val entries =
        ShellTab.entries.associateWith { tab ->
            key(tab) {
                rememberTabEntries(holder, tab) { route ->
                    CompositionLocalProvider(LocalTabReselect provides (holder.state.scrollToTop[tab] ?: 0)) {
                        when (route) {
                            Route.Today -> today()
                            Route.Trips -> trips()
                            Route.Settings -> settings()
                            else -> page(route)
                        }
                    }
                }
            }
        }
    val currentOverlay = state.overlay
    if (currentOverlay == null) {
        CfNavigationSuiteScaffold(
            layout,
            items,
            state.selected.ordinal,
            { holder.select(ShellTab.entries[it]) },
            modifier.fillMaxSize(),
        ) {
            CompositionLocalProvider(
                LocalSafeInsets provides contentInsets,
                LocalViewModelStoreOwner provides holder.owner(state.selected),
            ) {
                saveable.SaveableStateProvider(state.selected.name) {
                    NavDisplay(entries = entries.getValue(state.selected), onBack = { holder.back() })
                }
            }
        }
    } else {
        Box(modifier.fillMaxSize().background(CfTheme.colors.background)) { overlay(currentOverlay) }
    }
    val atTabRoot = state.stacks.getValue(state.selected).size == 1
    val backSelectsToday = currentOverlay == null && atTabRoot && state.selected != ShellTab.Today
    BackHandler(enabled = backSelectsToday) {
        holder.back()
    }
    BackHandler(enabled = currentOverlay != null) { holder.back() }
}

@Composable
private fun rememberTabEntries(
    holder: ShellViewModel,
    tab: ShellTab,
    content: @Composable (Route) -> Unit,
): List<NavEntry<Route>> {
    val currentContent = rememberUpdatedState(content)
    val stack = holder.state.stacks.getValue(tab)
    val entries =
        remember(stack) {
            stack.mapIndexed { index, route ->
                NavEntry(route, contentKey = "$index:${route.stateKey()}") { currentContent.value(it) }
            }
        }
    val parent = checkNotNull(LocalViewModelStoreOwner.current) as? HasDefaultViewModelProviderFactory
    val factory = parent?.defaultViewModelProviderFactory ?: remember { ViewModelProvider.NewInstanceFactory() }
    val provider =
        rememberViewModelStoreProvider(
            holder.owner(tab),
            defaultCreationExtras = parent?.defaultViewModelCreationExtras ?: CreationExtras.Empty,
            defaultFactory = factory,
        )
    return rememberDecoratedNavEntries(
        entries,
        listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(provider),
        ),
    )
}
