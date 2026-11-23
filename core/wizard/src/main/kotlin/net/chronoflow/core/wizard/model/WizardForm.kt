package net.chronoflow.core.wizard.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import net.chronoflow.core.model.contracts.ApiReviewIdentity
import net.chronoflow.core.model.contracts.ApiReviewItinerary
import net.chronoflow.core.model.contracts.ApiTripInputBusy
import net.chronoflow.core.model.contracts.ObjectSerializer
import java.util.UUID

@Serializable
data class WizardLeg(
    val id: String = UUID.randomUUID().toString(),
    val fromIata: String = "",
    val fromCity: String? = null,
    val toIata: String = "",
    val toCity: String? = null,
    val departLocal: String = "",
    val arriveLocal: String = "",
    val flightNumber: String? = null,
    val seatLieFlat: Boolean = false,
    val seatWindow: String? = null,
    val leaveForAirportLocal: String? = null,
    val airportTransferMin: Int? = null,
)

@Serializable
data class WizardBusy(
    val id: String = UUID.randomUUID().toString(),
    val startLocal: String,
    val endLocal: String,
) {
    val input: ApiTripInputBusy get() = ApiTripInputBusy(startLocal, endLocal)
}

@Serializable(with = WizardStay.Serializer::class)
data class WizardStay(
    val bedAvailableMode: String? = "arrival",
    val bedAvailableFromLocal: String? = null,
    val checkOutLocal: String? = null,
    val lateCheckOutOrDayRoom: Boolean = false,
    val arrivalTransferMin: Int? = null,
    val transferMode: String? = null,
    val busyIntervals: List<WizardBusy> = emptyList(),
) {
    object Serializer : ObjectSerializer<WizardStay>("net.chronoflow.core.wizard.model.WizardStay") {
        override fun decode(
            json: Json,
            obj: JsonObject,
        ): WizardStay {
            val value = json.decodeFromJsonElement(WizardStayWire.serializer(), obj)
            return WizardStay(
                value.bedAvailableMode,
                value.bedAvailableFromLocal,
                value.checkOutLocal,
                value.lateCheckOutOrDayRoom,
                value.arrivalTransferMin,
                value.transferMode,
                value.busyIntervals,
            )
        }

        override fun encode(
            json: Json,
            value: WizardStay,
        ): JsonObject {
            val wire =
                WizardStayWire(
                    value.bedAvailableMode,
                    value.bedAvailableFromLocal,
                    value.checkOutLocal,
                    value.lateCheckOutOrDayRoom,
                    value.arrivalTransferMin,
                    value.transferMode,
                    value.busyIntervals,
                )
            val result = json.encodeToJsonElement(WizardStayWire.serializer(), wire) as JsonObject
            // Omitting null would decode as the non-null default and change a recovered answer.
            return if (value.bedAvailableMode == null) JsonObject(result + ("bedAvailableMode" to JsonNull)) else result
        }
    }
}

@Serializable
private data class WizardStayWire(
    val bedAvailableMode: String? = "arrival",
    val bedAvailableFromLocal: String? = null,
    val checkOutLocal: String? = null,
    val lateCheckOutOrDayRoom: Boolean = false,
    val arrivalTransferMin: Int? = null,
    val transferMode: String? = null,
    val busyIntervals: List<WizardBusy> = emptyList(),
)

@Serializable
data class WizardEnd(
    val kind: String = "one_way",
    val nights: Int? = null,
    val checkOutDate: String? = null,
    val returnFromLeg: Int? = null,
)

@Serializable
data class WizardSleepOverride(
    val enabled: Boolean = false,
    val habitualBed: String? = null,
    val habitualWake: String? = null,
    val chronotype: String? = null,
)

@Serializable
data class WizardPreparation(
    val days: Int = 3,
    val fastDelayOptIn: Boolean = false,
    val latestWake: String? = null,
    val feasibleNightBed: String? = null,
    val feasibleNightWake: String? = null,
    val busyAnswered: Boolean = false,
    val busyIntervals: List<WizardBusy> = emptyList(),
)

