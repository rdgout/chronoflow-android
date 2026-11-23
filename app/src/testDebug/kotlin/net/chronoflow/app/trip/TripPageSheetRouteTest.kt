package net.chronoflow.app.trip

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.chronoflow.app.shell.AppShellScaffold
import net.chronoflow.app.shell.ShellViewModel
import net.chronoflow.app.shell.rememberShellViewModel
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.model.store.TripPlanSnapshot
import net.chronoflow.core.model.store.TripRecord
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.TripSheet
import net.chronoflow.core.navigation.shell.ShellTab
import net.chronoflow.core.plan.CopyCatalogProvider
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.FixedPlanClock
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.plan.PlanPreparer
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.plan.trippage.PlanPreferences
import net.chronoflow.core.sync.PlanRetention
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.FakeTextMetrics
import net.chronoflow.core.testing.LargeTimelineFixtures
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.trip.page.DefaultTripPlanPipeline
import net.chronoflow.feature.trip.page.TripPageArgs
import net.chronoflow.feature.trip.page.TripPageSource
import net.chronoflow.feature.trip.page.TripPageViewModel
import net.chronoflow.feature.trip.timeline.TimelineTracer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class, qualifiers = "w411dp-h914dp-420dpi")
class TripPageSheetRouteTest {
    @get:Rule val compose = createComposeRule()
    private val detail = LargeTimelineFixtures.fixture("S1").detail
    private val route = Route.TripPage("fixture-S1")
    private lateinit var holder: ShellViewModel
    private lateinit var current: TripPageViewModel
    private lateinit var list: LazyListState
    private var scroll: () -> Unit = {}
    private val source =
        object : TripPageSource {
            private val snapshot =
                MutableStateFlow(
                    TripPlanSnapshot(
                        TripRecord(
                            route.tripId,
                            "{}",
                            currentInputHash = "fixture-S1",
                            keepOffline = true,
                            departureUtc = detail.trip.departureUtc,
                        ),
                        PlanVersionRecord(route.tripId, "fixture-S1", LargeTimelineFixtures.detailBytes("S1"), "now"),
                    ),
                )

            override fun snapshots(tripId: String) = snapshot

            override fun preferences() = MutableStateFlow(PlanPreferences())

            override fun hold(tripId: String) = PlanRetention().hold(tripId)

            override suspend fun refresh() = null
        }

    private fun newModel(): TripPageViewModel {
        val dispatcher = Dispatchers.Unconfined
        val catalogs = CopyCatalogProvider(dispatcher)
        val decoder = PlanDecoder(dispatcher)
        return TripPageViewModel(
            TripPageArgs(route.tripId),
            source,
            FixedPlanClock(
                checkNotNull(
                    planDate(
                        detail.plan.days
                            .first()
                            .startUtc,
                    ),
                ),
            ),
            DefaultTripPlanPipeline(
                decoder,
                PlanPreparer(decoder, catalogs, dispatcher),
                catalogs,
                dispatcher,
                TimelineTracer.None,
            ) {},
            SavedStateHandle(),
            Language.En,
            dispatcher,
            dispatcher,
            { FakeTextMetrics() },
            formatter = { CopyLineFormatter(catalogs.catalog()) },
            tracer = TimelineTracer.None,
        )
    }

    @Test fun sheetRouteChangesKeepTheViewModelAndScroll() {
        val factory = viewModelFactory { initializer { newModel() } }
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(reduceMotion = true) {
                    val shell = rememberShellViewModel(route)
                    SideEffect { holder = shell }
                    AppShellScaffold(shell, {}, {}, {}, {}, page = { key ->
                        val page = key as Route.TripPage
                        val vm: TripPageViewModel = viewModel(factory = factory)
                        TrackTripPageSheet(vm, page) { shell.replaceRoute(page, it) }
                        val state = rememberLazyListState()
                        val scope = rememberCoroutineScope()
                        SideEffect {
                            current = vm
                            list = state
                            scroll = { scope.launch { state.scrollToItem(6) } }
                        }
                        LazyColumn(state = state) {
                            items(20, key = { it }, contentType = { "row" }) {
                                Box(Modifier.height(48.dp).testTag("row.$it"))
                            }
                        }
                    }, overlay = {})
                }
            }
        }
        compose.runOnIdle { scroll() }
        compose.waitForIdle()
        val before = current
        val beforeList = list
        val beforeIndex = list.firstVisibleItemIndex
        val blockId =
            detail.plan.blocks
                .first()
                .id
        compose.runOnIdle { current.openStep(blockId) }
        compose.waitForIdle()
        assertEquals(
            route.copy(sheet = TripSheet.Step(blockId)),
            holder.state.stacks
                .getValue(ShellTab.Trips)
                .last(),
        )
        assertSame(before, current)
        assertSame(beforeList, list)
        assertEquals(beforeIndex, list.firstVisibleItemIndex)
        compose.runOnIdle { current.dismissSheet() }
        compose.waitForIdle()
        assertEquals(
            route,
            holder.state.stacks
                .getValue(ShellTab.Trips)
                .last(),
        )
        assertSame(before, current)
    }
}
