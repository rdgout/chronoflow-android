package net.chronoflow.feature.auth.guest

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
import net.chronoflow.feature.auth.model.GuestSignupState
import org.junit.Assert.assertEquals
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
class GuestSignupGuardsTest internal constructor(
    private val case: GuestSignupGuardCase,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(case.profile, case.language, case.dark),
            ).around(compose)

    @Test fun sheetFitsTheWindowAndKeepsEveryControlReachable() {
        compose.setContent {
            ProfileHost(case.profile) {
                ChronoFlowTheme(
                    darkTheme = case.dark,
                ) { GuestSignupSheetFrame(GuestSignupUiState(guestState = case.state)) }
            }
        }
        Guards.assertAll(compose, case.profile, headingTags = setOf("guest-signup.title"))
        compose.onNodeWithTag("guest-signup.name").performScrollTo()
        val field = compose.onNodeWithTag("guest-signup.name").fetchSemanticsNode().boundsInRoot
        val frame = compose.onNodeWithTag("guest-signup.frame").fetchSemanticsNode().boundsInRoot
        if (case.profile.name == "foldInner") {
            assertEquals(480f, field.width / case.profile.density, 0.5f)
            assertEquals(frame.center.x, field.center.x, 1f)
        }
        if (case.profile.name == "pixel8") {
            assertEquals(
                frame.width / case.profile.density - 40f,
                field.width / case.profile.density,
                0.5f,
            )
        }
        compose.onNodeWithTag("guest-signup.consent").performScrollTo().assertExists()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        internal fun cases(): List<Array<Any>> =
            GuestSignupState.entries.flatMap { state ->
                listOf(
                    DeviceProfiles.small,
                    DeviceProfiles.pixel8,
                    DeviceProfiles.foldInner,
                    DeviceProfiles.largeFont,
                    DeviceProfiles.landscapeSideNav,
                ).flatMap { profile ->
                    listOf(AppLanguage.En, AppLanguage.Nl).flatMap { language ->
                        listOf(false, true).map { dark ->
                            arrayOf<Any>(GuestSignupGuardCase(state, profile, language, dark))
                        }
                    }
                }
            }
    }
}

internal data class GuestSignupGuardCase(
    val state: GuestSignupState,
    val profile: DeviceProfile,
    val language: AppLanguage,
    val dark: Boolean,
) {
    override fun toString(): String = "$state-${profile.name}-${language.tag}-dark=$dark"
}
