package net.chronoflow.feature.launch.welcome

import androidx.compose.runtime.LongState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import net.chronoflow.core.designsystem.generated.WelcomeTokens
import net.chronoflow.feature.launch.model.LaunchIntro
import kotlin.time.Duration.Companion.nanoseconds

internal fun Modifier.introItem(
    index: Int,
    intro: LaunchIntro?,
    now: LongState,
): Modifier =
    if (intro == null) {
        this
    } else {
        graphicsLayer {
            val progress = intro.progress(index, now.longValue.nanoseconds).toFloat()
            alpha = progress
            translationY = WelcomeTokens.introRise * density * (1 - progress)
        }
    }
