package net.chronoflow.core.plan

import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.testing.sync.card
import org.junit.Assert.assertEquals
import org.junit.Test

class TripSectionsTest {
    private fun value(status: TripStatus) = TripListValue(card().card, null, null, status)

    @Test fun unknownStatusIsDropped() {
        assertEquals(emptyList<TripSection>(), tripSections(listOf(value(TripStatus.Unknown("future")))))
    }

    @Test fun emptySectionsAreOmitted() {
        assertEquals(listOf(TripStatus.Upcoming), tripSections(listOf(value(TripStatus.Upcoming))).map { it.status })
    }

    @Test fun sectionsRunActiveUpcomingCompleted() {
        assertEquals(
            listOf(TripStatus.Active, TripStatus.Upcoming, TripStatus.Completed),
            tripSections(
                listOf(value(TripStatus.Completed), value(TripStatus.Upcoming), value(TripStatus.Active)),
            ).map {
                it.status
            },
        )
    }
}
