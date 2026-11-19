package net.chronoflow.feature.trip.state

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import net.chronoflow.core.designsystem.component.CfIconButton
import net.chronoflow.core.designsystem.icon.MaterialSymbols
import net.chronoflow.core.designsystem.system.CfBackButton
import net.chronoflow.core.designsystem.system.CfCenterAlignedTopAppBar
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.ui.R

@Composable
fun TripPageLoadingBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier,
        topBar = {
            CfCenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.plan_loading_your_plan),
                        Modifier.testTag("trip.loadingTitle"),
                        style = CfTheme.type.planTitle,
                        color = CfTheme.colors.foreground,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    CfBackButton(
                        stringResource(R.string.screens_my_trips),
                        onBack,
                        Modifier.testTag("trip.back"),
                    )
                },
                actions = {
                    CfIconButton(MaterialSymbols.MoreVert, stringResource(R.string.plan_trip_actions), {
                    }, Modifier.testTag("trip.actions.open"), enabled = false)
                },
            )
        },
        contentWindowInsets =
            WindowInsets(
                0,
                0,
                0,
                0,
            ),
        containerColor = CfTheme.colors.background,
    ) { inner -> Box(Modifier.padding(inner)) }
}
