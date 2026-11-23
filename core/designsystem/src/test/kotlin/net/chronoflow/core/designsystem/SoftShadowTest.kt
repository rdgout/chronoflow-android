package net.chronoflow.core.designsystem

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.softShadowFor
import net.chronoflow.core.designsystem.generated.CfDarkColors
import net.chronoflow.core.designsystem.generated.CfLightColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SoftShadowTest {
    @Test fun shadowUsesThePerThemeGeometryAndAlpha() {
        listOf(false, true).forEach { dark ->
            val shadow = softShadowFor(dark)
            assertSame(shadow, softShadowFor(dark))
            assertEquals((if (dark) 40 else 30).dp, shadow.radius)
            assertEquals((if (dark) -14 else -12).dp, shadow.spread)
            assertEquals(DpOffset(0.dp, (if (dark) 12 else 10).dp), shadow.offset)
            val color = (if (dark) CfDarkColors else CfLightColors).authShadow
            assertEquals(color.copy(alpha = 1f), shadow.color)
            assertEquals(color.alpha, shadow.alpha)
        }
    }
}
