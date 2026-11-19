package net.chronoflow.feature.trips

import net.chronoflow.core.model.contracts.AirportRef
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.TripListValue
import net.chronoflow.core.testing.sync.card
import net.chronoflow.core.ui.trip.NextStepUi
import net.chronoflow.core.ui.trip.tripCardUi
import java.time.Instant

internal object TripsBoardFixture {
    fun cards(): List<TripCardView> {
        val ams = AirportRef("AMS", "Amsterdam", "Europe/Amsterdam")
        val nrt = AirportRef("NRT", "Tokyo", "Asia/Tokyo")
        val jfk = AirportRef("JFK", "New York", "America/New_York")
        val template = card().card
        return listOf(
            template.copy(
                id = "active",
                from = ams,
                to = nrt,
                flightNumber = "KL 861",
                departureUtc = "2026-10-12T12:40:00Z",
                arrivalUtc = "2026-10-13T00:15:00Z",
                offsetDifferenceMinutes = 420,
            ),
            template.copy(
                id = "upcoming",
                from = nrt,
                to = ams,
                flightNumber = "KL 862",
                departureUtc = "2026-10-21T01:25:00Z",
                arrivalUtc = "2026-10-21T13:30:00Z",
                offsetDifferenceMinutes = -420,
            ),
            template.copy(
                id = "completed",
                from = ams,
                to = jfk,
                flightNumber = "KL 641",
                departureUtc = "2026-06-03T08:10:00Z",
                arrivalUtc = "2026-06-03T16:20:00Z",
                offsetDifferenceMinutes = -360,
            ),
        )
    }

    fun content(
        formatter: CopyLineFormatter,
        language: Language,
    ): TripsUiState.Content {
        val statuses = listOf(TripStatus.Active, TripStatus.Upcoming, TripStatus.Completed)
        return TripsUiState.Content(
            cards().mapIndexed { index, card ->
                val status = statuses[index]
                val ui =
                    tripCardUi(
                        TripListValue(card, null, null, status),
                        null,
                        Instant.EPOCH,
                        net.chronoflow.core.plan.trippage
                            .PlanPreferences(),
                        language,
                        formatter,
                    )
                val next =
                    if (index == 0) {
                        NextStepUi(
                            BlockType.Transfer,
                            Instant.EPOCH.plusSeconds(1800),
                            if (language == Language.Nl) "Trein naar het hotel" else "Train to the hotel",
                        )
                    } else {
                        null
                    }
                TripSectionUi(status, listOf(ui.copy(next = next)))
            },
        )
    }
}
