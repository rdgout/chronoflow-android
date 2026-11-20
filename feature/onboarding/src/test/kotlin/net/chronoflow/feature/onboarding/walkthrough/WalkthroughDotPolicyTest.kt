package net.chronoflow.feature.onboarding.walkthrough

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import net.chronoflow.core.designsystem.generated.PlanUITokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkthroughDotPolicyTest {
    @Test fun reduceMotionSnapsTheDots() {
        val animation =
            TargetBasedAnimation(
                walkthroughDotAnimationSpec(true),
                Float.VectorConverter,
                walkthroughDotWidth(false),
                walkthroughDotWidth(true),
            )
        assertEquals(8f, walkthroughDotWidth(false), 0f)
        assertEquals(24f, walkthroughDotWidth(true), 0f)
        assertEquals(24f, animation.getValueFromNanos(0), 0f)
        assertEquals(0L, animation.durationNanos)
    }

    @Test fun normalMotionUsesTokenDurationAndEaseInOut() {
        val spec = walkthroughDotAnimationSpec(false)
        val millis = (PlanUITokens.walkthroughDotDuration * 1000).toInt()
        assertEquals(tween<Float>(millis, easing = EaseInOut), spec)
        val animation = TargetBasedAnimation(spec, Float.VectorConverter, 8f, 24f)
        val intermediate = animation.getValueFromNanos(16_000_000L)
        assertTrue(intermediate > 8f && intermediate < 24f)
        assertEquals(millis * 1_000_000L, animation.durationNanos)
    }
}
