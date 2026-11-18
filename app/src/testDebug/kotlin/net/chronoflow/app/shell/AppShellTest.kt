package net.chronoflow.app.shell

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.WizardMode
import net.chronoflow.core.navigation.shell.ShellTab
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.shell.LocalTabReselect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class AppShellTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var holder: ShellViewModel
    private lateinit var activity: ComponentActivity
    private val lists = mutableMapOf<ShellTab, LazyListState>()
    private val modelIds = mutableMapOf<ShellTab, Int>()
    private var pageId = -1
    private var contentBottom = -1

    private fun host(
        root: Route = Route.Trips,
        profile: DeviceProfile = DeviceProfiles.pixel8,
    ) {
        CounterViewModel.cleared.clear()
        compose.setContent {
            ProfileHost(profile) {
                ChronoFlowTheme(reduceMotion = true) {
                    val currentActivity = LocalActivity.current as ComponentActivity
                    val currentHolder = rememberShellViewModel(root)
                    SideEffect {
                        holder = currentHolder
                        activity = currentActivity
                    }
                    AppShellScaffold(
                        currentHolder,
                        today = { TabRoot(ShellTab.Today) },
                        trips = { TabRoot(ShellTab.Trips) },
                        settings = { TabRoot(ShellTab.Settings) },
                        page = {
                            val model = viewModel<CounterViewModel>()
                            SideEffect { pageId = model.id }
                            Box(Modifier.fillMaxSize().testTag("route.tripPage"))
                        },
                        overlay = { Box(Modifier.fillMaxSize().testTag("route.wizard")) },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    @Composable
    private fun TabRoot(
        tab: ShellTab,
        model: CounterViewModel = viewModel(),
    ) {
        val list = rememberLazyListState()
        val bottom = LocalSafeInsets.current.getBottom(LocalDensity.current)
        SideEffect {
            lists[tab] = list
            modelIds[tab] = model.id
            contentBottom = bottom
        }
        val signal = LocalTabReselect.current
        var handled by rememberSaveable { mutableIntStateOf(signal) }
        LaunchedEffect(signal) {
            if (signal != handled) {
                handled = signal
                list.scrollToItem(0)
            }
        }
        val name = tab.name.lowercase()
        Column(Modifier.fillMaxSize().testTag("route.$name")) {
            Button({ holder.openTrip("t1") }, Modifier.testTag("test.open.$name")) {
                Text(stringResource(R.string.screens_new_trip))
            }
            LazyColumn(Modifier.weight(1f).testTag("test.list.$name"), state = list) {
                items(50, key = { it }, contentType = { "item" }) { index ->
                    Box(Modifier.fillMaxWidth().height(64.dp).testTag("test.item.$name.$index"))
                }
            }
        }
    }

    @Test fun barBelow600() {
        host()
        compose.onNodeWithTag("cf.nav.bar").assertExists()
        compose.onNodeWithTag("cf.nav.rail").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w841dp-h701dp-420dpi")
    fun railFrom600() {
        host(profile = DeviceProfiles.foldInner)
        compose.onNodeWithTag("cf.nav.rail").assertExists()
        compose.onNodeWithTag("cf.nav.bar").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w914dp-h411dp-land-420dpi")
    fun railInLandscape() {
        host(profile = DeviceProfiles.landscapeSideNav)
        compose.onNodeWithTag("cf.nav.rail").assertExists()
    }

    @Test fun barNeverHidesOnScroll() {
        host()
        val before = compose.onNodeWithTag("tab.trips").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("test.list.trips").performScrollToIndex(49)
        compose.onNodeWithTag("tab.trips").assertIsDisplayed()
        assertEquals(before, compose.onNodeWithTag("tab.trips").fetchSemanticsNode().boundsInRoot)
    }

    @Test fun backFromTripsRootSelectsTodayThenFinishes() {
        host()
        compose.runOnIdle { activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("route.today").assertExists()
        compose.runOnIdle { activity.onBackPressedDispatcher.onBackPressed() }
        assertTrue(activity.isFinishing)
    }

    @Test fun backPopsPushedPageFirst() {
        host()
        compose.onNodeWithTag("test.open.trips").performClick()
        compose.onNodeWithTag("route.tripPage").assertExists()
        compose.runOnIdle { activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("route.trips").assertExists()
    }

    @Test fun predictiveBackCancelKeepsThePage() {
        host()
        compose.onNodeWithTag("test.open.trips").performClick()
        val before = compose.onNodeWithTag("route.tripPage").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle {
            activity.onBackPressedDispatcher.dispatchOnBackStarted(
                BackEventCompat(0f, 0f, 0f, BackEventCompat.EDGE_LEFT),
            )
        }
        compose.waitForIdle()
        compose.runOnIdle {
            activity.onBackPressedDispatcher.dispatchOnBackProgressed(
                BackEventCompat(100f, 0f, 0.5f, BackEventCompat.EDGE_LEFT),
            )
        }
        compose.mainClock.advanceTimeBy(100)
        val progressed = compose.onNodeWithTag("route.tripPage").fetchSemanticsNode().boundsInRoot
        assertNotEquals(before, progressed)
        compose.runOnIdle { activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
        compose.onNodeWithTag("route.tripPage").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(
                2,
                holder.state.stacks
                    .getValue(ShellTab.Trips)
                    .size,
            )
        }
    }

    @Test fun reselectPopsToRootThenScrollsToTop() {
        host()
        compose.onNodeWithTag("test.list.trips").performScrollToIndex(30)
        compose.onNodeWithTag("test.open.trips").performClick()
        compose.onNodeWithTag("route.tripPage").assertExists()
        compose.onNodeWithTag("tab.trips").performClick()
        compose.onNodeWithTag("route.trips").assertExists()
        compose.runOnIdle { assertEquals(30, lists.getValue(ShellTab.Trips).firstVisibleItemIndex) }
        compose.onNodeWithTag("tab.trips").performClick()
        compose.runOnIdle { assertEquals(0, lists.getValue(ShellTab.Trips).firstVisibleItemIndex) }
    }

    @Test fun tabSwitchKeepsScrollAndViewModel() {
        host()
        compose.onNodeWithTag("test.list.trips").performScrollToIndex(30)
        val id = modelIds.getValue(ShellTab.Trips)
        compose.onNodeWithTag("tab.settings").performClick()
        compose.onNodeWithTag("route.settings").assertExists()
        compose.onNodeWithTag("tab.trips").performClick()
        compose.onNodeWithTag("route.trips").assertExists()
        compose.runOnIdle {
            assertEquals(30, lists.getValue(ShellTab.Trips).firstVisibleItemIndex)
            assertEquals(id, modelIds.getValue(ShellTab.Trips))
        }
    }

    @Test fun popClearsThePagesViewModel() {
        host()
        compose.onNodeWithTag("test.open.trips").performClick()
        compose.onNodeWithTag("route.tripPage").assertExists()
        val id = pageId
        compose.runOnIdle { activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("route.trips").assertExists()
        compose.runOnIdle { assertTrue(id in CounterViewModel.cleared) }
    }

    @Test fun wizardOverlayCoversTabsAndBackCloses() {
        host()
        compose.runOnIdle { holder.push(Route.Wizard(WizardMode.Create)) }
        compose.onNodeWithTag("route.wizard").assertIsDisplayed()
        compose.onNodeWithTag("tab.trips").assertDoesNotExist()
        compose.runOnIdle { activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("route.trips").assertIsDisplayed()
    }

    @Test fun insetsDifferForBarAndRail() {
        host()
        compose.runOnIdle { assertEquals(0, contentBottom) }
    }

    @Test
    @Config(qualifiers = "w841dp-h701dp-420dpi")
    fun railContentKeepsTheBottomInset() {
        host(profile = DeviceProfiles.foldInner)
        compose.runOnIdle { assertEquals(63, contentBottom) }
    }

    class CounterViewModel : ViewModel() {
        val id = next.incrementAndGet()

        override fun onCleared() {
            cleared += id
        }

        companion object {
            val next = AtomicInteger()
            val cleared = mutableSetOf<Int>()
        }
    }
}
