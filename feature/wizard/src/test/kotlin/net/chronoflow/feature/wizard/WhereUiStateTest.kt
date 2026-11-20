@file:Suppress("ktlint:standard:max-line-length")
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.wizard

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardSnapshot
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardAirportSheet
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.core.wizard.model.setRouteAirport
import net.chronoflow.feature.wizard.ui.AirportRowUi
import net.chronoflow.feature.wizard.ui.AirportSheetUi
import net.chronoflow.feature.wizard.ui.RowHint
import net.chronoflow.feature.wizard.ui.RowLabel
import net.chronoflow.feature.wizard.ui.StopQuestionUi
import net.chronoflow.feature.wizard.ui.WhereUi
import net.chronoflow.feature.wizard.ui.WizardChromeUi
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.airportPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class WhereUiStateTest {
    @Test fun emptyRouteShowsPlaceholdersAndDisablesSwap() {
        val ui = WhereUi.from(MemberWizardDraft())
        assertEquals(2, ui.rows.size)
        assertNull(ui.rows[0].iata)
        assertNull(ui.rows[1].iata)
        assertTrue(ui.showSwap)
        assertFalse(ui.swapEnabled)
        assertNull(ui.question)
        assertFalse(ui.complete)
    }

    @Test fun completeDirectRouteAsksAboutStops() {
        val ui = WhereUi.from(routeDraft())
        assertEquals(StopQuestionUi.Selection.Direct, ui.question!!.selected)
        assertFalse(ui.showAddStop)
        assertFalse(ui.limitReached)
    }

    @Test fun lastTripHintOnlyOnMemberFromWhenNotSame() {
        val draft = routeDraft()
        assertEquals(RowHint.LastTrip, WhereUi.from(draft).rows[0].hint)
        assertNull(WhereUi.from(draft.copy(state = draft.state.copy(fromLastTrip = false))).rows[0].hint)
        assertNull(WhereUi.from(draft.copy(state = draft.state.copy(mode = WizardMode.guest))).rows[0].hint)
        assertEquals(RowHint.WhereYouLanded, WhereUi.from(routeDraft(home = true)).rows[0].hint)
        assertNull(WhereUi.from(draft.copy(form = draft.form.setRouteAirport(AMS, 1))).rows[0].hint)
    }

    @Test fun sameRouteMarksTheLastRowAndHidesTheQuestion() {
        val draft = routeDraft()
        val ui = WhereUi.from(draft.copy(form = draft.form.setRouteAirport(AMS, 1)))
        assertTrue(ui.same)
        assertTrue(ui.rows.last().invalid)
        assertFalse(ui.rows.first().invalid)
        assertNull(ui.question)
    }

    @Test fun oneStopIsLabelledStopAndHidesSwap() {
        val ui = WhereUi.from(routeDraft(1))
        assertEquals(listOf(RowLabel.From, RowLabel.Stop, RowLabel.To), ui.rows.map { it.label })
        assertFalse(ui.showSwap)
        assertTrue(ui.rows[1].removable)
        assertTrue(ui.showAddStop)
    }

    @Test fun twoStopsAreNumbered() {
        val ui = WhereUi.from(routeDraft(2))
        assertEquals(RowLabel.StopN(1), ui.rows[1].label)
        assertEquals(RowLabel.StopN(2), ui.rows[2].label)
    }

    @Test fun sixteenFlightsReachesTheLimit() {
        val ui = WhereUi.from(routeDraft(15))
        assertTrue(ui.limitReached)
        assertFalse(ui.showAddStop)
    }

    @Test fun yesIsDisabledAtTheLimitWithoutOutboundStops() {
        var draft = routeDraft(home = true)
        var form = draft.form
        repeat(14) {
            form =
                form.insertStop(
                    if (it % 2 ==
                        0
                    ) {
                        DXB
                    } else {
                        BKK
                    },
                    form.legs.lastIndex,
                )
        }
        draft =
            draft.copy(form = form, state = draft.state.copy(step = MemberWizardStep.whereTo))
        val ui = WhereUi.from(draft)
        assertFalse(ui.question!!.yesEnabled)
        assertTrue(ui.limitReached)
    }

    @Test fun homeRouteLocksFromAndHidesSwapAndNotice() {
        val ui = WhereUi.from(routeDraft(home = true))
        assertTrue(ui.rows[0].locked)
        assertNull(ui.rows[0].sheet)
        assertEquals(RowHint.WhereYouLanded, ui.rows[0].hint)
        assertFalse(ui.showSwap)
        assertFalse(ui.showNotice)
    }

    @Test fun rowTapTargetsMatchIos() {
        val ui = WhereUi.from(routeDraft(1))
        assertEquals(
            listOf(
                WizardAirportSheet(0, WizardAirportSheet.Side.from, false),
                WizardAirportSheet(0, WizardAirportSheet.Side.stop, true),
                WizardAirportSheet(1, WizardAirportSheet.Side.to, false),
            ),
            ui.rows.map {
                it.sheet
            },
        )
        val home = routeDraft(1, home = true)
        assertEquals(
            WizardAirportSheet(2, WizardAirportSheet.Side.home, false),
            WhereUi
                .from(home)
                .rows
                .last()
                .sheet,
        )
    }

    @Test fun progressFollowsTheBoardValues() {
        val empty = TripWizardUiState.Active(MemberWizardSnapshot(MemberWizardDraft()), 0.08, emptyList())
        assertEquals(8, WizardChromeUi.from(empty).progressPercent)
        assertEquals(0.08f, WizardChromeUi.from(empty).progress)
        assertEquals(15, WizardChromeUi.from(empty.copy(progress = 0.15)).progressPercent)
        assertEquals(50, WizardChromeUi.from(empty.copy(progress = 0.5)).progressPercent)
    }

    @Test fun rowDescriptionsUseTheIosKeys() {
        val ui = WhereUi.from(routeDraft(1))
        assertEquals(WizardCopy("From"), ui.rows[0].description)
        assertEquals(
            WizardCopy(
                "Remove stop in {{city}}",
                mapOf("city" to "Dubai"),
            ),
            ui.rows[1].removeDescription,
        )
        assertEquals(WizardCopy("Swap airports"), ui.swapDescription)
        assertEquals(WizardCopy("Do you change planes on the way?"), ui.question!!.description)
        assertEquals(
            WizardCopy(
                "Leaves from {{city}}, where you landed. You can't change this.",
                mapOf("city" to "Tokyo"),
            ),
            WhereUi.from(routeDraft(home = true)).rows[0].description,
        )
    }

    @Test fun sheetTitleAndCaptionPerSide() {
        val draft = routeDraft()
        assertEquals(
            "Where do you fly from?",
            AirportSheetUi
                .from(
                    draft.copy(state = draft.state.copy(sheet = WizardAirportSheet(0, WizardAirportSheet.Side.from))),
                )!!
                .title.key,
        )
        assertEquals(
            "Where do you fly to?",
            AirportSheetUi(WizardAirportSheet(0, WizardAirportSheet.Side.to), null).title.key,
        )
        assertEquals(
            "Where do you fly home to?",
            AirportSheetUi(WizardAirportSheet(1, WizardAirportSheet.Side.home), null).title.key,
        )
        assertEquals(
            "Where's your stop?",
            AirportSheetUi(WizardAirportSheet(0, WizardAirportSheet.Side.stop), null).title.key,
        )
        assertEquals(
            "Amsterdam" to "Tokyo",
            AirportSheetUi
                .from(
                    draft.copy(state = draft.state.copy(sheet = WizardAirportSheet(0, WizardAirportSheet.Side.stop))),
                )!!
                .caption,
        )
        val stop = routeDraft(1)
        assertEquals(
            "Amsterdam" to "Tokyo",
            AirportSheetUi
                .from(
                    stop.copy(
                        state = stop.state.copy(sheet = WizardAirportSheet(0, WizardAirportSheet.Side.stop, true)),
                    ),
                )!!
                .caption,
        )
        assertEquals(
            "Dubai" to "Tokyo",
            AirportSheetUi
                .from(
                    stop.copy(state = stop.state.copy(sheet = WizardAirportSheet(1, WizardAirportSheet.Side.stop))),
                )!!
                .caption,
        )
        val home = routeDraft(home = true)
        assertEquals(
            "Tokyo" to "Amsterdam",
            AirportSheetUi
                .from(
                    home.copy(state = home.state.copy(sheet = WizardAirportSheet(1, WizardAirportSheet.Side.stop))),
                )!!
                .caption,
        )
    }

    @Test fun placeUsesTheGivenLocale() {
        assertEquals("Amsterdam, Netherlands", airportPlace(AirportRowUi.from(AMS), java.util.Locale.ENGLISH))
        assertEquals(
            "Amsterdam, Nederland",
            airportPlace(AirportRowUi.from(AMS), java.util.Locale.forLanguageTag("nl")),
        )
    }
}
