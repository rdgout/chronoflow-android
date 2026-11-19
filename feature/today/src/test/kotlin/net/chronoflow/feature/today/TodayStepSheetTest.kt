@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.today

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.FakePlanClock
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.tiers.Logic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class TodayStepSheetTest {
    @get:Rule val compose = createComposeRule()
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val source = FakeTodaySource()
    private val model =
        TodayViewModel(source, FakePlanClock(todayNow), { todayFormatter(dispatcher) }, dispatcher, dispatcher)
    private val store = ViewModelStore().apply { put("today", model) }

    @After fun close() {
        store.clear()
        scheduler.runCurrent()
    }

    private fun host() {
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(reduceMotion = true) {
                    TodayRoute(model, noTodayActions)
                }
            }
        }
        waitFor { compose.onNodeWithTag("today.step.actionable.row0").isDisplayed() }
    }

    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(5_000) {
            scheduler.runCurrent()
            condition()
        }
        compose.waitForIdle()
    }

    @Test fun doneAndUndoUseTheSelectionAndKeepTheRowInPlace() {
        host()
        val before = (model.uiState.value as TodayUiState.Active).panel!!.current.map { it.blockId }
        compose.onNodeWithTag("today.step.actionable.row0").performClick()
        waitFor { compose.onNodeWithTag("step.done").isDisplayed() }
        compose.onNodeWithTag("step.done").performScrollTo().performClick()
        waitFor { source.writes.size == 1 }
        val write = source.writes.single()
        assertEquals("hero", write.tripId)
        assertEquals("row0", write.blockId)
        assertEquals(CheckStatus.Done, write.status)
        val detail = source.details.getValue("hero").value!!
        val block = detail.plan.blocks.first()
        compose.runOnIdle {
            source.details.getValue("hero").value =
                detail.copy(
                    checkState =
                        detail.checkState.copy(
                            marks = listOf(CheckMark(block.checkKey!!, CheckStatus.Done, block.startUtc)),
                        ),
                )
        }
        waitFor { compose.onNodeWithTag("step.undo").isDisplayed() }
        assertEquals(before, (model.uiState.value as TodayUiState.Active).panel!!.current.map { it.blockId })
        compose
            .onNodeWithTag(
                "today.step.actionable.row0",
            ).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
        compose.onNodeWithTag("step.undo").performScrollTo().performClick()
        waitFor { source.writes.size == 2 }
        assertEquals(null, source.writes.last().status)
    }
}
