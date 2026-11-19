@file:Suppress("ktlint:standard:filename", "ktlint:standard:max-line-length")

package net.chronoflow.core.datamock

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.AccountSettings
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.TripWizardErrors
import net.chronoflow.core.model.contracts.AdaptationDirection
import net.chronoflow.core.model.contracts.AdaptationGoal
import net.chronoflow.core.model.contracts.AirportRef
import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import net.chronoflow.core.model.contracts.ApiReviewIdentity
import net.chronoflow.core.model.contracts.ApiReviewItinerary
import net.chronoflow.core.model.contracts.ApiReviewLayover
import net.chronoflow.core.model.contracts.ApiReviewLeg
import net.chronoflow.core.model.contracts.ApiReviewPrice
import net.chronoflow.core.model.contracts.ApiReviewPrompt
import net.chronoflow.core.model.contracts.ApiReviewPromptScope
import net.chronoflow.core.model.contracts.ApiReviewPromptShape
import net.chronoflow.core.model.contracts.ApiReviewSummary
import net.chronoflow.core.model.contracts.ApiStayEnd
import net.chronoflow.core.model.contracts.ApiTripInput
import net.chronoflow.core.model.contracts.ApiTripInputJourney
import net.chronoflow.core.model.contracts.ApiTripInputLeg
import net.chronoflow.core.model.contracts.ApiTripInputOverride
import net.chronoflow.core.model.contracts.ApiTripInputSnapshot
import net.chronoflow.core.model.contracts.ApiTripInputStay
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.model.contracts.AssumedReturnBasis
import net.chronoflow.core.model.contracts.BlockParams
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CopyArg
import net.chronoflow.core.model.contracts.CopyArgKind
import net.chronoflow.core.model.contracts.CopyArgValue
import net.chronoflow.core.model.contracts.CopyArgument
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.ObjectSerializer
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.model.contracts.ProfilePreferences
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.contracts.ReviewAnswerShape
import net.chronoflow.core.model.contracts.ReviewCreditState
import net.chronoflow.core.model.contracts.ReviewPromptTarget
import net.chronoflow.core.model.contracts.SegmentKindCode
import net.chronoflow.core.model.contracts.SummaryView
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.contracts.TripDetailLegView
import net.chronoflow.core.model.contracts.TripDetailView
import net.chronoflow.core.model.contracts.TripDirection
import net.chronoflow.core.model.contracts.TripListItem
import net.chronoflow.core.model.contracts.TripProgress
import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.model.contracts.objectAt
import net.chronoflow.core.model.contracts.optionalStringAt
import net.chronoflow.core.model.contracts.require
import net.chronoflow.core.model.contracts.stringAt
import net.chronoflow.core.testing.MockTripTemplates
import net.chronoflow.core.testing.Template
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@Serializable(with = MockWizardDraft.Serializer::class)
@ConsistentCopyVisibility
data class MockWizardDraft private constructor(
    val owner: String,
    val tripID: String?,
    val input: ApiTripInput,
    val review: ApiTripReview,
    val profile: JsonObject,
    val expiresAt: Double,
) {
    companion object {
        operator fun invoke(
            owner: String,
            tripID: String?,
            input: ApiTripInput,
            review: ApiTripReview,
            profile: JsonObject,
            expiresAt: Double,
        ): MockWizardDraft =
            MockWizardDraft(owner, tripID, input, review, ProfilePreferences.bodyClock(profile), expiresAt)
    }

    override fun toString(): String = "MockWizardDraft(***)"

    object Serializer : ObjectSerializer<MockWizardDraft>("net.chronoflow.core.datamock.MockWizardDraft") {
        override fun decode(
            json: Json,
            obj: JsonObject,
        ): MockWizardDraft =
            MockWizardDraft(
                obj.stringAt("owner"),
                obj.optionalStringAt("tripID"),
                json.decodeFromJsonElement(ApiTripInput.serializer(), obj.require("input")),
                json.decodeFromJsonElement(ApiTripReview.serializer(), obj.require("review")),
                ProfilePreferences.bodyClock(obj.objectAt("profile")),
                (obj.require("expiresAt") as? JsonPrimitive)?.takeIf { !it.isString }?.doubleOrNull
                    ?: throw SerializationException("expiresAt must be a number"),
            )

        override fun encode(
            json: Json,
            value: MockWizardDraft,
        ): JsonObject =
            JsonObject(
                buildMap {
                    put("owner", JsonPrimitive(value.owner))
                    value.tripID?.let { put("tripID", JsonPrimitive(it)) }
                    put("input", json.encodeToJsonElement(ApiTripInput.serializer(), value.input))
                    put("review", json.encodeToJsonElement(ApiTripReview.serializer(), value.review))
                    put("profile", ProfilePreferences.bodyClock(value.profile))
                    put("expiresAt", JsonPrimitive(value.expiresAt))
                },
            )
    }
}

