package net.chronoflow.app

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.Haptics
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.system.CfSegmentedButtons
import net.chronoflow.core.designsystem.system.CfSwitch
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.tiers.Smoke
import net.chronoflow.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Test

@Smoke
class DesignSystemControlsDeviceTest : ScreenTest() {
    @Test fun nativeControlsKeepTheirStateSemanticsAndHaptics() {
        val events = mutableListOf<HapticEvent>()
        var selection = 0
        launch(mapOf("cf.reduceMotion" to "1")).onActivity { activity ->
            activity.setContent {
                ChronoFlowTheme {
                    CompositionLocalProvider(LocalHaptics provides Haptics { events += it }) {
                        var checked by remember { mutableStateOf(false) }
                        Column {
                            CfSwitch(checked, { checked = it }, Modifier.testTag("controls.switch"))
                            CfSegmentedButtons(
                                listOf(
                                    stringResource(R.string.settings_light),
                                    stringResource(R.string.settings_dark),
                                ),
                                selection,
                                {
                                    selection =
                                        it
                                },
                                testTag = "controls.segments",
                            )
                        }
                    }
                }
            }
        }
        compose
            .onNodeWithTag("controls.switch")
            .assertIsOff()
            .performClick()
            .assertIsOn()
            .performClick()
            .assertIsOff()
        compose.onNodeWithTag("controls.segments.1").performClick()
        compose.runOnIdle {
            assertEquals(1, selection)
            assertEquals(listOf(HapticEvent.ToggleOn, HapticEvent.ToggleOff, HapticEvent.Selection), events)
        }
    }
}
