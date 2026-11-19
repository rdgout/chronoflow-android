package net.chronoflow.feature.trips

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.plan.CopyCatalog
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.TripListValue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class TripCardFormattingTest {
    @Test fun cardsUseTheRealFormatterForBoardTimesAndOffsets() =
        runTest(timeout = 60.seconds) {
            val formatter = CopyLineFormatter(CopyCatalog.load(StandardTestDispatcher(testScheduler)))
            val cards =
                TripsBoardFixture.cards().map {
                    tripCardUi(
                        TripListValue(it, null, null, TripStatus.Upcoming),
                        null,
                        Instant.EPOCH,
                        TripsPreferences(),
                        Language.En,
                        formatter,
                    )
                }
            assertEquals(
                listOf(
                    "12 Oct 14:40 → 13 Oct 09:15",
                    "21 Oct 10:25 → 21 Oct 15:30",
                    "3 Jun 10:10 → 3 Jun 12:20",
                ),
                cards.map {
                    it.times
                },
            )
            assertEquals(listOf("+7 h", "−7 h", "−6 h"), cards.map { it.offset })
        }
}