@Serializable
data class MockWizardReceipt(
    val intent: String,
    val tripID: String,
    val expiresAt: Double,
) {
    override fun toString(): String = "MockWizardReceipt(***)"
}

object MockTripGeneration {
    fun airports(): List<ApiAirportSuggestion> {
        val names =
            mapOf(
                "AMS" to ("Amsterdam Airport Schiphol" to "NL"),
                "HND" to ("Tokyo Haneda Airport" to "JP"),
                "JFK" to ("John F. Kennedy International Airport" to "US"),
                "SIN" to ("Singapore Changi Airport" to "SG"),
                "SYD" to ("Sydney Kingsford Smith Airport" to "AU"),
                "LHR" to ("London Heathrow Airport" to "GB"),
                "DXB" to ("Dubai International Airport" to "AE"),
                "LAX" to ("Los Angeles International Airport" to "US"),
            )
        val airports = linkedMapOf<String, ApiAirportSuggestion>()
        for (airport in MockTripTemplates.templates().flatMap {
            it.detail.trip.legs.flatMap { leg ->
                listOf(leg.from, leg.to)
            }
        }) {
            val name = names[airport.iata] ?: continue
            airports[airport.iata] =
                ApiAirportSuggestion(airport.iata, name.first, airport.city, name.second, airport.zone)
        }
        return airports.values.sortedBy { it.iata }
    }

    private val localFormat =
        DateTimeFormatter
            .ofPattern(
                "uuuu-MM-dd'T'HH:mm",
                Locale.ROOT,
            ).withResolverStyle(ResolverStyle.STRICT)

    fun instant(
        local: String,
        airport: ApiAirportSuggestion,
    ): Instant {
        val date =
            try {
                LocalDateTime
                    .parse(local, localFormat)
                    .atZone(ZoneId.of(airport.timezone))
                    .withLaterOffsetAtOverlap()
            } catch (
                _: DateTimeException,
            ) {
                null
            }
        if (date == null ||
            localFormat.format(date) != local
        ) {
            throw MockSettings.invalid("review", "Check the departure and arrival times.")
        }
        return date.toInstant()
    }

    fun itinerary(input: ApiTripInput): ApiReviewItinerary {
        val airports = airports()
        val legs = mutableListOf<ApiReviewLeg>()
        for ((direction, journey) in listOf(
            TripDirection.Outbound to input.outbound,
            TripDirection.Return to input.`return`,
        )) {
            if (journey == null) continue
            if (journey.legs.isEmpty()) throw MockSettings.invalid("${direction.raw}.legs", "Please fill this in.")
            for ((index, leg) in journey.legs.withIndex()) {
                val prefix = "${direction.raw}.legs.$index"
                val from =
                    airports.firstOrNull { it.iata == leg.fromIata }
                        ?: throw MockSettings.invalid("$prefix.from_iata", "Choose an airport from the search results.")
                val to =
                    airports.firstOrNull { it.iata == leg.toIata }
                        ?: throw MockSettings.invalid("$prefix.to_iata", "Choose an airport from the search results.")
                val depart = instant(leg.departLocal, from)
                val arrive = instant(leg.arriveLocal, to)
                if (arrive <=
                    depart
                ) {
                    throw MockSettings.invalid("$prefix.arrive_local", "Check the departure and arrival times.")
                }
                legs +=
                    ApiReviewLeg(
                        direction,
                        index,
                        iso(depart),
                        iso(arrive),
                        java.time.Duration
                            .between(depart, arrive)
                            .toMinutes()
                            .toInt(),
                    )
            }
        }
        return ApiReviewItinerary(legs, layovers(input, legs, airports))
    }

