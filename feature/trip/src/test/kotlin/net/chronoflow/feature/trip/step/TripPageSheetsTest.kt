@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package net.chronoflow.feature.trip.step

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.waitUntilExactlyOneExists
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.RecordingHaptics
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.trip.page.TripPage
import net.chronoflow.feature.trip.page.TripPageTestRig
import org.junit.After
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
class TripPageSheetsTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var rig: TripPageTestRig
    private val haptics = RecordingHaptics()
    private val id get() =
        rig.model.prepared.days
            .first()
            .blocks
            .first {
                it.block.actionable &&
                    it.block.checkKey != null
            }.id

    private fun host(
        list: Boolean = false,
        summary: Boolean = false,
    ) {
        rig = TripPageTestRig("S1", initialTimeline = !list, startAtSummary = summary, applyChecks = true)
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(reduceMotion = true) {
                    CompositionLocalProvider(LocalHaptics provides haptics) { TripPage(rig.vm, {}, null) }
                }
            }
        }
    }

    @After fun close() {
        if (::rig.isInitialized) rig.close()
    }

    private fun open() {
        compose.waitUntilExactlyOneExists(hasTestTag("trip.block.$id"), 2_000)
        compose.onNodeWithTag("trip.block.$id").performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.onNodeWithTag("step.sheet").assertExists()
    }

    @Test fun blockSemanticClickOpensStepSheet() {
        host()
        open()
        compose.onNodeWithTag("step.title").assertExists()
    }

    @Test fun listRowTapOpensSheet() {
        host(list = true)
        compose.onNodeWithTag("trip.block.$id").performScrollTo().performClick()
        compose.onNodeWithTag("step.sheet").assertExists()
        assertEquals(
            id,
            rig.vm.selectedStep.value
                ?.blockId,
        )
    }

    @Test fun legendButtonOpensPriorityLegend() {
        host(summary = true)
        compose.onNodeWithTag("trip.legend").performClick()
        compose.onNodeWithTag("legend.priorities").assertExists()
        compose.onNodeWithTag("legend.close").performClick()
        compose.onNodeWithTag("legend.priorities").assertDoesNotExist()
    }

    @Test fun doneThenUndoInSheet() {
        host()
        open()
        compose.onNodeWithTag("step.done").performScrollTo().performClick()
        compose.onNodeWithTag("step.undo").performScrollTo().performClick()
        compose.onNodeWithTag("step.done").assertExists()
    }

    @Test fun noHapticOnCheckOff() {
        host()
        open()
        compose.onNodeWithTag("step.done").performScrollTo().performClick()
        compose.onNodeWithTag("step.undo").assertExists()
        assertTrue(haptics.events.isEmpty())
    }
}
