package net.chronoflow.feature.auth.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class AuthHeadingGlyphTest(
    private val glyph: AuthHeadingGlyph,
    private val dark: Boolean,
) {
    private val profile = DeviceProfiles.pixel8
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile, dark = dark)).around(compose)

    @Test fun badgeIsDecorativeAnd48DpSquare() {
        compose.setContent {
            ChronoFlowTheme(darkTheme = dark) {
                Box(Modifier.testTag("glyph.host")) { AuthHeadingBadge(glyph) }
            }
        }
        val bounds = compose.bounds("glyph.host")
        assertEquals(48f, (bounds.right - bounds.left).value, 0.5f)
        assertEquals(48f, (bounds.bottom - bounds.top).value, 0.5f)
        val node = compose.onNodeWithTag("glyph.host", useUnmergedTree = true).fetchSemanticsNode()
        val badge = node.children.single()
        assertFalse(badge.config.contains(SemanticsProperties.ContentDescription))
        assertFalse(badge.config.contains(SemanticsProperties.Text))
        assertEquals(0, badge.children.size)
    }

    @Test fun shellLeadsWithTheBadgeAndUsesTheTokenFill() {
        var expected = Color.Unspecified
        compose.setContent {
            ProfileHost(profile) {
                ChronoFlowTheme(darkTheme = dark) {
                    expected = if (glyph == AuthHeadingGlyph.Check) CfTheme.colors.dawn else CfTheme.colors.card
                    AuthScreen(
                        "glyph.scroll",
                        stringResource(
                            R.string.auth_password_saved,
                        ),
                        stringResource(R.string.auth_password_updated_sign_in_with_your_n_12f248),
                        {
                        },
                        headingGlyph = glyph,
                    ) {
                        Text(stringResource(R.string.auth_continue))
                    }
                }
            }
        }
        val title = compose.bounds("auth.heading")
        val brand = compose.bounds("auth.brand.row")
        val circleTop = title.top.value - 16f - 48f
        assertEquals(brand.bottom.value + 32f, circleTop, 0.5f)
        val pixels = compose.onRoot().captureToImage().toPixelMap()
        val density = profile.density
        val x = ((title.left.value + 4f) * density).toInt()
        val y = ((circleTop + 24f) * density).toInt()
        assertEquals(expected, pixels[x, y])
        assertEquals(
            expected,
            pixels[((title.left.value + 24f) * density).toInt(), ((circleTop + 4f) * density).toInt()],
        )
        assertEquals(expected, pixels[((title.left.value + 44f) * density).toInt(), y])
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_dark={1}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf(AuthHeadingGlyph.Problem, false),
                arrayOf(AuthHeadingGlyph.Problem, true),
                arrayOf(AuthHeadingGlyph.Check, false),
                arrayOf(AuthHeadingGlyph.Check, true),
            )
    }
}
