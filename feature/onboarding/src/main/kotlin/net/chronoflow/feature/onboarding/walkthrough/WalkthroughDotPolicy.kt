package net.chronoflow.feature.onboarding.walkthrough

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import net.chronoflow.core.designsystem.generated.PlanUITokens

internal fun walkthroughDotWidth(active: Boolean): Float = if (active) PlanUITokens.space6 else PlanUITokens.space2

internal fun walkthroughDotAnimationSpec(reduceMotion: Boolean): FiniteAnimationSpec<Float> =
    if (reduceMotion) {
        snap()
    } else {
        tween(
            (PlanUITokens.walkthroughDotDuration * 1000).toInt(),
            easing = EaseInOut,
        )
    }
