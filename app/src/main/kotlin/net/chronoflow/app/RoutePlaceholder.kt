package net.chronoflow.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.debugName
import net.chronoflow.feature.launch.model.LaunchRoute

@Composable
internal fun RoutePlaceholder(
    route: Route,
    label: (@Composable (String) -> Unit)?,
) {
    RoutePlaceholder(route.debugName(), label)
}

@Composable
internal fun RoutePlaceholder(
    route: LaunchRoute,
    label: (@Composable (String) -> Unit)?,
) {
    RoutePlaceholder(route.name.lowercase(), label)
}

@Composable
internal fun RoutePlaceholder(
    rawName: String,
    label: (@Composable (String) -> Unit)?,
) {
    val background = Modifier.background(CfTheme.colors.background)
    Box(Modifier.fillMaxSize().then(background).testTag("route.$rawName"), contentAlignment = Alignment.Center) {
        label?.invoke(rawName)
    }
}
