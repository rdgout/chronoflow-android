package net.chronoflow.feature.wizard

import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.feature.wizard.debug.WizardBoardCaptureSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Locale

@RunWith(Parameterized::class)
class WizardFlightBoardSeedTest(
    private val board: String,
    private val expected: Expected,
) {
    data class Expected(
        val step: MemberWizardStep,
        val boundary: Int,
        val times: List<Pair<String, String>>,
        val errors: Set<String> = emptySet(),
    )

    @Test fun flightDraftMatchesThePinnedSeed() {
        val language = if (board.endsWith("NL")) "nl" else "en"
        val draft = WizardBoardCaptureSeed.draft(board, Locale.forLanguageTag(language))
        assertEquals(expected.step, draft.state.step)
        assertEquals(expected.times.size, draft.form.legs.size)
        assertEquals(expected.boundary, draft.form.returnBoundary)
        assertEquals(expected.times, draft.form.legs.map { it.departLocal to it.arriveLocal })
        assertEquals(expected.errors, draft.fieldErrors.keys)
        assertFalse(draft.state.fromLastTrip)
        assertFalse(draft.state.sleepNeeded)
        assertTrue(draft.state.entered)
        assertEquals(if (language == "nl") "Tokio" else "Tokyo", draft.airports.getValue("NRT").city)
        if (expected.step == MemberWizardStep.returnFlight) {
            assertEquals(true, draft.state.returnRouteConfirmed)
            assertEquals("NRT", draft.form.legs[1].fromIata)
            assertEquals("AMS", draft.form.legs[1].toIata)
        }
        if (WizardBoardCaptureSeed.baseName(board) ==
            "WizardDiscard"
        ) {
            assertTrue(WizardBoardCaptureSeed.presentation(board).discardDialogVisible)
        }
    }

    companion object {
        private val base = "2026-10-12T14:40" to "2026-10-13T09:15"
        private val stop = listOf("2026-10-12T14:40" to "2026-10-12T23:25", "2026-10-13T02:05" to "2026-10-13T16:35")
        val expected =
            mapOf(
                "WizardOutbound" to Expected(MemberWizardStep.outbound, 1, listOf(base)),
                "WizardOutboundEmpty" to Expected(MemberWizardStep.outbound, 1, listOf("" to "")),
                "WizardOutboundErrors" to
                    Expected(
                        MemberWizardStep.outbound,
                        1,
                        listOf(base.first to "2026-10-11T09:15"),
                        setOf("legs.0.arriveLocal"),
                    ),
                "WizardOutboundTwoErrors" to
                    Expected(
                        MemberWizardStep.outbound,
                        2,
                        listOf(stop[0], stop[1].first to "2026-10-12T16:35"),
                        setOf("legs.1.arriveLocal"),
                    ),
                "WizardOutboundStop" to Expected(MemberWizardStep.outbound, 2, stop),
                "WizardOutboundStopEmpty" to Expected(MemberWizardStep.outbound, 2, listOf("" to "", "" to "")),
                "WizardOutboundOvernight" to
                    Expected(
                        MemberWizardStep.outbound,
                        2,
                        listOf(
                            base.first to "2026-10-12T22:25",
                            "2026-10-14T02:05" to "2026-10-14T16:35",
                        ),
                    ),
                "WizardOutboundThree" to
                    Expected(
                        MemberWizardStep.outbound,
                        3,
                        listOf(stop[0], stop[1].first to "2026-10-13T11:20", "" to ""),
                    ),
                "WizardOutboundPast" to
                    Expected(
                        MemberWizardStep.outbound,
                        1,
                        listOf("2026-09-02T14:40" to "2026-09-03T09:15"),
                        setOf("review"),
                    ),
                "WizardOutboundScrollErrors" to
                    Expected(
                        MemberWizardStep.outbound,
                        5,
                        listOf(
                            base.first to "2026-10-11T23:25",
                            stop[1].first to "2026-10-13T11:20",
                            "2026-10-13T13:00" to "2026-10-13T17:00",
                            "2026-10-13T19:00" to "2026-10-14T04:00",
                            "2026-10-14T03:00" to "2026-10-14T14:30",
                        ),
                        setOf("legs.0.arriveLocal", "legs.4.departLocal"),
                    ),
                "WizardReturn" to Expected(MemberWizardStep.returnChoice, 1, listOf(base)),
                "WizardReturnFlight" to Expected(MemberWizardStep.returnFlight, 1, listOf(base, "" to "")),
                "WizardReturnFlightPast" to
                    Expected(
                        MemberWizardStep.returnFlight,
                        1,
                        listOf(base, "2026-10-22T10:25" to "2026-10-22T15:30"),
                        setOf("review"),
                    ),
                "WizardDiscard" to Expected(MemberWizardStep.outbound, 1, listOf(base)),
            )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            WizardBoardCaptureSeed.names
                .filter {
                    WizardBoardCaptureSeed.baseName(it) in
                        expected
                }.map { arrayOf(it, expected.getValue(WizardBoardCaptureSeed.baseName(it))) }
    }
}