    fun template(input: ApiTripInput): Template {
        val templates = MockTripTemplates.templates()
        val first = input.outbound.legs.firstOrNull() ?: throw DataFailure.ReviewExpired
        val allLegs = input.outbound.legs + input.`return`?.legs.orEmpty()

        fun score(template: Template): Int {
            val matches =
                template.detail.trip.legs.maxOfOrNull {
                    (if (it.from.iata == first.fromIata) 1 else 0) +
                        (if (it.to.iata == first.toIata) 1 else 0)
                }
                    ?: 0
            val others =
                allLegs.drop(1).count { leg ->
                    template.detail.trip.legs.any {
                        it.from.iata == leg.fromIata &&
                            it.to.iata == leg.toIata
                    }
                }
            return matches * 100 + others
        }
        val template =
            templates.drop(1).fold(templates.first()) { best, candidate ->
                if (score(candidate) >
                    score(best)
                ) {
                    candidate
                } else {
                    best
                }
            }
        val airport = airports().firstOrNull { it.iata == first.fromIata } ?: throw DataFailure.ReviewExpired
        val target = instant(first.departLocal, airport)
        val original =
            instant(
                template.card.departLocal,
                ApiAirportSuggestion(template.card.from.iata, "", template.card.from.city, "", template.card.from.zone),
            )
        val zone = ZoneId.of(airport.timezone)
        val days =
            ChronoUnit.DAYS
                .between(
                    original.atZone(zone).toLocalDate(),
                    target.atZone(zone).toLocalDate(),
                ).toInt()
        val moved = original.atZone(zone).plusDays(days.toLong()).toInstant()
        return MockTripFixtures.rebase(
            template,
            days,
            java.time.Duration
                .between(moved, target)
                .toMinutes()
                .toInt(),
        )
    }

    fun prompts(profile: JsonObject): List<ApiReviewPrompt> {
        val fields =
            listOf(
                "age_band" to "Which age group are you in?",
                "shift_work" to
                    "In the past 14 days, have you worked night or rotating shifts, or had a very irregular sleep schedule?",
            )
        return fields.mapNotNull { (field, text) ->
            val value = profile[TripWizardErrors.camel(field)] ?: JsonNull
            if (value != JsonNull && value.string != "unanswered") return@mapNotNull null
            val age = field == "age_band"
            ApiReviewPrompt(
                id = if (age) "age_band:::" else "screening:$field::",
                kind = if (age) PromptKind.AgeBand else PromptKind.Screening,
                blocking = true,
                required = true,
                text = CopyLine(text, emptyList()),
                scope = ApiReviewPromptScope(null, null, null),
                target = ReviewPromptTarget.Profile,
                fields = listOf(field),
                answer = mapOf(field to (value as JsonPrimitive)),
                shape =
                    ApiReviewPromptShape(
                        if (age) ReviewAnswerShape.Options else ReviewAnswerShape.Boolean,
                        if (age) listOf("under_18", "18_64", "65_plus") else emptyList(),
                        null,
                        null,
                    ),
                side = null,
                deviceMin = null,
            )
        }
    }

    fun review(
        input: ApiTripInput,
        identity: ApiReviewIdentity,
        settings: AccountSettings,
        edit: Boolean,
        paidDirections: List<TripDirection>,
    ): ApiTripReview {
        val itinerary = itinerary(input)
        val template = template(input)
        val profile = settings.bodyClock
        val excluded = profile["ageBand"].string == "under_18" || profile["shiftWork"].bool == true
        val blocking = if (excluded) emptyList() else prompts(profile)
        val prompts =
            if (excluded) {
                emptyList()
            } else if (blocking.isEmpty()) {
                MockWizardPrompts.make(input, profile)
            } else {
                blocking
            }
        val status =
            if (excluded) {
                PlanStatus.NoPlan
            } else if (blocking.isEmpty()) {
                template.detail.plan.status
            } else {
                PlanStatus.NeedsAnswers
            }
        val planned = status == PlanStatus.Ok || status == PlanStatus.Modified
        val unlimited = settings.planCredits["unlimited"].bool == true
        val balance = settings.planCredits["credits"].number?.toInt() ?: 0
        val directions =
            if (input.`return` ==
                null
            ) {
                listOf(TripDirection.Outbound)
            } else {
                listOf(TripDirection.Outbound, TripDirection.Return)
            }
        val free =
            template.detail.plan.summary
                ?.adaptation
                ?.goal == AdaptationGoal.Routine
        val cost = if (unlimited || free) 0 else directions.count { it !in paidDirections }
        val price =
            if (planned) {
                ApiReviewPrice(
                    state =
                        if (unlimited) {
                            ReviewCreditState.Unlimited
                        } else if (cost ==
                            0
                        ) {
                            if (edit) ReviewCreditState.Paid else ReviewCreditState.Free
                        } else if (edit) {
                            ReviewCreditState.Edit
                        } else {
                            ReviewCreditState.Create
                        },
                    cost = cost,
                    balance = balance,
                    unlimited = unlimited,
                    freeDirections = if (free) directions else emptyList(),
                )
            } else {
                null
            }
        val plan = template.detail.plan
        val value = plan.summary
        val first = plan.days.firstOrNull()
        val last = plan.days.lastOrNull()
        val summary =
            if (planned && value != null && first != null &&
                last != null
            ) {
                ApiReviewSummary(
                    summaryLine(value, input.`return` != null),
                    value.adaptation,
                    value.preparation.emitted,
                    first.startUtc,
                    first.zones.firstOrNull()?.tz ?: template.card.from.zone,
                    last.labelDate,
                    value.endCopy,
                    stayEnd(input),
                )
            } else {
                null
            }
        val reasons =
            if (excluded) {
                listOf(
                    CopyLine(
                        if (profile["ageBand"].string ==
                            "under_18"
                        ) {
                            "This plan is not available for travellers under 18."
                        } else {
                            "This plan is not available if, in the past 14 days, you have worked night or rotating shifts or had a very irregular sleep schedule."
                        },
                        emptyList(),
                    ),
                )
            } else if (blocking.isNotEmpty()) {
                listOf(CopyLine("Answer the questions to get your plan.", emptyList()))
            } else {
                plan.reasons
            }
        return ApiTripReview(
            identity,
            status,
            reasons,
            prompts,
            summary,
            price,
            planned && balance >= cost,
            if (planned &&
                balance < cost
            ) {
                "Not enough credits"
            } else {
                null
            },
            itinerary,
        )
    }

