package net.chronoflow.feature.trip.list

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.plan.trippage.TripPageState
import net.chronoflow.core.testing.LargeTimelineFixtures
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.trip.page.PageComponentTest
import net.chronoflow.feature.trip.page.TripPageTestRig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ViewToggleTest : PageComponentTest() {
    private var rig: TripPageTestRig? = null

    @After fun close() {
        rig?.close()
    }

    private fun start(
        timeline: Boolean = true,
        state: TripPageState? = null,
    ): TripPageTestRig {
        val detail = LargeTimelineFixtures.fixture("S3").detail
        val host =
            TripPageTestRig(
                "S3",
                checkNotNull(planDate(detail.plan.days[1].startUtc)).plusSeconds(7200),
                initialTimeline = timeline,
            )
        rig = host
        if (timeline && state == null) host.prewarm(compose.density)
        host { host.Content(state) }
        compose.waitForIdle()
        return host
    }

    private fun headerAtTop(
        host: TripPageTestRig,
        list: Boolean,
    ) {
        val day = host.model.prepared.days[1]
        assertEquals(
            if (list) host.model.listIndex.headerIndex(day.id) else host.model.index.headerIndex(day.id),
            host.listState!!.firstVisibleItemIndex,
        )
        assertEquals(0, host.listState!!.firstVisibleItemScrollOffset)
    }

    @Test fun toggleShowsListRowsAndKeepsDay() {
        val host = start()
        val day = host.model.listRows[1]
        compose.onNodeWithTag("trip.viewToggle").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("trip.block.${day.rows.first { !it.zone }.blockId}").assertExists()
        compose.onNodeWithTag("trip.day.${day.dayId}").assertDoesNotExist()
        headerAtTop(host, true)
        compose.onNodeWithTag("trip.viewToggle").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("trip.day.${day.dayId}").assertExists()
        headerAtTop(host, false)
    }

    @Test fun compactHeaderListVariant() {
        val host = start(timeline = false)
        val day = host.model.listRows[1]
        compose.runOnIdle { host.listState!!.requestScrollToItem(host.model.listIndex.rowIndex(day.rows[1].key), 40) }
        compose.waitForIdle()
        compose.onNodeWithTag("trip.compactHeader.day", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("trip.compactHeader.zone", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("trip.lanes").assertDoesNotExist()
    }

    private fun disabled(state: TripPageState) {
        start(state = state)
        compose.onNodeWithTag("trip.viewToggle").assertIsNotEnabled()
    }

    private fun absent(state: TripPageState) {
        start(state = state)
        compose.onNodeWithTag("trip.viewToggle").assertDoesNotExist()
    }

    @Test fun toggleAbsentOrDisabledByState_loading() = disabled(TripPageState.Loading)

    @Test fun toggleAbsentOrDisabledByState_remoteLoading() = disabled(TripPageState.RemoteLoading)

    @Test fun toggleAbsentOrDisabledByState_noPlan() = absent(TripPageState.NoPlan)

    @Test fun toggleAbsentOrDisabledByState_offline() = absent(TripPageState.RemoteOffline)

    @Test fun toggleAbsentOrDisabledByState_notFound() = absent(TripPageState.NotFound)

    @Test fun toggleAbsentOrDisabledByState_server() = absent(TripPageState.Server)

    @Test fun toggleAbsentOrDisabledByState_ready() {
        start()
        compose.onNodeWithTag("trip.viewToggle").assertIsEnabled()
    }
}
