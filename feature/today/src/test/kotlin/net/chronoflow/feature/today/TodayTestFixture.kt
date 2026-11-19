package net.chronoflow.feature.today

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckKey
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.contracts.TripProgress
import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.model.store.TripRecord
import net.chronoflow.core.plan.CopyCatalog
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.TripListValue
import net.chronoflow.core.plan.trippage.PlanPreferences
import net.chronoflow.core.testing.Fixtures
import net.chronoflow.core.testing.sync.card
import java.time.Instant

internal val todayNow: Instant = Instant.parse("2026-11-08T10:00:00Z")

internal fun todayDetail(id: String = "hero"): TripDetail {
    val source = ApiJson.response.decodeFromString(TripDetail.serializer(), Fixtures.text("trips/detail.json"))
    val block = source.plan.blocks.first { it.actionable && it.checkKey != null }
    val blocks =
        (0..4).map { index ->
            block.copy(
                id = "row$index",
                type = BlockType.Sleep,
                priority = Priority.Core,
                text = CopyLine("Sleep", emptyList()),
                startUtc =
                    todayNow
                        .plusSeconds(
                            if (index ==
                                0
                            ) {
                                -600
                            } else {
                                index * 3600L
                            },
                        ).toString(),
                endUtc = todayNow.plusSeconds((index + 1) * 3600L).toString(),
                checkKey = CheckKey(BlockType.Sleep, "2026-11-08", index + 1),
            )
        }
    return source.copy(
        trip = source.trip.copy(id = id),
        plan =
            source.plan.copy(
                blocks = blocks,
                days =
                    listOf(
                        source.plan.days.first().copy(
                            startUtc = todayNow.minusSeconds(3600).toString(),
                            endUtc = todayNow.plusSeconds(86400).toString(),
                        ),
                    ),
            ),
        checkState = source.checkState.copy(marks = emptyList()),
    )
}

internal fun todayTrip(
    id: String = "hero",
    status: TripStatus = TripStatus.Active,
    start: Instant = todayNow.minusSeconds(3600),
    end: Instant = todayNow.plusSeconds(86400),
): TripListValue {
    val template = card().card
    val card =
        template.copy(
            id = id,
            from = template.from.copy(city = "Amsterdam"),
            to = template.to.copy(city = "New York"),
            progress = TripProgress(status, null),
            departureUtc = start.toString(),
            arrivalUtc = end.toString(),
            arriveLocal = "2026-11-09T12:00",
        )
    return TripListValue(card, start.toString(), end.toString(), status, card.from.zone, "2026-11-09", 3)
}

internal fun todayRecord(value: TripListValue = todayTrip()): TripRecord =
    TripRecord(
        value.card.id,
        ApiJson.response.encodeToString(TripCardView.serializer(), value.card),
        currentInputHash = todayDetail(value.card.id).plan.meta.inputHash,
        departureUtc = value.card.departureUtc,
        planStartUtc = value.planStartUtc,
        planEndUtc = value.planEndUtc,
        planStartZone = value.planStartZone,
        planEndDate = value.planEndDate,
        preparationDays = value.preparationDays,
    )

internal suspend fun todayFormatter(dispatcher: CoroutineDispatcher) = CopyLineFormatter(CopyCatalog.load(dispatcher))

internal fun todayInput(formatter: CopyLineFormatter) =
    TodayInput(listOf(todayTrip()), true, PlanPreferences(), todayDetail(), todayNow, Language.En, formatter)
