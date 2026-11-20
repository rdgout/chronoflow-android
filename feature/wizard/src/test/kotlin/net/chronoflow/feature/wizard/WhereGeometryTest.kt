@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.RecordingHaptics
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.setRouteAirport
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WhereGeometryTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    private fun bounds(tag: String): Rect =
        compose.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot().let {
            Rect(it.left.value, it.top.value, it.right.value, it.bottom.value)
        }

    private val events = mutableListOf<WizardEvent>()
    private val haptics = RecordingHaptics()

    private fun host(draft: MemberWizardDraft = routeDraft()) {
        compose.setContent {
            WizardTestHost {
                CompositionLocalProvider(
                    LocalHaptics provides haptics,
                ) { WizardScreen(screenState(draft), events::add) }
            }
        }
    }

    private fun click(tag: String) {
        compose.onNodeWithTag(tag).performScrollTo().performClick()
    }

    @Test fun rowsAndCardMatchTheBoard() {
        host(routeDraft(1))
        val card = bounds("wizard.route")
        val subtitle = bounds("wizard.subtitle")
        assertEquals(subtitle.bottom + 18f, card.top, 0.5f)
        assertTrue(bounds("wizard.from").height >= 68f)
        assertTrue(bounds("wizard.stop.1").height >= 68f)
        assertTrue(bounds("wizard.to").height >= 68f)
        assertEquals(card.left + 16f, bounds("wizard.label.0").left, 0.5f)
        val remove = bounds("wizard.remove-stop.1")
        assertTrue(remove.width >= 48f && remove.height >= 48f)
    }

    @Test fun swapVisualHasTheBoardSizeAndInset() {
        host()
        val card = bounds("wizard.route")
        val swap = bounds("wizard.swap.visual")
        assertEquals(40f, swap.width, 0.5f)
        assertEquals(card.right - 14, swap.right, 0.5f)
        assertTrue(bounds("wizard.swap").height >= 48f)
    }
}
