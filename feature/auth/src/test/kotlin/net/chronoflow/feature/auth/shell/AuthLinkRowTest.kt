package net.chronoflow.feature.auth.shell

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
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
class AuthLinkRowTest(
    private val profile: DeviceProfile,
    private val language: AppLanguage,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile, language)).around(compose)

    @Test fun linksShareARowOnlyWhenTheirIntrinsicWidthsFit() {
        compose.setContent { ProfileHost(profile) { ChronoFlowTheme(darkTheme = false) { AuthLinksSample() } } }
        val row = compose.bounds("auth.links")
        val forgot = compose.bounds("sign-in.forgot")
        val signup = compose.bounds("sign-in.signup")
        assertEquals(row.left.value, forgot.left.value, 0.5f)
        assertTrue((forgot.bottom - forgot.top).value >= 48f)
        assertTrue((signup.bottom - signup.top).value >= 48f)
        if (language == AppLanguage.En) {
            assertEquals(forgot.top.value, signup.top.value, 0.5f)
            assertEquals(row.right.value, signup.right.value, 0.5f)
        } else {
            assertTrue(forgot.bottom.value <= signup.top.value)
            assertEquals(row.left.value, signup.left.value, 0.5f)
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun cases() =
            listOf(
                arrayOf<Any>(DeviceProfiles.pixel8, AppLanguage.En),
                arrayOf<Any>(DeviceProfiles.small.copy(fontScale = 2f), AppLanguage.Nl),
            )
    }
}
