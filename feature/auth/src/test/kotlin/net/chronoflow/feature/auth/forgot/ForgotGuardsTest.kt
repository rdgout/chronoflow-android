package net.chronoflow.feature.auth.forgot

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
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
class ForgotGuardsTest internal constructor(
    private val case: ForgotGuardCase,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(case.profile, case.language)).around(compose)

    @Test fun forgotStatesRespectTheWindowAndTextCap() {
        compose.setContent {
            ProfileHost(case.profile) { ChronoFlowTheme(darkTheme = false) { ForgotSample(case.state, case.expired) } }
        }
        Guards.assertAll(compose, case.profile, headingTags = setOf("auth.heading"))
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        internal fun cases(): List<Array<Any>> {
            val expired =
                listOf(
                    DeviceProfiles.small,
                    DeviceProfiles.pixel8,
                    DeviceProfiles.foldInner,
                    DeviceProfiles.landscapeSideNav,
                    DeviceProfiles.largeFont,
                ).flatMap { profile ->
                    listOf(AppLanguage.En, AppLanguage.Nl).map { ForgotGuardCase("server", profile, it, true) }
                }
            return (
                expired +
                    listOf(
                        ForgotGuardCase("sent", DeviceProfiles.small, AppLanguage.Nl),
                        ForgotGuardCase("sent", DeviceProfiles.largeFont, AppLanguage.Nl),
                        ForgotGuardCase("fieldError", DeviceProfiles.small, AppLanguage.Nl),
                        ForgotGuardCase("busy", DeviceProfiles.pixel8, AppLanguage.En),
                    )
            ).map { arrayOf<Any>(it) }
        }
    }
}

internal data class ForgotGuardCase(
    val state: String,
    val profile: DeviceProfile,
    val language: AppLanguage,
    val expired: Boolean = false,
) {
    override fun toString(): String = "$state-${profile.name}-${language.tag}-expired=$expired"
}
