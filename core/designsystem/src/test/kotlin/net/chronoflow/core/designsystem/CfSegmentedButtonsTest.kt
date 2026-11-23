package net.chronoflow.core.designsystem

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.Haptics
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.system.CfSegmentedButtons
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
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
@Config(sdk = [36])
class CfSegmentedButtonsTest {
    @get:Rule val compose = createComposeRule()
    private val haptics = mutableListOf<HapticEvent>()
    private val selections = mutableListOf<Int>()

    @Test fun defaultsKeepEveryOptionEnabledAndUseOneSelectionHaptic() {
        compose.setContent {
            ChronoFlowTheme(reduceMotion = true) {
                CompositionLocalProvider(LocalHaptics provides Haptics { haptics += it }) {
                    CfSegmentedButtons(listOf("", ""), 0, selections::add, testTag = "segments")
                }
            }
        }
        compose.onNodeWithTag("segments.0").assertIsEnabled().performClick()
        assertTrue(haptics.isEmpty())
        assertTrue(selections.isEmpty())
        compose.onNodeWithTag("segments.1").assertIsEnabled().performClick()
        assertEquals(listOf(1), selections)
        assertEquals(listOf(HapticEvent.Selection), haptics)
    }

    @Test fun minusOneAllowsNoSelectedOptionAndATapSelectsAValidOne() {
        compose.setContent {
            ChronoFlowTheme(reduceMotion = true) {
                CfSegmentedButtons(listOf("", ""), -1, selections::add, testTag = "segments")
            }
        }
        compose.onNodeWithTag("segments.0").assertIsNotSelected()
        compose.onNodeWithTag("segments.1").assertIsNotSelected().performClick()
        assertEquals(listOf(1), selections)
    }

    @Test fun aDisabledOptionExposesMaterialSemanticsAndHasNoFeedback() {
        compose.setContent {
            ChronoFlowTheme(reduceMotion = true) {
                CompositionLocalProvider(LocalHaptics provides Haptics { haptics += it }) {
                    CfSegmentedButtons(
                        listOf("", ""),
                        0,
                        selections::add,
                        testTag = "segments",
                        optionEnabled = { it == 0 },
                    )
                }
            }
        }
        compose.onNodeWithTag("segments.1").assertIsNotEnabled().performClick()
        assertTrue(haptics.isEmpty())
        assertTrue(selections.isEmpty())
    }
}
