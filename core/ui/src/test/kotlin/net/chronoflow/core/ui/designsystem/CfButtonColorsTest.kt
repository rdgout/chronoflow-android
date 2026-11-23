@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.ui.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.ui.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class CfButtonColorsTest(
    override val dark: Boolean,
) : PrimitiveTestFixture() {
    @Test fun everyButtonStyleUsesItsDesignedFillAndOutline() {
        host {
            Column {
                CfButtonStyle.entries.forEach { style ->
                    CfButton(stringResource(R.string.auth_sign_in), {}, Modifier.testTag(style.name), style = style)
                }
            }
        }
        val expected =
            mapOf(
                "Primary" to colors.primary,
                "Secondary" to colors.secondary,
                "Outline" to colors.background,
                "Ghost" to colors.background,
                "GhostDestructive" to colors.background,
                "Destructive" to colors.destructive,
                "Dawn" to colors.dawn,
                "OnSky" to colors.skyCopyBackground.compositeOver(colors.background),
            )
        expected.forEach { (tag, fill) ->
            val p = pixels(tag)
            assertTokenColor(p[p.width / 2, (4 * compose.density.density).toInt()], fill)
            if (tag == "Outline") assertTokenColor(p[p.width / 2, 0], colors.input)
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun cases(): Collection<Array<Any>> = listOf(arrayOf(false), arrayOf(true))
    }
}
