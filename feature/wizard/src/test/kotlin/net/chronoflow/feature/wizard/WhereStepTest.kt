@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard

import androidx.compose.runtime.CompositionLocalProvider
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

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WhereStepTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)
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

    @Test fun tappingRowsOpensTheMatchingSheet() {
        val draft = routeDraft(1)
        host(draft)
        click("wizard.from")
        click("wizard.to")
        click("wizard.stop.1")
        assertEquals(
            listOf(
                WizardEvent.OpenAirport(screenState(draft).where!!.rows[0].sheet!!),
                WizardEvent.OpenAirport(screenState(draft).where!!.rows[2].sheet!!),
                WizardEvent.OpenAirport(screenState(draft).where!!.rows[1].sheet!!),
            ),
            events,
        )
    }

    @Test fun lockedHomeFromIsNotClickable() {
        host(routeDraft(home = true))
        compose.onNodeWithTag("wizard.from").assertIsNotEnabled()
        click("wizard.from")
        assertTrue(events.isEmpty())
        click("wizard.to")
        assertEquals(
            WizardEvent.OpenAirport(
                screenState(routeDraft(home = true))
                    .where!!
                    .rows
                    .last()
                    .sheet!!,
            ),
            events.single(),
        )
    }

    @Test fun swapFiresLightAndSwaps() {
        host()
        click("wizard.swap")
        assertEquals(listOf(WizardEvent.SwapAirports), events)
        assertEquals(listOf(HapticEvent.Light), haptics.events)
    }

    @Test fun removeFiresLightAndRemoves() {
        host(routeDraft(1))
        click("wizard.remove-stop.1")
        assertEquals(listOf(WizardEvent.RemoveStop(1)), events)
        assertEquals(listOf(HapticEvent.Light), haptics.events)
    }

    @Test fun noDirectWithStopsFiresLight() {
        host(routeDraft(1))
        click("wizard.route.stops.0")
        assertEquals(listOf(WizardEvent.MakeDirect(false)), events)
        assertTrue(HapticEvent.Light in haptics.events)
    }

    @Test fun yesWithoutStopsOpensTheStopSheet() {
        val draft = routeDraft()
        host(draft)
        click("wizard.route.stops.1")
        assertEquals(listOf(WizardEvent.OpenAirport(screenState(draft).where!!.question!!.addStopSheet)), events)
        assertEquals(listOf(HapticEvent.Selection), haptics.events)
    }

    @Test fun limitLineReplacesAddStop() {
        host(routeDraft(15))
        compose.onNodeWithTag("wizard.route.limit").performScrollTo().assertExists()
        compose.onNodeWithTag("wizard.add-stop").assertDoesNotExist()
    }

    @Test fun continueIsDisabledWhenEmptyOrSame() {
        host(MemberWizardDraft())
        compose.onNodeWithTag("wizard.where.continue").assertIsNotEnabled()
        compose.onNodeWithTag("wizard.swap").assertIsNotEnabled()
    }

    @Test fun sameLineIsAPoliteLiveRegion() {
        val draft = routeDraft()
        host(draft.copy(form = draft.form.setRouteAirport(AMS, 1)))
        compose.onNodeWithTag("wizard.where.continue").assertIsNotEnabled()
        assertEquals(
            LiveRegionMode.Polite,
            compose.onNodeWithTag("wizard.where.same").fetchSemanticsNode().config[SemanticsProperties.LiveRegion],
        )
    }
}
