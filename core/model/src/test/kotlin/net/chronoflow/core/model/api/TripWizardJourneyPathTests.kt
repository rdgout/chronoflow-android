package net.chronoflow.core.model.api

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TripWizardJourneyPathTests(
    private val label: String,
    private val path: String,
    private val expected: String,
) {
    @Test fun journeyPathRewritesLegsStaysAndTopLevelKeys() {
        assertThat(TripWizardErrors.journeyPath(path, 1)).isEqualTo(expected)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<String>> =
            listOf(
                arrayOf(
                    "outbound.legs.0.stay.bed_available_from_local",
                    "outbound.legs.0.stay.bed_available_from_local",
                    "stays.0.bedAvailableFromLocal",
                ),
                arrayOf("return.legs.0.arrive_local", "return.legs.0.arrive_local", "legs.1.arriveLocal"),
                arrayOf("outbound.legs.0.surprise", "outbound.legs.0.surprise", "legs.0.surprise"),
                arrayOf("trip_end_nights", "trip_end_nights", "end.nights"),
                arrayOf("trip_end_check_out_date", "trip_end_check_out_date", "end.checkOutDate"),
                arrayOf("preparation_days", "preparation_days", "preparation.days"),
                arrayOf(
                    "pre_departure_busy_intervals.2.start_local",
                    "pre_departure_busy_intervals.2.start_local",
                    "preparation.busyIntervals.2.startLocal",
                ),
                arrayOf("override.use", "override.use", "override.enabled"),
                arrayOf("override.habitual_bed", "override.habitual_bed", "override.habitualBed"),
                arrayOf("profile.habitual_bed", "profile.habitual_bed", "profile.habitualBed"),
                arrayOf("return.legs.x.arrive_local", "return.legs.x.arrive_local", "return.legs.x.arriveLocal"),
                arrayOf("outbound..legs.0", "outbound..legs.0", "legs.0"),
                arrayOf("<empty>", "", ""),
            )
    }
}
