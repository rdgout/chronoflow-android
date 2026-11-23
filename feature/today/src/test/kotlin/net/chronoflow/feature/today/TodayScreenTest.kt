package net.chronoflow.feature.today

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.clock.MinuteClock
import net.chronoflow.feature.today.ui.TodayScreen
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

internal val noTodayActions = TodayActions({}, {}, {}, {}, {})

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class TodayScreenTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var formatter: CopyLineFormatter

    @Before fun prepare() =
        runTest(timeout = 60.seconds) {
            formatter =
                todayFormatter(StandardTestDispatcher(testScheduler))
        }

    private fun host(
        state: TodayUiState,
        actions: TodayActions = noTodayActions,
    ) {
        val clock = MinuteClock(mutableStateOf(todayNow))
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(reduceMotion = true) {
                    TodayScreen(state, clock, Language.En, formatter, TimeFormat.Value24, actions, {})
                }
            }
        }
        compose.onNodeWithTag("today.appbar").assertExists()
    }

    @Test fun loadingHasOneAccessibleSkeleton() {
        host(TodayUiState.Loading)
        compose
            .onNodeWithTag(
                "plan.loading",
            ).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
    }

    @Test fun emptyHasCreateControl() {
        var creates = 0
        host(TodayUiState.Empty, noTodayActions.copy(create = { creates++ }))
        compose.onNodeWithTag("today.empty").assertExists()
        compose.onNodeWithTag("today.create").performClick()
        assertEquals(1, creates)
    }

    @Test fun completedHasPastTripsControl() {
        host(TodayUiState.Completed("AMS → JFK", "2026-11-07"))
        compose.onNodeWithTag("today.completed").assertExists()
        compose.onNodeWithTag("today.past").assertExists()
    }

    @Test fun futureHasHeroAndNote() {
        val row = (TodayReducer.reduce(todayInput(formatter)) as TodayUiState.Active).panel!!.current.single()
        val next = NextStepUi(row.blockId, row.type, row.title, todayNow, row.timeText)
        val state = TodayUiState.Future(todayTrip(), next, 3, todayNow.toString(), "UTC")
        host(state)
        compose.onNodeWithTag("today.trip").assertExists()
        compose.onNodeWithTag("today.future").assertExists()
    }

    @Test fun activeHasProgressAndActionableRows() {
        val reduced = TodayReducer.reduce(todayInput(formatter)) as TodayUiState.Active
        val panel = reduced.panel!!.copy(progress = ProgressUi(1, 7, 0))
        host(reduced.copy(panel = panel))
        compose.onNodeWithTag("today.trip").assertExists()
        compose.onNodeWithTag("today.progress").assertExists()
        compose
            .onNodeWithTag(
                "today.step.actionable.row0",
            ).assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test fun markedRowKeepsOneAccessibleButton() {
        val reduced = TodayReducer.reduce(todayInput(formatter)) as TodayUiState.Active
        val panel = reduced.panel!!
        host(reduced.copy(panel = panel.copy(current = panel.current.map { it.copy(mark = CheckStatus.Done) })))
        compose
            .onNodeWithTag("today.step.actionable.row0")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test fun hiddenNoticeIsOneSettingsControl() {
        val reduced = TodayReducer.reduce(todayInput(formatter)) as TodayUiState.Active
        var settings = 0
        host(
            reduced.copy(panel = reduced.panel!!.copy(hidden = HiddenNotice.All)),
            noTodayActions.copy(settings = {
                settings++
            }),
        )
        compose.onNodeWithTag("today.screen").performScrollToNode(hasTestTag("today.hidden"))
        compose.onNodeWithTag("today.hidden").performClick()
        assertEquals(1, settings)
    }
}
