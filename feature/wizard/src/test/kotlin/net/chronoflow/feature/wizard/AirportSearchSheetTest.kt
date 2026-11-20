@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.theme.toTextStyle
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.RecordingHaptics
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.wizard.model.WizardAirportSheet
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardScreen
import net.chronoflow.feature.wizard.ui.AirportRowUi
import net.chronoflow.feature.wizard.ui.AirportSearchUi
import net.chronoflow.feature.wizard.ui.AirportSheetUi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class AirportSearchSheetTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)
    private val events = mutableListOf<WizardEvent>()
    private val haptics = RecordingHaptics()

    private fun host(
        search: AirportSearchUi = AirportSearchUi.Hint,
        stop: Boolean = false,
        retryEnabled: Boolean = true,
    ) {
        val sheet = WizardAirportSheet(0, if (stop) WizardAirportSheet.Side.stop else WizardAirportSheet.Side.from)
        val state =
            screenState().copy(
                sheet = AirportSheetUi(sheet, if (stop) "Amsterdam" to "Tokyo" else null),
                search = search,
                retryEnabled = retryEnabled,
            )
        compose.setContent {
            WizardTestHost {
                CompositionLocalProvider(LocalHaptics provides haptics) {
                    WizardScreen(state) { if (it !is WizardEvent.Query) events.add(it) }
                }
            }
        }
    }

    @Test fun fieldIsFocusedWhenTheSheetOpens() {
        host()
        compose.onNodeWithTag("wizard.airport.query").assertIsFocused()
    }

    @Test fun headerUsesTheBoardDisplayStyle() {
        host()
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag("wizard.airport.title").performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it(layouts)
        }
        val expected = CfTypeScale.wizardSheetTitle.toTextStyle()
        val actual = layouts.single().layoutInput.style
        assertEquals(expected.fontFamily, actual.fontFamily)
        assertEquals(expected.fontWeight, actual.fontWeight)
        assertEquals(expected.fontSize, actual.fontSize)
        assertEquals(expected.lineHeight, actual.lineHeight)
        assertEquals(expected.letterSpacing, actual.letterSpacing)
    }

    @Test fun eachSearchStateShowsItsTagHint() {
        host()
        compose.onNodeWithTag("wizard.airport.hint").assertExists()
    }

    @Test fun eachSearchStateShowsItsTagSearching() {
        host(AirportSearchUi.Searching)
        compose.onNodeWithTag("wizard.airport.searching").assertExists()
    }

    @Test fun eachSearchStateShowsItsTagResults() {
        host(AirportSearchUi.Results(listOf(AirportRowUi.from(AMS))))
        compose.onNodeWithTag("wizard.airport.results").assertExists()
        compose.onNodeWithTag("wizard.airport.AMS").assertExists()
    }

    @Test fun eachSearchStateShowsItsTagEmpty() {
        host(AirportSearchUi.Empty)
        compose.onNodeWithTag("wizard.airport.empty").assertExists()
    }

    @Test fun eachSearchStateShowsItsTagFailed() {
        host(AirportSearchUi.Failed(null))
        compose.onNodeWithTag("wizard.airport.error").assertExists()
        compose.onNodeWithTag("wizard.airport.retry").assertIsEnabled().performClick()
        assertEquals(listOf(WizardEvent.RetrySearch), events)
    }

    @Test fun eachSearchStateShowsItsTagThrottled() {
        host(AirportSearchUi.Failed(Instant.EPOCH.plusSeconds(60)), retryEnabled = false)
        compose.onNodeWithTag("wizard.airport.retry").assertIsNotEnabled()
    }

    @Test fun selectingAStopFiresLightAndChoosesOnce() {
        host(AirportSearchUi.Results(listOf(AirportRowUi.from(DXB))), stop = true)
        compose.onNodeWithTag("wizard.airport.DXB").performSemanticsAction(SemanticsActions.OnClick) {
            it()
            it()
        }
        compose.waitForIdle()
        assertEquals(
            listOf(WizardEvent.ChooseAirport(DXB, WizardAirportSheet(0, WizardAirportSheet.Side.stop))),
            events,
        )
        assertEquals(listOf(HapticEvent.Light), haptics.events)
    }

    @Test fun selectingFromFiresSelection() {
        host(AirportSearchUi.Results(listOf(AirportRowUi.from(AMS))))
        compose.onNodeWithTag("wizard.airport.AMS").performClick()
        compose.waitForIdle()
        assertEquals(
            listOf(WizardEvent.ChooseAirport(AMS, WizardAirportSheet(0, WizardAirportSheet.Side.from))),
            events,
        )
        assertEquals(listOf(HapticEvent.Selection), haptics.events)
    }

    @Test fun cancelSendsCancelAirport() {
        host()
        compose.onNodeWithTag("wizard.airport.cancel").performClick()
        compose.waitForIdle()
        assertEquals(listOf(WizardEvent.CancelAirport), events)
    }

    @Test fun queryIsCappedAt60() {
        host()
        compose.onNodeWithTag("wizard.airport.query").performTextInput("a".repeat(60))
        compose.onNodeWithTag("wizard.airport.query").performTextInput("a")
        assertEquals(
            60,
            compose
                .onNodeWithTag(
                    "wizard.airport.query",
                ).fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
                .length,
        )
    }
}
