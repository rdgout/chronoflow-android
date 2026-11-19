package net.chronoflow.feature.today

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.ui.clock.MinuteClock
import net.chronoflow.feature.today.ui.TodayScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
class TodayWideLayoutTest(
    private val profile: DeviceProfile,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile)).around(compose)

    @Test fun heroAndRowsAreCappedAndCentredInsideSafeInsets() {
        lateinit var formatter: CopyLineFormatter
        lateinit var state: TodayUiState.Active
        runTest(timeout = 60.seconds) {
            formatter = todayFormatter(StandardTestDispatcher(testScheduler))
            state = TodayReducer.reduce(todayInput(formatter)) as TodayUiState.Active
        }
        val clock = MinuteClock(mutableStateOf(todayNow))
        compose.setContent {
            ProfileHost(profile) {
                ChronoFlowTheme(reduceMotion = true) {
                    TodayScreen(state, clock, Language.En, formatter, TimeFormat.Value24, noTodayActions, {})
                }
            }
        }
        assertWidth("today.trip")
        Guards.assertTextFits(compose, profile, scrollIntoView = false)
        compose.onNodeWithTag("today.screen").performScrollToNode(hasTestTag("today.step.actionable.row0"))
        assertWidth("today.step.actionable.row0")
        Guards.assertTextFits(compose, profile, scrollIntoView = false)
    }

    private fun assertWidth(tag: String) {
        val r = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val density = profile.density
        val contentWidth = profile.widthDp - profile.insets.left - profile.insets.right
        val expected = minOf(640f, contentWidth - 32)
        assertEquals(expected, r.width / density, 1f)
        val left = r.left / density - profile.insets.left
        val right = profile.widthDp - profile.insets.right - r.right / density
        assertEquals(left, right, 1f)
        assertTrue(r.width > 0)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): Collection<Array<Any>> =
            listOf(
                DeviceProfiles.foldInner,
                DeviceProfiles.landscapeSideNav,
                DeviceProfiles.pixel8,
                DeviceProfiles.small.copy(name = "minimumWidth", widthDp = 280),
            ).map {
                arrayOf<Any>(it)
            }
    }
}