    fun saved(
        draft: MockWizardDraft,
        tripID: String,
    ): Triple<TripListItem, TripDetail, ApiTripInputSnapshot> {
        val template = template(draft.input)
        val lookup = airports().associateBy { it.iata }

        fun ref(iata: String): AirportRef {
            val airport = lookup[iata] ?: throw DataFailure.ReviewExpired
            return AirportRef(iata, airport.city, airport.timezone)
        }
        val input = draft.input
        val legs = input.outbound.legs + input.`return`?.legs.orEmpty()
        val first = legs.firstOrNull() ?: throw DataFailure.ReviewExpired
        val outboundLast = input.outbound.legs.lastOrNull() ?: throw DataFailure.ReviewExpired
        val start =
            template.detail.plan.days
                .firstOrNull() ?: throw DataFailure.ReviewExpired
        val end =
            template.detail.plan.days
                .lastOrNull() ?: throw DataFailure.ReviewExpired
        val card =
            template.card.copy(
                id = tripID,
                from = ref(first.fromIata),
                to = ref(outboundLast.toIata),
                departLocal = first.departLocal,
                arriveLocal = outboundLast.arriveLocal,
                departureUtc =
                    draft.review.itinerary.legs
                        .first()
                        .departUtc,
                arrivalUtc =
                    draft.review.itinerary.legs[
                        input.outbound.legs.size -
                            1,
                    ].arriveUtc,
                flightNumber = first.flightNumber,
                connections =
                    input.outbound.legs
                        .dropLast(1)
                        .map { ref(it.toIata) },
                progress = TripProgress(TripStatus.Upcoming, null),
            )
        var detail =
            template.detail.copy(
                trip =
                    TripDetailView(
                        tripID,
                        card.from,
                        card.to,
                        card.departLocal,
                        card.departureUtc,
                        end.endUtc,
                        card.offsetDifferenceMinutes,
                        legs.mapIndexed { index, leg ->
                            TripDetailLegView(
                                index,
                                leg.flightNumber,
                                ref(leg.fromIata),
                                ref(leg.toIata),
                                index >= input.outbound.legs.size,
                            )
                        },
                    ),
            )
        val material =
            canonical(ApiJson.response.encodeToJsonElement(ApiTripInput.serializer(), draft.input)) +
                canonical(ApiJson.response.encodeToJsonElement(ApiReviewIdentity.serializer(), draft.review.identity))
        val hash = sha256Hex(material)
        detail =
            detail.copy(
                plan = detail.plan.copy(meta = detail.plan.meta.copy(inputHash = hash)),
                checkState = detail.checkState.copy(planInputHash = hash, marks = emptyList()),
                detailVersion = hash,
            )
        val item =
            TripListItem(
                card,
                hash,
                start.startUtc,
                start.zones.first().tz,
                end.endUtc,
                end.labelDate,
                detail.plan.summary
                    ?.preparation
                    ?.emitted ?: 0,
                detail.plan.summary?.adaptation,
                true,
                hash,
                detail.checkState,
            )
        val used = legs.flatMap { listOf(it.fromIata, it.toIata) }.toSet()
        return Triple(item, detail, ApiTripInputSnapshot(input, hash, lookup.filterKeys { it in used }, stayEnd(input)))
    }

