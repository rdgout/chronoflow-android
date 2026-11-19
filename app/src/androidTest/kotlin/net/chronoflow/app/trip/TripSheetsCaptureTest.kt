package net.chronoflow.app.trip

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.chronoflow.app.ChronoFlowApp
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.tiers.Capture
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.Instant

@Capture
@OptIn(ExperimentalTestApi::class)
@RunWith(Parameterized::class)
class TripSheetsCaptureTest(
    private val state: String,
) : ScreenTest() {
    @Test fun captureSeededSheet() {
        val now = Instant.now()
        val block =
            runBlocking(Dispatchers.IO) {
                MockAppSeed
                    .make(now)
                    .accounts.values
                    .first()
                    .details
                    .getValue(TRIP)
                    .plan.blocks
                    .first { it.type == BlockType.AvoidLight && it.actionable && it.checkKey != null }
            }
        val clock = checkNotNull(planDate(block.startUtc)).plusSeconds(60)
        val sheet =
            when (state) {
                "running", "done" -> "step:${block.id}"
                "priorities" -> "priorities"
                else -> "legend"
            }
        val values =
            mutableMapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_PLAN_TEST_ROUTE" to "trip:$TRIP",
                "CF_PLAN_TEST_FREEZE_CLOCK" to "1",
                "cf.freezeAt" to clock.toString(),
                "CF_MOCK_DELAY" to "0",
                "cf.settings.language" to "en",
                "cf.settings.theme" to "light",
                "cf.reduceMotion" to "1",
            )
        if (state == "actions") {
            values["CF_WIZARD_BOARD_CAPTURE"] = "TripActions"
        } else if (state != "noPlan") {
            values["CF_PLAN_TEST_SHEET"] = sheet
        }
        launch(values)
        if (state == "noPlan") {
            compose.waitUntilExactlyOneExists(hasTestTag("trip.timeline.ready"), 5_000)
            val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as ChronoFlowApp
            runBlocking(Dispatchers.IO) {
                val session = checkNotNull(app.graph.sessionGraphs.current.value)
                session.sync.setEnabled(false)
                val snapshot = session.trips.detailValues(TRIP).first()
                val detail = checkNotNull(PlanDecoder(Dispatchers.IO).detail(snapshot))
                val noPlan =
                    detail.copy(
                        plan = detail.plan.copy(status = PlanStatus.NoPlan, days = emptyList(), blocks = emptyList()),
                    )
                val record = checkNotNull(snapshot.plan)
                session.trips.store(
                    PlanVersionRecord(
                        TRIP,
                        record.inputHash,
                        ApiJson.response.encodeToString(TripDetail.serializer(), noPlan).encodeToByteArray(),
                        record.receivedAt,
                        "capture-no-plan",
                    ),
                    emptyList(),
                )
            }
            compose.waitUntilExactlyOneExists(hasTestTag("trip.noPlan"), 5_000)
            compose.onNodeWithTag("trip.actions.open").performClick()
        }
        val tag =
            when (state) {
                "running", "done" -> "step.sheet"
                "actions", "noPlan" -> "trip.actions"
                "priorities" -> "legend.priorities"
                else -> "legend.sheet"
            }
        compose.waitUntilExactlyOneExists(hasTestTag(tag), 5_000)
        if (state != "actions" && state != "noPlan") {
            val expand =
                compose
                    .onNodeWithTag("trip.sheet")
                    .fetchSemanticsNode()
                    .config
                    .getOrNull(SemanticsActions.Expand)
                    ?.action
            if (expand != null) compose.runOnIdle { expand() }
        }
        if (state == "done") {
            compose.onNodeWithTag("step.done").performScrollTo().performClick()
            compose.waitUntilExactlyOneExists(hasTestTag("step.undo"), 5_000)
        }
        compose.waitForIdle()
        val name =
            when (state) {
                "running", "done" -> "ItemDetail_${state}_device_en_light"
                "actions" -> "TripActions_device_en_light"
                "noPlan" -> "TripActionsNoPlan_device_en_light"
                "priorities" -> "PlanLegend_priorities_device_en_light"
                else -> "PlanLegend_device_en_light"
            }
        BoardCapture.capture(compose, name)
    }

    companion object {
        private const val TRIP = "0199a3c0-0000-7000-8000-000000000001"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            listOf("running", "done", "legend", "priorities", "actions", "noPlan").map { arrayOf(it) }
    }
}
