package net.chronoflow.feature.auth.signup

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
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
class SignUpGuardsTest internal constructor(
    private val case: SignUpGuardCase,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(case.profile, case.language, case.dark),
            ).around(compose)

    @Test fun testAllStatesKeepConsentToggleAndFooterReachableAtXXLarge() {
        compose.setContent {
            ProfileHost(case.profile) { ChronoFlowTheme(darkTheme = case.dark) { SignUpSample(case.state) } }
        }
        Guards.assertAll(compose, case.profile, headingTags = setOf("auth.heading"))
        if (case.profile.name == "largeFont") {
            compose.onNodeWithTag("sign-up.visibility").performScrollTo().assertExists()
            compose.onNodeWithTag("sign-up.consent").performScrollTo().assertExists()
            compose.onNodeWithTag("sign-up.back").performScrollTo().assertExists()
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        internal fun cases(): List<Array<Any>> {
            val tallest =
                listOf(
                    DeviceProfiles.small,
                    DeviceProfiles.pixel8,
                    DeviceProfiles.foldInner,
                    DeviceProfiles.landscapeSideNav,
                    DeviceProfiles.largeFont,
                ).flatMap { profile ->
                    listOf(AppLanguage.En, AppLanguage.Nl).map { SignUpGuardCase("empty", profile, it) }
                }
            val small =
                listOf("form", "taken", "server", "throttle", "offline", "visible").map {
                    SignUpGuardCase(it, DeviceProfiles.small, AppLanguage.Nl)
                }
            val large =
                listOf("email", "password", "confirmation").map {
                    SignUpGuardCase(it, DeviceProfiles.largeFont, AppLanguage.Nl)
                }
            return (
                tallest + small + large +
                    SignUpGuardCase(
                        "form",
                        DeviceProfiles.pixel8,
                        AppLanguage.En,
                        true,
                    )
            ).map {
                arrayOf<Any>(it)
            }
        }
    }
}

internal data class SignUpGuardCase(
    val state: String,
    val profile: DeviceProfile,
    val language: AppLanguage,
    val dark: Boolean = false,
) {
    override fun toString() = "$state-${profile.name}-${language.tag}-dark=$dark"
}
