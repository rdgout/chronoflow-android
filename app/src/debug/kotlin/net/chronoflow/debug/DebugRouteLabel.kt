@file:JvmName("DebugRouteLabel")

package net.chronoflow.debug

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import net.chronoflow.core.ui.R

@Composable
fun DebugRouteLabel(
    rawName: String,
    modifier: Modifier = Modifier,
) {
    Text(stringResource(R.string.app_debug_route_x, rawName), modifier = modifier.testTag("route.label"))
}
