package net.chronoflow.core.wizard.model

import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import net.chronoflow.core.model.contracts.ApiReviewIdentity
import net.chronoflow.core.model.contracts.ApiTripInput
import net.chronoflow.core.model.contracts.ApiTripInputBusy
import net.chronoflow.core.model.contracts.ApiTripInputJourney
import net.chronoflow.core.model.contracts.ApiTripInputLeg
import net.chronoflow.core.model.contracts.ApiTripInputOverride
import net.chronoflow.core.model.contracts.ApiTripInputSnapshot
import net.chronoflow.core.model.contracts.ApiTripInputStay

fun WizardForm.reviewInput(
    identity: ApiReviewIdentity?,
    profile: JsonObject? = null,
): ApiTripReviewInput {
    val inputs =
        legs.mapIndexed { index, leg ->
            val stay = stays.getOrElse(index) { WizardStay() }
            ApiTripInputLeg(
                fromIata = leg.fromIata,
                toIata = leg.toIata,
                departLocal = leg.departLocal,
                arriveLocal = leg.arriveLocal,
                flightNumber = leg.flightNumber,
                seatLieFlat = leg.seatLieFlat,
                seatWindow = leg.seatWindow,
                leaveForAirportLocal = leg.leaveForAirportLocal,
                airportTransferMin = leg.airportTransferMin,
                stay =
                    ApiTripInputStay(
                        bedAvailableMode = stay.bedAvailableMode,
                        bedAvailableFromLocal = stay.bedAvailableFromLocal,
                        checkOutLocal = stay.checkOutLocal,
                        lateCheckOutOrDayRoom = stay.lateCheckOutOrDayRoom,
                        arrivalTransferMin = stay.arrivalTransferMin,
                        transferMode = stay.transferMode,
                        busyIntervals = stay.busyIntervals.map { it.input },
                    ),
            )
        }
    return ApiTripReviewInput(
        input =
            ApiTripInput(
                outbound = ApiTripInputJourney(inputs.take(returnBoundary)),
                `return` = if (end.kind == "return_legs") ApiTripInputJourney(inputs.drop(returnBoundary)) else null,
                tripEndKind = end.kind,
                tripEndNights = end.nights,
                tripEndCheckOutDate = end.checkOutDate,
                preparationDays = preparation.days,
                fastDelayOptIn = preparation.fastDelayOptIn,
                override =
                    ApiTripInputOverride(
                        override.enabled,
                        override.habitualBed,
                        override.habitualWake,
                        override.chronotype,
                    ),
                latestWake = preparation.latestWake,
                feasibleNightBed = preparation.feasibleNightBed,
                feasibleNightWake = preparation.feasibleNightWake,
                preDepartureBusyAnswered = preparation.busyAnswered,
                preDepartureBusyIntervals = preparation.busyIntervals.map { it.input },
            ),
        identity = identity,
        profile = profile,
    )
}

fun WizardForm.Companion.from(
    snapshot: ApiTripInputSnapshot,
    retained: WizardForm? = null,
): WizardForm = from(snapshot.input, snapshot.airports, retained)

fun WizardForm.Companion.from(
    reviewInput: ApiTripReviewInput,
    retained: WizardForm? = null,
): WizardForm = from(reviewInput.input, retained = retained).copy(identity = reviewInput.identity)

fun WizardForm.Companion.from(
    input: ApiTripInput,
    airports: Map<String, ApiAirportSuggestion> = emptyMap(),
    retained: WizardForm? = null,
): WizardForm {
    fun check(
        value: String?,
        choices: List<String>,
    ) {
        if (value != null && value !in choices) throw DataFailure.Server(null)
    }
    check(input.tripEndKind, listOf("return_legs", "nights", "check_out", "one_way"))
    check(input.override.chronotype, listOf("early", "intermediate", "late"))
    val inputs = input.outbound.legs + (input.`return`?.legs ?: emptyList())
    val legs =
        inputs.mapIndexed { index, value ->
            check(value.seatWindow, listOf("left", "right"))
            val previous = retained?.legs?.getOrNull(index)
            WizardLeg(
                id = previous?.id ?: "leg-$index",
                fromCity = airports[value.fromIata]?.city ?: previous?.fromCity,
                toCity = airports[value.toIata]?.city ?: previous?.toCity,
                fromIata = value.fromIata,
                toIata = value.toIata,
                departLocal = value.departLocal,
                arriveLocal = value.arriveLocal,
                flightNumber = value.flightNumber,
                seatLieFlat = value.seatLieFlat,
                seatWindow = value.seatWindow,
                leaveForAirportLocal = value.leaveForAirportLocal,
                airportTransferMin = value.airportTransferMin,
            )
        }

    fun busy(
        values: List<ApiTripInputBusy>,
        retained: List<WizardBusy>,
    ): List<WizardBusy> =
        values.mapIndexed { index, value ->
            WizardBusy(
                retained.getOrNull(index)?.id ?: "busy-$index",
                value.startLocal,
                value.endLocal,
            )
        }
    val stays =
        inputs.mapIndexed { index, leg ->
            val value = leg.stay
            check(value.bedAvailableMode, listOf("arrival", "declared"))
            check(value.transferMode, listOf("passenger", "self_driven", "unknown"))
            WizardStay(
                bedAvailableMode = value.bedAvailableMode,
                bedAvailableFromLocal = value.bedAvailableFromLocal,
                checkOutLocal = value.checkOutLocal,
                lateCheckOutOrDayRoom = value.lateCheckOutOrDayRoom,
                arrivalTransferMin = value.arrivalTransferMin,
                transferMode = value.transferMode,
                busyIntervals =
                    busy(
                        value.busyIntervals,
                        retained?.stays?.getOrNull(index)?.busyIntervals ?: emptyList(),
                    ),
            )
        }
    return WizardForm(
        legs = legs,
        stays = stays,
        end =
            WizardEnd(
                input.tripEndKind,
                input.tripEndNights,
                input.tripEndCheckOutDate,
                if (input.tripEndKind == "return_legs") input.outbound.legs.size else null,
            ),
        override =
            WizardSleepOverride(
                input.override.use,
                input.override.habitualBed,
                input.override.habitualWake,
                input.override.chronotype,
            ),
        preparation =
            WizardPreparation(
                input.preparationDays,
                input.fastDelayOptIn,
                input.latestWake,
                input.feasibleNightBed,
                input.feasibleNightWake,
                input.preDepartureBusyAnswered,
                busy(input.preDepartureBusyIntervals, retained?.preparation?.busyIntervals ?: emptyList()),
            ),
        identity = retained?.identity,
        itinerary = retained?.itinerary,
    )
}
