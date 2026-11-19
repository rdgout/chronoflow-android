package net.chronoflow.core.ui.plan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.sync.SyncRefusal
import net.chronoflow.core.sync.SyncRefusalNotice
import net.chronoflow.core.sync.SyncResult
import net.chronoflow.core.sync.SyncState
import net.chronoflow.core.sync.refusal.RefusedChangeModel
import net.chronoflow.core.testing.compose.RecordingHaptics
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class StepSheetContentTest {
    @get:Rule val compose = createComposeRule()
    private val state = mutableStateOf(sheetState("upcoming"))
    private val haptics = RecordingHaptics()
    private val calls = mutableListOf<CheckStatus?>()
    private var today = false

    private fun content(busy: Boolean = false) {
        compose.setContent {
            ChronoFlowTheme {
                CompositionLocalProvider(LocalHaptics provides haptics) {
                    StepSheetContent(state.value, busy, calls::add, { today = true })
                }
            }
        }
    }

    @Test fun doneCallsCheckDoneWithoutHaptic() {
        content()
        compose.onNodeWithTag("step.done").performScrollTo().performClick()
        assertEquals(listOf(CheckStatus.Done), calls)
        assertTrue(haptics.events.isEmpty())
    }

    @Test fun skipAndUndoCallCheck() {
        content()
        compose.onNodeWithTag("step.skip").performScrollTo().performClick()
        compose.runOnIdle { state.value = sheetState("skipped") }
        compose.onNodeWithTag("step.undo").performScrollTo().performClick()
        assertEquals(listOf(CheckStatus.Skipped, null), calls)
    }

    @Test fun busyDisablesActions() {
        content(true)
        compose.onNodeWithTag("step.done").assertIsNotEnabled()
        compose.onNodeWithTag("step.skip").assertIsNotEnabled()
    }

    @Test fun statusChangeAnnouncesAndMovesFocus() {
        content()
        compose.runOnIdle { state.value = sheetState("done") }
        compose.onNodeWithTag("step.undo").assertIsFocused()
        compose.onNodeWithTag("plan.announcer").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
        compose.runOnIdle { state.value = sheetState("upcoming") }
        compose.onNodeWithTag("step.done").assertIsFocused()
    }

    @Test fun refusalShowsOnceOnTopHost() {
        val model = RefusedChangeModel()
        val result =
            SyncResult(
                SyncState.Idle,
                0,
                refusals =
                    listOf(
                        SyncRefusal("one", "checkOff", 422, "now", "refused", SyncRefusalNotice.SleepTimes),
                    ),
            )
        val before = RefusedChangeDebug.presentedCount.get()
        compose.setContent {
            ChronoFlowTheme {
                Column {
                    Box(Modifier.testTag("outer")) { RefusedChangeFeedback(model = model) }
                    Box(Modifier.testTag("inner")) { RefusedChangeFeedback(model = model) }
                }
            }
        }
        compose.runOnIdle { model.receive(result) }
        compose.waitUntil(2_000) { RefusedChangeDebug.presentedCount.get() == before + 1 }
        compose.onNode(hasTestTag("sync.refusal") and hasAnyAncestor(hasTestTag("inner"))).assertIsDisplayed()
        compose.runOnIdle {
            model.consume("one")
            model.receive(result)
        }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        assertEquals(before + 1, RefusedChangeDebug.presentedCount.get())
    }

    @Test fun goneSeeTodayCallsBack() {
        state.value = sheetState("gone")
        content()
        compose.onNodeWithTag("step.gone").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        compose.onNodeWithTag("step.seeToday").performScrollTo().performClick()
        assertTrue(today)
    }
}
