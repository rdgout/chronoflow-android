package net.chronoflow.feature.launch.welcome

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import net.chronoflow.feature.launch.model.LaunchRect
import net.chronoflow.feature.launch.model.MoonAnchorStore

internal fun Modifier.reportMoonAnchor(
    store: MoonAnchorStore?,
    density: Float,
): Modifier =
    if (store == null) {
        this
    } else {
        onGloballyPositioned { coordinates ->
            val origin = coordinates.positionInWindow()
            store.update(
                LaunchRect(
                    (origin.x / density).toDouble(),
                    (origin.y / density).toDouble(),
                    (
                        coordinates.size.width /
                            density
                    ).toDouble(),
                    (coordinates.size.height / density).toDouble(),
                ),
            )
        }
    }
