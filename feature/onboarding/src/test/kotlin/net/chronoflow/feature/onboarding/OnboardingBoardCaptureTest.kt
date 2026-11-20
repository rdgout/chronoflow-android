package net.chronoflow.feature.onboarding

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Capture
import net.chronoflow.feature.onboarding.terms.TermsScreen
import net.chronoflow.feature.onboarding.terms.TermsUiState
import net.chronoflow.feature.onboarding.walkthrough.WalkthroughModel
import net.chronoflow.feature.onboarding.walkthrough.WalkthroughScreen
import net.chronoflow.feature.onboarding.walkthrough.WalkthroughUiState
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

@Capture
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
class OnboardingBoardCaptureTest(
    private val board: String,
    private val language: Language,
    private val dark: Boolean,
) {
    private val profile = DeviceProfiles.pixel8.copy(widthDp = 375, heightDp = 812)
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(profile, if (language == Language.Nl) AppLanguage.Nl else AppLanguage.En, dark),
            ).around(compose)

    @Test fun captureBoard() {
        compose.setContent {
            ProfileHost(profile) {
                ChronoFlowTheme(darkTheme = dark, reduceMotion = true) {
                    if (board.startsWith("Terms")) {
                        TermsScreen(
                            TermsUiState(policies = AccountPolicies("1", "1"), failed = board == "TermsFailed"),
                            {},
                        )
                    } else {
                        val page =
                            when (board) {
                                "Walkthrough2" -> WalkthroughModel.Page.Categories
                                "Walkthrough3" -> WalkthroughModel.Page.Progress
                                else -> WalkthroughModel.Page.Timeline
                            }
                        WalkthroughScreen(WalkthroughUiState(page = page), {})
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        BoardCapture.capture(compose, "${board}_pixel8_${language.raw}_${if (dark) "dark" else "light"}")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-{2}")
        fun cases() =
            listOf(
                "Walkthrough",
                "Walkthrough2",
                "Walkthrough3",
                "WalkthroughReplay",
                "TermsUpdate",
                "TermsFailed",
            ).flatMap { board ->
                listOf(Language.En, Language.Nl).flatMap { language ->
                    listOf(false, true).map { arrayOf<Any>(board, language, it) }
                }
            }
    }
}
