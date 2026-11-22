@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.ui.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.system.CfMediumTopAppBar
import net.chronoflow.core.designsystem.system.CfSegmentedButtons
import net.chronoflow.core.designsystem.system.CfSwitch
import net.chronoflow.core.designsystem.system.cfToggleable
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.RecordingHaptics
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class SystemWrappersTest : PrimitiveTestFixture() {
    @Test fun switchTogglesAndEmitsOnlyItsNativeHapticEvents() {
        val haptics = RecordingHaptics()
        var checked by mutableStateOf(false)
        host {
            CompositionLocalProvider(LocalHaptics provides haptics) {
                CfSwitch(checked, { checked = it }, Modifier.testTag("switch"))
            }
        }
        compose
            .onNodeWithTag("switch")
            .assertIsOff()
            .performClick()
            .assertIsOn()
            .performClick()
            .assertIsOff()
        compose.runOnIdle { assertEquals(listOf(HapticEvent.ToggleOn, HapticEvent.ToggleOff), haptics.events) }
    }

    @Test fun wholeRowToggleExposesTheSwitchRoleAndUsesTheFakeHaptics() {
        val haptics = RecordingHaptics()
        var checked by mutableStateOf(false)
        host {
            CompositionLocalProvider(LocalHaptics provides haptics) {
                Box(Modifier.size(100.dp, 48.dp).testTag("toggle").cfToggleable(checked, { checked = it }))
            }
        }
        compose
            .onNodeWithTag("toggle")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .performClick()
            .assertIsOn()
        compose.runOnIdle { assertEquals(listOf(HapticEvent.ToggleOn), haptics.events) }
    }

    @Test fun segmentSelectionFiresOnceAndRetappingDoesNotEmitAHaptic() {
        val haptics = RecordingHaptics()
        var selected by mutableIntStateOf(0)
        var calls = 0
        host {
            CompositionLocalProvider(LocalHaptics provides haptics) {
                CfSegmentedButtons(
                    listOf(stringResource(R.string.settings_light), stringResource(R.string.settings_dark)),
                    selected,
                    {
                        selected = it
                        calls++
                    },
                    testTag = "segments",
                )
            }
        }
        compose.onNodeWithTag("segments.1").performClick().performClick()
        compose.runOnIdle {
            assertEquals(1, selected)
            assertEquals(1, calls)
            assertEquals(listOf(HapticEvent.Selection), haptics.events)
        }
    }

    @Test fun mediumAppBarCollapsesThroughItsNestedScrollConnection() {
        host {
            val behavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            Column(Modifier.fillMaxSize().nestedScroll(behavior.nestedScrollConnection)) {
                CfMediumTopAppBar(stringResource(R.string.settings_change_password), behavior, Modifier.testTag("bar"))
                LazyColumn(Modifier.fillMaxWidth().testTag("list")) {
                    items(30, key = { it }, contentType = { "spacer" }) { Box(Modifier.height(100.dp)) }
                }
            }
        }
        val expanded =
            compose
                .onNodeWithTag("bar")
                .fetchSemanticsNode()
                .boundsInRoot.height
        compose.onNodeWithTag("list").performTouchInput { swipeUp() }
        compose.waitForIdle()
        val collapsed =
            compose
                .onNodeWithTag("bar")
                .fetchSemanticsNode()
                .boundsInRoot.height
        val profile = DeviceProfiles.pixel8
        val topInset = profile.insets.top
        assertEquals(
            (TopAppBarDefaults.MediumAppBarExpandedHeight.value + topInset) * profile.density,
            expanded,
            1f,
        )
        assertEquals(
            (TopAppBarDefaults.MediumAppBarCollapsedHeight.value + topInset) * profile.density,
            collapsed,
            1f,
        )
    }
}
