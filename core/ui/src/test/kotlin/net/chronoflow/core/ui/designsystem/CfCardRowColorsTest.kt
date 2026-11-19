@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.ui.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import net.chronoflow.core.designsystem.component.CfBadge
import net.chronoflow.core.designsystem.component.CfBadgeVariant
import net.chronoflow.core.designsystem.component.CfCard
import net.chronoflow.core.designsystem.component.CfPill
import net.chronoflow.core.designsystem.component.CfRow
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.ui.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class CfCardRowColorsTest(
    override val dark: Boolean,
) : PrimitiveTestFixture() {
    @Test fun pillBordersAndBadgeFillsKeepTheirBoardTokens() {
        host {
            Column {
                CfPill(stringResource(R.string.settings_light), true, {}, testTag = "selected")
                CfPill(stringResource(R.string.settings_dark), false, {}, testTag = "unselected")
                CfBadgeVariant.entries.forEach { variant ->
                    CfBadge(stringResource(R.string.screens_now), Modifier.testTag(variant.name), variant)
                }
            }
        }
        val selected = pixels("selected")
        val unselected = pixels("unselected")
        assertTokenColor(selected[selected.width / 2, 0], colors.dawn)
        assertTokenColor(unselected[unselected.width / 2, 0], colors.border)
        mapOf("Default" to colors.primary, "Secondary" to colors.secondary, "Dawn" to colors.dawn).forEach { (tag, c) ->
            val p = pixels(tag)
            assertTokenColor(p[p.width / 2, (2 * compose.density.density).toInt()], c)
        }
    }

    @Test fun rowDisabledAlphaAndDestructiveInkArePreserved() {
        host {
            Column {
                CfCard(rows = true) {
                    CfRow(
                        stringResource(R.string.settings_saved),
                        testTag = "disabled",
                        enabled = false,
                        icon = LucideGlyphs.Lock.imageVector(),
                    )
                    CfRow(stringResource(R.string.settings_saved), testTag = "destructive", destructive = true)
                }
            }
        }
        val faded = colors.muted.copy(alpha = .5f).compositeOver(colors.card)
        val disabled = pixels("disabled")
        assertTokenColor(disabled[(20 * compose.density.density).toInt(), disabled.height / 2], faded)
        assertTrue(pixels("destructive").hasInk(colors.destructive))
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun cases(): Collection<Array<Any>> = listOf(arrayOf(false), arrayOf(true))
    }
}
