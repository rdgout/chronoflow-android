package net.chronoflow.feature.wizard.debug

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.FieldMessage
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
            "WizardOutbound",
            "WizardOutboundDark",
            "WizardOutboundNL",
            "WizardOutboundEmpty",
            "WizardOutboundEmptyDarkNL",
            "WizardOutboundErrors",
            "WizardOutboundErrorsDarkNL",
            "WizardOutboundTwoErrors",
            "WizardOutboundTwoErrorsDarkNL",
            "WizardOutboundStop",
            "WizardOutboundStopDarkNL",
            "WizardOutboundStopEmpty",
            "WizardOutboundStopEmptyDarkNL",
            "WizardOutboundOvernight",
            "WizardOutboundOvernightDarkNL",
            "WizardOutboundThree",
            "WizardOutboundThreeDarkNL",
            "WizardOutboundPast",
            "WizardOutboundPastDarkNL",
            "WizardOutboundScrollErrors",
            "WizardReturn",
            "WizardReturnDark",
            "WizardReturnNL",
            "WizardReturnFlight",
            "WizardReturnFlightDarkNL",
            "WizardReturnFlightPast",
            "WizardReturnFlightPastDarkNL",
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
        if (board == "WizardWhere" ||
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
        val errors = mutableMapOf<String, FieldMessage>()
        if (board.startsWith("WizardOutbound") || board == "WizardDiscard" || board == "WizardReturn") {
            state = state.copy(step = MemberWizardStep.outbound)
        }
        if (board == "WizardOutboundEmpty") {
            form = form.updateLeg(0) { it.copy(departLocal = "", arriveLocal = "") }
        }
        if (board == "WizardOutboundErrors") {
            form = form.updateLeg(0) { it.copy(arriveLocal = "2026-10-11T09:15") }
            errors["legs.0.arriveLocal"] = FieldMessage("Check the departure and arrival times.")
        }
        if (board in
            listOf(
                "WizardOutboundStop",
                "WizardOutboundStopEmpty",
                "WizardOutboundOvernight",
                "WizardOutboundThree",
                "WizardOutboundTwoErrors",
                "WizardOutboundScrollErrors",
            )
        ) {
            form = form.insertStop(airports[2], 0)
            if (board != "WizardOutboundStopEmpty") {
                val overnight = board == "WizardOutboundOvernight"
                form =
                    form
                        .updateLeg(
                            0,
                        ) { it.copy(arriveLocal = if (overnight) "2026-10-12T22:25" else "2026-10-12T23:25") }
                        .updateLeg(1) {
                            it.copy(
                                departLocal = if (overnight) "2026-10-14T02:05" else "2026-10-13T02:05",
                                arriveLocal = if (overnight) "2026-10-14T16:35" else "2026-10-13T16:35",
                            )
                        }
            }
        }
        if (board == "WizardOutboundStopEmpty") form = form.updateLeg(0) { it.copy(departLocal = "") }
        if (board == "WizardOutboundThree") {
            form = form.insertStop(airports[3], 1).updateLeg(1) { it.copy(arriveLocal = "2026-10-13T11:20") }
        }
        if (board == "WizardOutboundTwoErrors") {
            form = form.updateLeg(1) { it.copy(arriveLocal = "2026-10-12T16:35") }
            errors["legs.1.arriveLocal"] = FieldMessage("Check the departure and arrival times.")
        }
        if (board == "WizardOutboundScrollErrors") {
            form =
                form
                    .insertStop(airports[3], 1)
                    .insertStop(airports[2], 2)
                    .insertStop(airports[3], 3)
                    .updateLeg(0) { it.copy(arriveLocal = "2026-10-11T23:25") }
                    .updateLeg(1) { it.copy(arriveLocal = "2026-10-13T11:20") }
                    .updateLeg(2) { it.copy(departLocal = "2026-10-13T13:00", arriveLocal = "2026-10-13T17:00") }
                    .updateLeg(3) { it.copy(departLocal = "2026-10-13T19:00", arriveLocal = "2026-10-14T04:00") }
                    .updateLeg(4) { it.copy(departLocal = "2026-10-14T03:00", arriveLocal = "2026-10-14T14:30") }
            errors["legs.0.arriveLocal"] = FieldMessage("Check the departure and arrival times.")
            errors["legs.4.departLocal"] = FieldMessage("This flight leaves before the previous one lands.")
        }
        if (board == "WizardReturn") state = state.copy(step = MemberWizardStep.returnChoice)
        if (board.startsWith("WizardReturnFlight")) {
            form = form.addReturn().setRouteAirport(airports[0], 1, home = true)
            state = state.copy(step = MemberWizardStep.returnFlight, returnRouteConfirmed = true)
            if (board == "WizardReturnFlightPast") {
                form = form.updateLeg(1) { it.copy(departLocal = "2026-10-22T10:25", arriveLocal = "2026-10-22T15:30") }
                errors["review"] = FieldMessage("A new plan needs an upcoming arrival. Check your flight dates.")
            }
        }
        if (board == "WizardOutboundPast") {
            form = form.updateLeg(0) { it.copy(departLocal = "2026-09-02T14:40", arriveLocal = "2026-09-03T09:15") }
            errors["review"] =
                FieldMessage("A new plan needs an upcoming arrival. Check your flight dates or add your return flight.")
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
        return MemberWizardDraft(form, state, errors)
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
