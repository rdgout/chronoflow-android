package net.chronoflow.core.plan

import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.testing.sync.card
import org.junit.Assert.assertEquals
import org.junit.Test

class TripsTests {
    @Test fun tripsSectionsKeepRNNewestFirstAndStableTies() {
        fun value(
            id: String,
            status: TripStatus,
            departure: String,
        ) = TripListValue(card(id).card.copy(departureUtc = departure), null, null, status)
        val sections =
            tripSections(
                listOf(
                    value("b", TripStatus.Upcoming, "2026-11-10T00:00:00Z"),
                    value("a", TripStatus.Upcoming, "2026-11-10T00:00:00Z"),
                    value("new", TripStatus.Upcoming, "2026-11-11T00:00:00Z"),
                    value("active", TripStatus.Active, "2026-11-01T00:00:00Z"),
                    value("past", TripStatus.Completed, "2026-10-01T00:00:00Z"),
                ),
            )
        assertEquals(listOf(TripStatus.Active, TripStatus.Upcoming, TripStatus.Completed), sections.map { it.status })
        assertEquals(listOf("new", "b", "a"), sections[1].trips.map { it.card.id })
        assertEquals(emptyList<TripSection>(), tripSections(emptyList()))
    }
}