    internal fun canonical(value: JsonElement): String {
        fun sorted(entry: JsonElement): JsonElement =
            when (entry) {
                is JsonObject -> JsonObject(entry.toSortedMap().mapValues { sorted(it.value) })
                is JsonArray -> JsonArray(entry.map(::sorted))
                else -> entry
            }
        return ApiJson.response.encodeToString(JsonElement.serializer(), sorted(value))
    }

    fun snapshot(detail: TripDetail): ApiTripInputSnapshot {
        val lookup = airports().associateBy { it.iata }
        val outbound = mutableListOf<ApiTripInputLeg>()
        val returning = mutableListOf<ApiTripInputLeg>()
        val flights = detail.plan.blocks.filter { it.type == BlockType.Flight }
        for (leg in detail.trip.legs) {
            val flight =
                flights.firstOrNull { (it.params as? BlockParams.Flight)?.value?.legIndex == leg.position } ?: continue
            val input =
                ApiTripInputLeg(
                    leg.from.iata,
                    leg.to.iata,
                    MockTripFixtures.local(flight.startUtc, leg.from.zone),
                    MockTripFixtures.local(flight.endUtc, leg.to.zone),
                    leg.flightNumber,
                    false,
                    null,
                    null,
                    null,
                    ApiTripInputStay(null, null, null, false, null, null, emptyList()),
                )
            if (leg.returnLeg) returning += input else outbound += input
        }
        var input =
            ApiTripInput(
                ApiTripInputJourney(outbound),
                if (returning.isEmpty()) null else ApiTripInputJourney(returning),
                if (returning.isEmpty()) "one_way" else "return_legs",
                null,
                null,
                detail.plan.summary
                    ?.preparation
                    ?.days ?: 0,
                false,
                ApiTripInputOverride(false, null, null, null),
                null,
                null,
                null,
                false,
                emptyList(),
            )
        val end = detail.plan.meta.assumedReturn
        if (returning.isEmpty() && end != null) {
            if (end.basis ==
                AssumedReturnBasis.Nights
            ) {
                input = input.copy(tripEndKind = "nights", tripEndNights = end.nights)
            } else if (end.basis ==
                AssumedReturnBasis.CheckOut
            ) {
                input =
                    input.copy(
                        tripEndKind = "check_out",
                        tripEndCheckOutDate = MockTripFixtures.local(end.leaveUtc, detail.trip.to.zone).take(10),
                    )
            }
        }
        val used =
            detail.trip.legs
                .flatMap { listOf(it.from.iata, it.to.iata) }
                .toSet()
        return ApiTripInputSnapshot(input, detail.plan.meta.inputHash, lookup.filterKeys { it in used }, stayEnd(input))
    }

    fun summaryLine(
        summary: SummaryView,
        withReturn: Boolean,
    ): CopyLine {
        val adaptation = summary.adaptation
        if (adaptation.goal ==
            AdaptationGoal.HomeTime
        ) {
            return CopyLine("Your plan keeps your clock on your home time.", emptyList())
        }
        if (adaptation.goal == AdaptationGoal.Routine ||
            adaptation.direction == AdaptationDirection.None
        ) {
            return CopyLine(
                "Your trip needs little or no clock shift. Your plan keeps you on your usual routine.",
                emptyList(),
            )
        }
        val earlier = adaptation.direction == AdaptationDirection.Earlier
        val days = summary.preparation.emitted
        val key =
            when (Triple(earlier, days > 0, withReturn)) {
                Triple(
                    true,
                    true,
                    false,
                ),
                -> "We shift your clock :shift earlier, starting :count day before you leave.|We shift your clock :shift earlier, starting :count days before you leave."

                Triple(
                    false,
                    true,
                    false,
                ),
                -> "We shift your clock :shift later, starting :count day before you leave.|We shift your clock :shift later, starting :count days before you leave."

                Triple(
                    true,
                    true,
                    true,
                ),
                -> "We shift your clock :shift earlier, starting :count day before you leave. Your trip home is planned too.|We shift your clock :shift earlier, starting :count days before you leave. Your trip home is planned too."

                Triple(
                    false,
                    true,
                    true,
                ),
                -> "We shift your clock :shift later, starting :count day before you leave. Your trip home is planned too.|We shift your clock :shift later, starting :count days before you leave. Your trip home is planned too."

                Triple(true, false, false) -> "We shift your clock :shift earlier during your trip."

                Triple(false, false, false) -> "We shift your clock :shift later during your trip."

                Triple(
                    true,
                    false,
                    true,
                ),
                -> "We shift your clock :shift earlier during your trip. Your trip home is planned too."

                else -> "We shift your clock :shift later during your trip. Your trip home is planned too."
            }
        val shift =
            CopyArgument.Standard(
                CopyArg("shift", CopyArgKind.Minutes, CopyArgValue.Numeric(adaptation.shiftMin.toBigDecimal()), null),
            )
        val args =
            listOf(shift) +
                if (days > 0) {
                    listOf(
                        CopyArgument.Standard(
                            CopyArg("count", CopyArgKind.Count, CopyArgValue.Numeric(days.toBigDecimal()), null),
                        ),
                    )
                } else {
                    emptyList()
                }

        return CopyLine(key, args)
    }

