package net.chronoflow.feature.auth.signin

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.auth.shell.AuthStatesSample
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
class SignInGuardsTest internal constructor(
    private val case: SignInGuardCase,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(case.profile, case.language)).around(compose)

    @Test fun signInStatesRespectTheWindowAndTextCap() {
        compose.setContent {
            ProfileHost(case.profile) {
                ChronoFlowTheme(darkTheme = false) {
                    if (case.state == "authStates") AuthStatesSample() else SignInSample(case.state, case.passkey)
                }
            }
        }
        if (case.state == "authStates") {
            Guards.assertTextFits(compose, case.profile)
            Guards.assertInk(compose, case.profile)
        } else {
            Guards.assertAll(compose, case.profile, headingTags = setOf("auth.heading"))
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        internal fun cases(): List<Array<Any>> {
            val locked =
                listOf(
                    DeviceProfiles.small,
                    DeviceProfiles.pixel8,
                    DeviceProfiles.foldInner,
                    DeviceProfiles.landscapeSideNav,
                    DeviceProfiles.largeFont,
                ).flatMap { profile ->
                    listOf(AppLanguage.En, AppLanguage.Nl).map { language ->
                        SignInGuardCase("locked", profile, language)
                    }
                }
            val fields =
                listOf(DeviceProfiles.small, DeviceProfiles.largeFont).map {
                    SignInGuardCase("fieldError", it, AppLanguage.Nl)
                }
            val wrong =
                listOf(
                    AppLanguage.En,
                    AppLanguage.Nl,
                ).map { SignInGuardCase("wrong", DeviceProfiles.small, it) }
            return (
                locked + fields + wrong +
                    listOf(
                        SignInGuardCase("busy", DeviceProfiles.pixel8, AppLanguage.En),
                        SignInGuardCase("none", DeviceProfiles.pixel8, AppLanguage.En, false),
                        SignInGuardCase("authStates", DeviceProfiles.small, AppLanguage.Nl),
                    )
            ).map { arrayOf<Any>(it) }
        }
    }
}

internal data class SignInGuardCase(
    val state: String,
    val profile: DeviceProfile,
    val language: AppLanguage,
    val passkey: Boolean = true,
) {
    override fun toString(): String = "$state-${profile.name}-${language.tag}-passkey=$passkey"
}
