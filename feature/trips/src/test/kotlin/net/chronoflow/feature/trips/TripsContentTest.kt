package net.chronoflow.feature.trips

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.CopyCatalog
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.sync.refresh.RefreshFeedbackState
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.shell.LocalTabReselect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TripsContentTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    private fun board(): TripsUiState.Content {
        var state: TripsUiState.Content? = null
        runTest(timeout = 60.seconds) {
            state =
                TripsBoardFixture.content(
                    CopyLineFormatter(CopyCatalog.load(StandardTestDispatcher(testScheduler))),
                    Language.En,
                )
        }
        return checkNotNull(state)
    }

    private fun host(content: @Composable () -> Unit) {
        compose.setContent {
            ProfileHost(
                DeviceProfiles.pixel8,
            ) { ChronoFlowTheme(reduceMotion = true, content = content) }
        }
    }

    private fun host(
        state: TripsUiState,
        open: (String) -> Unit = {},
        create: () -> Unit = {},
        refresh: () -> Unit = {},
    ) {
        host { TripsContent(state, RefreshFeedbackState(), Language.En, refresh, {}, open, create) }
    }

    @Test fun contentTagsAndCardSemantics() {
        host(board())
        compose.onNodeWithTag("trips.screen").assertExists()
        compose.onNodeWithTag("trips.create").assertExists()
        compose.onNodeWithTag("trips.section.active").assertExists()
        val card =
            compose
                .onNodeWithTag(
                    "trip.active",
                ).assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
                .fetchSemanticsNode()
        assertTrue(card.config.isMergingSemanticsOfDescendants)
        assertTrue(card.children.isEmpty())
        compose.onNodeWithTag("trips.list").performScrollToNode(hasTestTag("trips.section.upcoming"))
        compose.onNodeWithTag("trips.section.upcoming").assertExists()
        compose.onNodeWithTag("trips.list").performScrollToNode(hasTestTag("trips.section.completed"))
        compose.onNodeWithTag("trips.section.completed").assertExists()
    }

    @Test fun cardAndCreateActions() {
        var opened = ""
        var created = 0
        host(board(), open = { opened = it }, create = { created++ })
        compose.onNodeWithTag("trip.active").performClick()
        assertEquals("active", opened)
        compose.onNodeWithTag("trips.create").performClick()
        assertEquals(1, created)
    }

    @Test fun emptyTagsAndCreateAction() {
        var created = 0
        host(TripsUiState.Empty, create = { created++ })
        compose.onNodeWithTag("trips.empty").assertExists()
        compose.onNodeWithTag("trips.create-empty").performClick()
        assertEquals(1, created)
    }

    @Test fun loadingTag() {
        host(TripsUiState.Loading)
        compose.onNodeWithTag("trips.loading").assertExists()
    }

    private fun pull(state: TripsUiState) {
        var refreshes = 0
        host(state, refresh = { refreshes++ })
        compose.onNodeWithTag("trips.list").performTouchInput { swipeDown() }
        compose.waitForIdle()
        assertEquals(1, refreshes)
    }

    @Test fun pullWorksOnContent() = pull(board())

    @Test fun pullWorksOnEmpty() = pull(TripsUiState.Empty)

    @Test fun pullWorksOnLoading() = pull(TripsUiState.Loading)

    @Test fun reselectScrollsOnceAndTheSameSignalDoesNotScrollAgain() {
        val original = board().sections.first()
        val content =
            TripsUiState.Content(
                listOf(
                    original.copy(
                        trips =
                            List(30) {
                                original.trips.single().copy(id = "t$it")
                            },
                    ),
                ),
            )
        val list = LazyListState()
        val signal = mutableIntStateOf(0)
        val language = mutableStateOf<Language>(Language.En)
        host {
            CompositionLocalProvider(LocalTabReselect provides signal.intValue) {
                TripsContent(content, RefreshFeedbackState(), language.value, {}, {}, {}, {}, listState = list)
            }
        }
        compose.onNodeWithTag("trips.list").performScrollToIndex(20)
        compose.runOnIdle { signal.intValue++ }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(0, list.firstVisibleItemIndex) }
        compose.onNodeWithTag("trips.list").performScrollToIndex(20)
        compose.runOnIdle { language.value = Language.Nl }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(20, list.firstVisibleItemIndex) }
    }
}