@Serializable
data class WizardForm(
    val legs: List<WizardLeg> = listOf(WizardLeg()),
    val stays: List<WizardStay> = listOf(WizardStay()),
    val end: WizardEnd = WizardEnd(),
    val override: WizardSleepOverride = WizardSleepOverride(),
    val preparation: WizardPreparation = WizardPreparation(),
    val identity: ApiReviewIdentity? = null,
    val itinerary: ApiReviewItinerary? = null,
) {
    @Suppress("ktlint:standard:property-naming")
    companion object {
        const val maxFlights = 16
        const val maxPreparationDays = 4
    }

    val returnBoundary: Int get() = if (end.kind == "return_legs") end.returnFromLeg ?: legs.size else legs.size

    fun updateLeg(
        index: Int,
        update: (WizardLeg) -> WizardLeg,
    ): WizardForm {
        if (index !in legs.indices) return this
        val before = legs[index]
        val updated = legs.toMutableList()
        updated[index] = update(before)
        if (end.kind == "return_legs" && index == returnBoundary - 1 && returnBoundary in legs.indices &&
            (updated[index].toIata != before.toIata || updated[index].toCity != before.toCity)
        ) {
            updated[returnBoundary] =
                updated[returnBoundary].copy(
                    fromIata = updated[index].toIata,
                    fromCity = updated[index].toCity,
                )
        }
        return copy(legs = updated, itinerary = null)
    }

    fun addFlight(home: Boolean = false): WizardForm {
        if (legs.size >= maxFlights || (home && end.kind != "return_legs")) return this
        val index = if (home) legs.size else returnBoundary
        val previous = legs[index - 1]
        return copy(
            legs =
                legs.toMutableList().apply {
                    add(
                        index,
                        WizardLeg(fromIata = previous.toIata, fromCity = previous.toCity),
                    )
                },
            stays = stays.toMutableList().apply { add(index, WizardStay()) },
            end = if (end.kind == "return_legs" && !home) end.copy(returnFromLeg = returnBoundary + 1) else end,
            itinerary = null,
        )
    }

    data class RemovedFlight(
        val index: Int,
        val outbound: Boolean,
        val leg: WizardLeg,
        val stay: WizardStay,
    )

    data class TakenFlight(
        val form: WizardForm,
        val removed: RemovedFlight,
    )

    fun takeFlight(index: Int): TakenFlight? {
        val boundary = returnBoundary
        val outbound = index < boundary
        if (index !in legs.indices || (if (outbound) boundary else legs.size - boundary) <= 1) return null
        val removed = RemovedFlight(index, outbound, legs[index], stays[index])
        return TakenFlight(
            copy(
                legs = legs.filterIndexed { i, _ -> i != index },
                stays = stays.filterIndexed { i, _ -> i != index },
                end = if (end.kind == "return_legs" && outbound) end.copy(returnFromLeg = boundary - 1) else end,
                itinerary = null,
            ),
            removed,
        )
    }

    fun restoreFlight(removed: RemovedFlight): WizardForm {
        if (legs.any { it.id == removed.leg.id } || legs.size >= maxFlights ||
            (!removed.outbound && end.kind != "return_legs")
        ) {
            return this
        }
        val index =
            if (removed.outbound) {
                minOf(
                    removed.index,
                    returnBoundary,
                )
            } else {
                minOf(legs.size, maxOf(removed.index, returnBoundary))
            }
        return copy(
            legs = legs.toMutableList().apply { add(index, removed.leg) },
            stays = stays.toMutableList().apply { add(index, removed.stay) },
            end =
                if (end.kind == "return_legs" &&
                    removed.outbound
                ) {
                    end.copy(returnFromLeg = returnBoundary + 1)
                } else {
                    end
                },
            itinerary = null,
        )
    }

    data class RetainedReturn(
        val legs: List<WizardLeg>,
        val stays: List<WizardStay>,
    )

    data class ReturnRemoval(
        val form: WizardForm,
        val retained: RetainedReturn,
    )

    fun addReturn(retained: RetainedReturn? = null): WizardForm {
        if (end.kind == "return_legs" || legs.size + (retained?.legs?.size ?: 1) > maxFlights) return this
        val last = legs.lastOrNull() ?: return this
        val returned = retained ?: RetainedReturn(listOf(WizardLeg()), listOf(WizardStay()))
        if (returned.legs.isEmpty()) return this
        return copy(
            legs =
                legs +
                    returned.legs.mapIndexed { index, leg ->
                        if (index == 0) leg.copy(fromIata = last.toIata, fromCity = last.toCity) else leg
                    },
            stays = stays + returned.stays,
            end = end.copy(kind = "return_legs", returnFromLeg = legs.size),
            itinerary = null,
        )
    }

    fun removeReturn(): ReturnRemoval {
        val boundary = returnBoundary
        return ReturnRemoval(
            copy(
                legs = legs.take(boundary),
                stays = stays.take(boundary),
                end =
                    end.copy(
                        kind =
                            when {
                                end.nights != null -> "nights"
                                end.checkOutDate != null -> "check_out"
                                else -> "one_way"
                            },
                        returnFromLeg = null,
                    ),
                itinerary = null,
            ),
            RetainedReturn(legs.drop(boundary), stays.drop(boundary)),
        )
    }

    fun insertStop(
        airport: ApiAirportSuggestion,
        beforeDestinationOf: Int,
    ): WizardForm {
        val index = beforeDestinationOf
        if (index !in legs.indices || legs.size >= maxFlights) return this
        val original = legs[index]
        val next =
            WizardLeg(
                fromIata = airport.iata,
                fromCity = airport.city,
                toIata = original.toIata,
                toCity = original.toCity,
            )
        val updated = legs.toMutableList()
        updated[index] = original.copy(toIata = airport.iata, toCity = airport.city, arriveLocal = "")
        updated.add(index + 1, next)
        return copy(
            legs = updated,
            stays = stays.toMutableList().apply { add(index, WizardStay()) },
            end =
                if (end.kind == "return_legs" &&
                    index < returnBoundary
                ) {
                    end.copy(returnFromLeg = returnBoundary + 1)
                } else {
                    end
                },
            itinerary = null,
        )
    }

    fun removeStop(at: Int): WizardForm {
        if (at <= 0 || at !in legs.indices || at == returnBoundary) return this
        val last = legs[at]
        val updated = legs.toMutableList()
        updated[at - 1] =
            updated[at - 1].copy(toIata = last.toIata, toCity = last.toCity, arriveLocal = last.arriveLocal)
        updated.removeAt(at)
        return copy(
            legs = updated,
            stays = stays.filterIndexed { index, _ -> index != at - 1 },
            end =
                if (end.kind == "return_legs" &&
                    at < returnBoundary
                ) {
                    end.copy(returnFromLeg = returnBoundary - 1)
                } else {
                    end
                },
            itinerary = null,
        )
    }
}
