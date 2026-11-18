@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.shell

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import net.chronoflow.app.RootContent
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.store.TripPlanSnapshot
import net.chronoflow.core.model.store.TripRecord
import net.chronoflow.core.navigation.RootState
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.debugName
import net.chronoflow.core.navigation.shell.ShellTab
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.sync.SyncResult
import net.chronoflow.core.sync.refresh.RefreshDebugProbe
import net.chronoflow.core.testing.FakePlanClock
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.trips.TripsDependencies
import net.chronoflow.feature.trips.TripsPreferences
import net.chronoflow.feature.trips.TripsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShellRecreationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun recreationKeepsTheSelectedTabPushedPageAndTripsViewModel() {
        val fixture = ShellRecreationFixture()
        ShellRecreationActivity.fixture = fixture
        val controller = Robolectric.buildActivity(ShellRecreationActivity::class.java).setup()

        fun flush() {
            compose.runOnIdle { fixture.scheduler.runCurrent() }
            compose.waitForIdle()
        }
        try {
            flush()
            val model = checkNotNull(fixture.model)
            val holder = checkNotNull(fixture.holder)
            compose.runOnIdle { model.refresh() }
            flush()
            assertTrue(model.refreshState.value.refreshing)
            compose.onNodeWithTag("tab.settings").performClick()
            compose.runOnIdle { holder.openTrip("t1") }
            compose.onNodeWithTag("route.tripPage").assertExists()
            controller.recreate()
            flush()
            assertSame(holder, fixture.holder)
            assertEquals(ShellTab.Settings, fixture.holder!!.state.selected)
            assertEquals(
                Route.TripPage("t1"),
                fixture.holder!!
                    .state.stacks
                    .getValue(ShellTab.Settings)
                    .last(),
            )
            compose.onNodeWithTag("tab.settings").assertIsSelected()
            compose.onNodeWithTag("route.tripPage").assertExists()
            compose.onNodeWithTag("tab.trips").performClick()
            flush()
            assertSame(model, fixture.model)
            assertTrue(model.refreshState.value.refreshing)
            assertFalse(fixture.refreshStopped.isCompleted)
            compose.runOnIdle { fixture.root.value = RootState(Route.Welcome, 2) }
            flush()
            assertTrue(fixture.refreshStopped.isCompleted)
        } finally {
            controller.pause().stop().destroy()
            fixture.scheduler.runCurrent()
        }
    }
}

internal class ShellRecreationFixture : TripsDependencies {
    val scheduler = TestCoroutineScheduler()
    override val worker = StandardTestDispatcher(scheduler)
    override val clock = FakePlanClock(Instant.EPOCH)
    override val decoder = PlanDecoder(worker)
    override val refreshProbe = RefreshDebugProbe.None
    val root = mutableStateOf(RootState(Route.Trips, 1))
    var holder: ShellHolder? = null
    var model: TripsViewModel? = null
    val refreshStopped = CompletableDeferred<Unit>()
    private val refreshResult = CompletableDeferred<SyncResult?>()
    val factory = viewModelFactory { initializer { TripsViewModel(this@ShellRecreationFixture) } }

    override fun tripRecords() = emptyFlow<List<TripRecord>>()

    override fun planSnapshots(tripId: String) = emptyFlow<TripPlanSnapshot>()

    override fun synced() = emptyFlow<Boolean>()

    override fun preferences() = emptyFlow<TripsPreferences>()

    override suspend fun formatter(): Nothing = error("No state collector in this lifecycle fixture")

    override suspend fun refreshNow(): SyncResult? =
        try {
            refreshResult.await()
        } finally {
            refreshStopped.complete(Unit)
        }
}

internal class ShellRecreationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChronoFlowTheme(reduceMotion = true) {
                CompositionLocalProvider(LocalSafeInsets provides WindowInsets(0)) {
                    RootContent(fixture.root.value, {}) { root ->
                        if (root == Route.Trips) {
                            val holder = rememberShellHolder(root)
                            SideEffect { fixture.holder = holder }
                            AppShellScaffold(
                                holder,
                                today = {},
                                trips = {
                                    val model: TripsViewModel = viewModel(factory = fixture.factory)
                                    SideEffect { fixture.model = model }
                                    Box(Modifier.fillMaxSize().testTag("trips.screen"))
                                },
                                settings = {},
                                page = { Box(Modifier.fillMaxSize().testTag("route.${it.debugName()}")) },
                                overlay = {},
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        lateinit var fixture: ShellRecreationFixture
    }
}