    fun layovers(
        input: ApiTripInput,
        legs: List<ApiReviewLeg>,
        airports: List<ApiAirportSuggestion>,
    ): List<ApiReviewLayover> {
        val inputs = input.outbound.legs + input.`return`?.legs.orEmpty()
        val layovers = mutableListOf<ApiReviewLayover>()
        for (index in 0 until maxOf(0, inputs.size - 1)) {
            val leg = inputs[index]
            val next = inputs[index + 1]
            val airport = airports.firstOrNull { it.iata == leg.toIata }
            if (leg.toIata != next.fromIata ||
                airport == null
            ) {
                throw MockSettings.invalid("review", "Start this flight where the previous flight lands.")
            }
            val arrival = instant(leg.arriveLocal, airport)
            val departure = instant(next.departLocal, airport)
            if (departure <= arrival) throw MockSettings.invalid("review", "Check the departure and arrival times.")
            val zone = ZoneId.of(airport.timezone)
            var date = arrival.atZone(zone).toLocalDate()
            var nights = 0
            while (date < departure.atZone(zone).toLocalDate()) {
                val nextDate = date.plusDays(1)
                val start = date.atTime(23, 0).atZone(zone).toInstant()
                val end = nextDate.atTime(7, 0).atZone(zone).toInstant()
                if (arrival <= start && end <= departure) nights++
                date = nextDate
            }
            val destination = input.`return` != null && index == input.outbound.legs.size - 1
            val kind =
                if (destination) {
                    SegmentKindCode.Destination
                } else if (nights >=
                    3
                ) {
                    SegmentKindCode.Stop
                } else if (nights >= 1) {
                    SegmentKindCode.Stopover
                } else {
                    SegmentKindCode.Connection
                }
            layovers +=
                ApiReviewLayover(
                    legs[index].journey,
                    legs[index].legIndex,
                    kind,
                    java.time.Duration
                        .between(arrival, departure)
                        .toMinutes()
                        .toInt(),
                    nights,
                )
        }
        return layovers
    }

    fun stayEnd(input: ApiTripInput): ApiStayEnd? {
        if (input.tripEndKind !in listOf("nights", "check_out")) return null
        val leg = input.outbound.legs.lastOrNull() ?: return null
        val airport = airports().firstOrNull { it.iata == leg.toIata } ?: return null
        val zone = ZoneId.of(airport.timezone)
        val declared = leg.stay.checkOutLocal
        val date = input.tripEndCheckOutDate
        val nights = input.tripEndNights
        val leave =
            when {
                declared != null -> {
                    instant(declared, airport)
                }

                input.tripEndKind == "check_out" && date != null -> {
                    instant(date + "T12:00", airport)
                }

                nights != null -> {
                    val transferEnd =
                        instant(
                            leg.arriveLocal,
                            airport,
                        ).plusSeconds((45 + (leg.stay.arrivalTransferMin ?: 90)) * 60L).atZone(zone)
                    val firstNight = transferEnd.toLocalDate().plusDays(if (transferEnd.hour < 3) -1 else 0)
                    firstNight
                        .atTime(12, 0)
                        .atZone(zone)
                        .plusDays(nights.toLong())
                        .toInstant()
                }

                else -> {
                    return null
                }
            }
        return ApiStayEnd(leave.atZone(zone).toLocalDate().toString(), iso(leave))
    }
}
