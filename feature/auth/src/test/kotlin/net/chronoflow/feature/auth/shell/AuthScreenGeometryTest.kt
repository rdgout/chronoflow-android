package net.chronoflow.feature.auth.shell

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
class AuthScreenGeometryTest(
    private val profile: DeviceProfile,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile)).around(compose)

    @Test fun shellRespectsTheWindowAndKeepsTheFooterReachable() {
        compose.setContent { ProfileHost(profile) { ChronoFlowTheme(darkTheme = false) { AuthShellSample() } } }
        val brand = compose.bounds("auth.brand.row")
        val heading = compose.bounds("auth.heading")
        val subtitle = compose.bounds("auth.subtitle")
        val card = compose.bounds("auth.card")
        val footer = compose.bounds("sample.footer")
        assertEquals(profile.insets.top + 16f, brand.top.value, 0.5f)
        if (profile.widthDp < 600) {
            assertEquals(profile.insets.left + 16f, brand.left.value, 0.5f)
            assertEquals(profile.widthDp - profile.insets.right - 16f, brand.right.value, 0.5f)
        } else {
            assertEquals(560f, (brand.right - brand.left).value, 0.5f)
            assertEquals(
                (profile.widthDp + profile.insets.left - profile.insets.right) / 2f,
                ((brand.left.value + brand.right.value) / 2f),
                0.5f,
            )
        }
        assertEquals(brand.bottom.value + 32f, heading.top.value, 0.5f)
        assertTrue(card.top.value >= subtitle.bottom.value + 24f - 0.5f)
        assertEquals(card.bottom.value + 12f, footer.top.value, 0.5f)
        assertTrue((footer.bottom - footer.top).value >= 48f)
        if (profile.name ==
            "pixel8"
        ) {
            assertEquals(profile.heightDp - profile.insets.bottom - 24f, footer.bottom.value, 0.5f)
        }
        if (profile.name == "small" || profile.name == "largeFont") {
            compose.onNodeWithTag("sample.footer").performScrollTo()
            val visible = compose.onNodeWithTag("sample.footer").fetchSemanticsNode().boundsInRoot
            assertTrue(visible.height >= 48f * profile.density - 1f)
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            listOf(
                DeviceProfiles.small,
                DeviceProfiles.pixel8,
                DeviceProfiles.foldInner,
                DeviceProfiles.landscapeSideNav,
                DeviceProfiles.largeFont,
            ).map {
                arrayOf<Any>(it)
            }
    }
}
