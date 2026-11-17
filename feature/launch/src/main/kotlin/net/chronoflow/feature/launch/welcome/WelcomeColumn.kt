package net.chronoflow.feature.launch.welcome

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.feature.launch.model.LaunchSize
import net.chronoflow.feature.launch.model.MoonTarget
import net.chronoflow.feature.launch.model.WelcomeGeometry
import kotlin.math.roundToInt

@Composable
internal fun WelcomeColumn(
    heading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit,
) {
    val insets = LocalSafeInsets.current
    val direction = LocalLayoutDirection.current
    val density = LocalDensity.current
    Layout(content = {
        heading()
        actions()
    }, modifier = modifier) { measurables, constraints ->
        val d = density.density
        val g =
            WelcomeGeometry(
                LaunchSize((constraints.maxWidth / d).toDouble(), (constraints.minHeight / d).toDouble()),
                MoonTarget.Insets(
                    insets.getTop(density) / d.toDouble(),
                    insets.getLeft(density, direction) / d.toDouble(),
                    insets.getBottom(density) / d.toDouble(),
                    insets.getRight(density, direction) / d.toDouble(),
                ),
            )
        val childConstraints = Constraints.fixedWidth((g.content.width * d).roundToInt())
        val headingPlaceable = measurables[0].measure(childConstraints)
        val actionsPlaceable = measurables[1].measure(childConstraints)
        val groups = g.groups(headingPlaceable.height / d.toDouble(), actionsPlaceable.height / d.toDouble())
        val height = maxOf(constraints.minHeight, ((groups.actions.maxY + g.bottom) * d).roundToInt())
        layout(constraints.maxWidth, height) {
            headingPlaceable.place((groups.heading.minX * d).roundToInt(), (groups.heading.minY * d).roundToInt())
            actionsPlaceable.place((groups.actions.minX * d).roundToInt(), (groups.actions.minY * d).roundToInt())
        }
    }
}
