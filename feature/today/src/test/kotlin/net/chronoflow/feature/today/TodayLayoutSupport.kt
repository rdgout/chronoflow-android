package net.chronoflow.feature.today

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.ui.clock.MinuteClock
import net.chronoflow.feature.today.ui.TodayScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.rules.RuleChain
import kotlin.time.Duration.Companion.seconds

abstract class TodayLayoutSupport(
    profile: DeviceProfile = DeviceProfiles.small,
) {
    protected val profile = profile.copy(fontScale = 1.3f)
    protected val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(this.profile, AppLanguage.Nl)).around(compose)

    protected fun checkTodayLayout(stateName: String) {
        lateinit var formatter: CopyLineFormatter
        lateinit var state: TodayUiState
        runTest(timeout = 60.seconds) {
            formatter = todayFormatter(StandardTestDispatcher(testScheduler))
            state = TodayBoardFixture.state(stateName, formatter, Language.Nl)
        }
        val clock = MinuteClock(mutableStateOf(todayNow))
        compose.setContent {
            ProfileHost(profile) {
                ChronoFlowTheme(reduceMotion = true) {
                    TodayScreen(state, clock, Language.Nl, formatter, TimeFormat.Value24, noTodayActions, {})
                }
            }
        }
        Guards.assertTextFits(compose, profile, scrollIntoView = false)
        when (val value = state) {
            is TodayUiState.Active -> {
                assertGutters("today.trip")
                val row = value.panel?.let { it.upcoming.lastOrNull() ?: it.current.lastOrNull() }
                if (row != null) {
                    val tag = "today.step.${if (row.actionable) "actionable." else ""}${row.blockId}"
                    compose.onNodeWithTag("today.screen").performScrollToNode(hasTestTag(tag))
                    assertGutters(tag)
                }
            }

            is TodayUiState.Future -> {
                assertGutters("today.trip")
            }

            is TodayUiState.Completed -> {
                assertGutters("today.completed")
            }

            TodayUiState.Empty -> {
                assertGutters("today.empty")
            }

            TodayUiState.Loading -> {
                assertGutters("plan.loading")
            }
        }
        Guards.assertTextFits(compose, profile, scrollIntoView = false)
    }

    private fun assertGutters(tag: String) {
        val bounds = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val left = (profile.insets.left + 16 - 1) * profile.density
        val right = (profile.widthDp - profile.insets.right - 16 + 1) * profile.density
        assertTrue("$tag crosses the left gutter", bounds.left >= left)
        assertTrue("$tag crosses the right gutter", bounds.right <= right)
    }
}
