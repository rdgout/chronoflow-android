@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.ui.designsystem

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.designsystem.component.CfPillGroup
import net.chronoflow.core.designsystem.component.CfPillOption
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.testing.compose.RecordingHaptics
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class CfPillGroupTest : PrimitiveTestFixture() {
    @Test fun selectionUsesRadioRolesStableTagsAndNoHaptic() {
        var selected by mutableStateOf("light")
        val haptics = RecordingHaptics()
        host {
            CompositionLocalProvider(LocalHaptics provides haptics) {
                CfPillGroup(
                    listOf(
                        CfPillOption("light", stringResource(R.string.settings_light)),
                        CfPillOption("dark", stringResource(R.string.settings_dark)),
                    ),
                    selected,
                    { selected = it },
                    "pills",
                )
            }
        }
        compose.onNodeWithTag("pills").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
        compose.onNodeWithTag("pills.light").assertIsSelected()
        compose
            .onNodeWithTag(
                "pills.dark",
            ).assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton),
            ).performClick()
            .assertIsSelected()
        compose.runOnIdle {
            assertEquals("dark", selected)
            assertTrue(haptics.events.isEmpty())
        }
    }
}
