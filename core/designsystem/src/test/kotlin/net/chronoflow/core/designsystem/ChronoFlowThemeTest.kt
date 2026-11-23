@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.designsystem

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.LocalTonalElevationEnabled
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.designsystem.generated.CfDarkColors
import net.chronoflow.core.designsystem.generated.CfLightColors
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.designsystem.theme.LocalReduceMotion
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class ChronoFlowThemeTest(
    private val dark: Boolean,
) {
    @get:Rule val compose = createComposeRule()

    @Test fun themeProvidesTheTokenInstancesAndNativeControlLocals() {
        var observed = false
        compose.setContent {
            ChronoFlowTheme(darkTheme = dark, reduceMotion = true) {
                assertSame(if (dark) CfDarkColors else CfLightColors, CfTheme.colors)
                assertEquals(CfTheme.colors.dawnText, MaterialTheme.colorScheme.primary)
                assertEquals(CfTheme.colors.foreground, LocalRippleConfiguration.current?.color)
                assertEquals(CfTheme.colors.dawnText, LocalTextSelectionColors.current.handleColor)
                assertTrue(LocalReduceMotion.current)
                assertFalse(LocalTonalElevationEnabled.current)
                SideEffect { observed = true }
            }
        }
        compose.runOnIdle { assertTrue(observed) }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun cases(): Collection<Array<Any>> = listOf(arrayOf(false), arrayOf(true))
    }
}
