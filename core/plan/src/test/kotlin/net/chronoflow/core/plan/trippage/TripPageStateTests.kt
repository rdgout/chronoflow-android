package net.chronoflow.core.plan.trippage

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import net.chronoflow.core.plan.tripDetail
import org.junit.Test

class TripPageStateTests {
    /** (new) */
    @Test fun replacementWithoutLabelDateFallsBackToFirstDay() {
        val days = tripDetail().plan.days
        val fallback = replacementAnchor(TripPageAnchor(days[2].id, null, 50.0), days)
        assertThat(fallback).isEqualTo(TripPageAnchor(days[0].id, days[0].labelDate, 0.0))
        val empty = replacementAnchor(TripPageAnchor(days[2].id, days[2].labelDate, 50.0), emptyList())
        assertThat(empty).isEqualTo(TripPageAnchor(null, null, 0.0))
        assertThat(empty.dayID).isNull()
    }
}
