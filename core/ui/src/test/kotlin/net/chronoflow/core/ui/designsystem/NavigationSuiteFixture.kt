package net.chronoflow.core.ui.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.chronoflow.core.designsystem.icon.MaterialSymbols
import net.chronoflow.core.designsystem.system.CfNavItem
import net.chronoflow.core.ui.R

@Composable
internal fun navigationItems() =
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
