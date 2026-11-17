@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.ui.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import net.chronoflow.core.designsystem.component.CfBanner
import net.chronoflow.core.designsystem.component.CfNotice
import net.chronoflow.core.designsystem.component.CfNoticeTone
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
class CfBannerNoticeColorsTest(
    override val dark: Boolean,
) : PrimitiveTestFixture() {
    @Test fun bannerUsesTheErrorSurfaceAndBorder() {
        host { CfBanner(stringResource(R.string.settings_saved), null, testTag = "banner") }
        val p = pixels("banner")
        assertTokenColor(p[p.width / 2, (4 * compose.density.density).toInt()], colors.authErrorBackground)
        assertTokenColor(p[p.width / 2, 0], colors.authErrorBorder.compositeOver(colors.authErrorBackground))
    }

    @Test fun noticesUseTheirToneSpecificFillsAndBorders() {
        host {
            Column {
                CfNoticeTone.entries.forEach { tone ->
                    CfNotice(
                        stringResource(R.string.settings_saved),
                        stringResource(R.string.settings_saved),
                        Modifier.testTag(tone.name),
                        tone,
                    )
                }
            }
        }
        mapOf(
            "Info" to (colors.card to colors.border),
            "Warning" to (colors.settingsWarningBackground to colors.settingsWarningBorder),
        ).forEach { (tag, pair) ->
            val p = pixels(tag)
            assertTokenColor(p[p.width / 2, (4 * compose.density.density).toInt()], pair.first)
            assertTokenColor(p[p.width / 2, 0], pair.second.compositeOver(pair.first))
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun cases(): Collection<Array<Any>> = listOf(arrayOf(false), arrayOf(true))
    }
}
