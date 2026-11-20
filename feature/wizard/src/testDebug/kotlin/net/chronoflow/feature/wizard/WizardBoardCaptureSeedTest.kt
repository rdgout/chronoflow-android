package net.chronoflow.feature.wizard

import net.chronoflow.core.wizard.model.WizardAirportSheet
import net.chronoflow.core.wizard.model.route
import net.chronoflow.feature.wizard.debug.WizardBoardCaptureSeed
import net.chronoflow.feature.wizard.ui.AirportSheetUi
import net.chronoflow.feature.wizard.ui.WhereUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Locale

@RunWith(Parameterized::class)
class WizardBoardCaptureSeedTest(
    private val board: String,
    private val expected: List<Any?>,
) {
    @Test fun draftMatchesTheNamedBoard() {
        val locale = Locale.forLanguageTag(if (board.endsWith("NL")) "nl" else "en")
        val draft = WizardBoardCaptureSeed.draft(board, locale)
        val where = WhereUi.from(draft)
        val sheet = AirportSheetUi.from(draft)
        val presentation = WizardBoardCaptureSeed.presentation(board)
        assertEquals(
            expected,
            listOf(
                where.rows.size,
                where.home,
                where.rows.first().iata,
                where.rows.last().iata,
                draft.state.fromLastTrip,
                sheet?.sheet?.index,
                sheet?.sheet?.side,
                presentation.query,
                presentation.discardDialogVisible,
            ),
        )
        if (board == "WizardWhereLimit") {
            assertEquals(16, draft.form.legs.size)
            assertTrue(
                draft.form
                    .route()
                    .zipWithNext()
                    .all { (a, b) -> a.iata != b.iata },
            )
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> {
            val from = WizardAirportSheet.Side.from
            val home = WizardAirportSheet.Side.home
            val stop = WizardAirportSheet.Side.stop
            val expected =
                mapOf(
                    "WizardWhere" to listOf(2, false, "AMS", "NRT", true, null, null, null, false),
                    "WizardWhereEmpty" to listOf(2, false, null, null, false, null, null, null, false),
                    "WizardWhereSame" to listOf(2, false, "AMS", "AMS", true, null, null, null, false),
                    "WizardWhereStop" to listOf(3, false, "AMS", "NRT", true, null, null, null, false),
                    "WizardWhereStops" to listOf(4, false, "AMS", "NRT", true, null, null, null, false),
                    "WizardWhereLimit" to listOf(17, false, "AMS", "NRT", true, null, null, null, false),
                    "WizardHomeRoute" to listOf(2, true, "NRT", "AMS", false, null, null, null, false),
                    "WizardHomeRouteStop" to listOf(3, true, "NRT", "AMS", false, null, null, null, false),
                    "WizardAirportFrom" to listOf(2, false, null, null, false, 0, from, "Am", false),
                    "WizardAirportHome" to listOf(2, true, "NRT", "AMS", false, 1, home, "Am", false),
                    "WizardStopSearch" to listOf(2, false, "AMS", "NRT", false, 0, stop, "Du", false),
                    "WizardStopSearchSecond" to listOf(3, false, "AMS", "NRT", true, 1, stop, "Ban", false),
                    "WizardStopSearchHome" to listOf(2, true, "NRT", "AMS", false, 1, stop, "Du", false),
                    "WizardDiscard" to listOf(2, false, "AMS", "NRT", false, null, null, null, true),
                ) +
                    (net.chronoflow.feature.wizard.debug.WizardCompletionBoardSeed.baseNames + "WizardSheetFreeSleep")
                        .associateWith { name ->
                            listOf(
                                if (name == "WizardFineTuneMulti" || name == "WizardReadyReturn") 3 else 2,
                                false,
                                "AMS",
                                "NRT",
                                false,
                                null,
                                null,
                                null,
                                false,
                            )
                        }
            return WizardBoardCaptureSeed.names.filter { WizardBoardCaptureSeed.baseName(it) in expected }.map {
                arrayOf<Any>(
                    it,
                    expected.getValue(WizardBoardCaptureSeed.baseName(it)),
                )
            }
        }
    }
}
