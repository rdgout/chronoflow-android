@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard.debug

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardState
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardAirportSheet
import net.chronoflow.core.wizard.model.WizardDraftStore
import net.chronoflow.core.wizard.model.WizardForm
import net.chronoflow.core.wizard.model.setRouteAirport
import net.chronoflow.feature.wizard.ui.WizardInitialPresentation
import java.time.Instant
import java.util.Locale

object WizardBoardCaptureSeed {
    val now: Instant = Instant.parse("2026-10-01T09:00:00Z")
    val names =
        listOf(
            "WizardWhere",
            "WizardWhereDark",
            "WizardWhereNL",
            "WizardWhereEmpty",
            "WizardWhereEmptyDarkNL",
            "WizardWhereSame",
            "WizardWhereStop",
            "WizardWhereStopDarkNL",
            "WizardWhereStops",
            "WizardWhereStopsDarkNL",
            "WizardHomeRoute",
            "WizardHomeRouteDarkNL",
            "WizardHomeRouteStop",
            "WizardHomeRouteStopDarkNL",
            "WizardAirportFrom",
            "WizardAirportFromDarkNL",
            "WizardAirportHome",
            "WizardAirportHomeDarkNL",
            "WizardStopSearch",
            "WizardStopSearchDarkNL",
            "WizardStopSearchSecond",
            "WizardStopSearchSecondDarkNL",
            "WizardStopSearchHome",
            "WizardStopSearchHomeDarkNL",
            "WizardDiscard",
            "WizardDiscardDarkNL",
            "WizardWhereLimit",
        )

    fun baseName(board: String): String =
        when {
            board.endsWith("DarkNL") -> board.removeSuffix("DarkNL")
            board.endsWith("Dark") -> board.removeSuffix("Dark")
            board.endsWith("NL") -> board.removeSuffix("NL")
            else -> board
        }

    fun knows(board: String): Boolean = names.any { baseName(it) == baseName(board) }

    fun presentation(board: String): WizardInitialPresentation =
        WizardInitialPresentation(
            query =
                when (baseName(board)) {
                    "WizardAirportFrom", "WizardAirportHome" -> "Am"
                    "WizardStopSearch", "WizardStopSearchHome" -> "Du"
                    "WizardStopSearchSecond" -> "Ban"
                    else -> null
                },
            discardDialogVisible = baseName(board) == "WizardDiscard",
        )

    fun draft(
        board: String,
        locale: Locale,
    ): MemberWizardDraft {
        require(knows(board)) { "Unknown wizard board" }
        val board = baseName(board)
        val airports =
            listOf(
                ApiAirportSuggestion("AMS", "Amsterdam Airport Schiphol", "Amsterdam", "NL", "Europe/Amsterdam"),
                ApiAirportSuggestion(
                    "NRT",
                    "Narita International",
                    if (locale.language ==
                        "nl"
                    ) {
                        "Tokio"
                    } else {
                        "Tokyo"
                    },
                    "JP",
                    "Asia/Tokyo",
                ),
                ApiAirportSuggestion("DXB", "Dubai International", "Dubai", "AE", "Asia/Dubai"),
                ApiAirportSuggestion("BKK", "Suvarnabhumi", "Bangkok", "TH", "Asia/Bangkok"),
            )
        var form =
            WizardForm()
                .setRouteAirport(airports[0], 0)
                .setRouteAirport(airports[1], 1)
                .updateLeg(0) { it.copy(departLocal = "2026-10-12T14:40", arriveLocal = "2026-10-13T09:15") }
        var state =
            MemberWizardState(
                entered = true,
                fromLastTrip = false,
                sleepNeeded = false,
                airports = airports.associateBy { it.iata },
                profile =
                    JsonObject(
                        mapOf(
                            "habitual_bed" to JsonPrimitive("23:30"),
                            "habitual_wake" to JsonPrimitive("07:15"),
                            "chronotype" to JsonPrimitive("intermediate"),
                        ),
                    ),
            )
        if (board == "WizardWhere" || board == "WizardDiscard" ||
            board == "WizardWhereSame"
        ) {
            state = state.copy(fromLastTrip = true)
        }
        if (board == "WizardWhereEmpty" || board == "WizardAirportFrom") {
            form = WizardForm()
            state = state.copy(entered = false)
        } else if (board == "WizardWhereSame") {
            form = form.setRouteAirport(airports[0], 1)
        }
        if (board in listOf("WizardWhereStop", "WizardWhereStops", "WizardWhereLimit", "WizardStopSearchSecond")) {
            state = state.copy(fromLastTrip = true)
            form = form.insertStop(airports[2], 0)
            if (board == "WizardWhereStops") form = form.insertStop(airports[3], 1)
            if (board == "WizardWhereLimit") {
                while (form.legs.size < WizardForm.maxFlights) {
                    form = form.insertStop(airports[if (form.legs.size % 2 == 0) 3 else 2], form.returnBoundary - 1)
                }
            }
            form = form.copy(legs = form.legs.map { it.copy(departLocal = "", arriveLocal = "") })
        }
        if (board in listOf("WizardHomeRoute", "WizardHomeRouteStop", "WizardAirportHome", "WizardStopSearchHome")) {
            form = form.addReturn().setRouteAirport(airports[0], 1, home = true)
            state = state.copy(step = MemberWizardStep.returnRoute, returnRouteConfirmed = false)
            if (board == "WizardHomeRouteStop") form = form.insertStop(airports[2], 1)
        }
        state =
            state.copy(
                sheet =
                    when (board) {
                        "WizardAirportFrom" -> WizardAirportSheet(0, WizardAirportSheet.Side.from)
                        "WizardAirportHome" -> WizardAirportSheet(form.returnBoundary, WizardAirportSheet.Side.home)
                        "WizardStopSearch" -> WizardAirportSheet(0, WizardAirportSheet.Side.stop)
                        "WizardStopSearchSecond" -> WizardAirportSheet(1, WizardAirportSheet.Side.stop)
                        "WizardStopSearchHome" -> WizardAirportSheet(form.legs.lastIndex, WizardAirportSheet.Side.stop)
                        else -> null
                    },
            )
        return MemberWizardDraft(form, state)
    }
}

class WizardMemoryDraftStore : WizardDraftStore {
    var draft: MemberWizardDraft? = null
        private set

    override suspend fun persist(draft: MemberWizardDraft) {
        this.draft = draft
    }

    override suspend fun discard(instance: String) {
        if (draft?.state?.instance == instance) draft = null
    }

    override suspend fun didSave(
        instance: String,
        firstTrip: Boolean,
    ) {
        discard(instance)
    }
}
