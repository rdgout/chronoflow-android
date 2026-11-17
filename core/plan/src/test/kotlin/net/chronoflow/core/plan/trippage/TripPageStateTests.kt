package net.chronoflow.core.plan.trippage

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import net.chronoflow.core.model.contracts.DayView
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.plan.tripDetail
import org.junit.Test
import java.time.Instant

class TripPageStateTests {
    @Test fun stateGatingTable() {
        val problems = mutableListOf<String>()
        for (keep in listOf(false, true)) {
            for (stored in listOf(false, true)) {
                for (online in listOf(false, true)) {
                    for (awaiting in listOf(false, true)) {
                        for (fetch in TripPageFetch.entries) {
                            val result =
                                TripPageState.resolve(
                                    keep,
                                    stored,
                                    online = online,
                                    fetch = fetch,
                                    awaitingSync = awaiting,
                                )
                            val expected =
                                when {
                                    stored -> TripPageState.Ready
                                    keep -> TripPageState.Loading
                                    !online || fetch == TripPageFetch.Offline -> TripPageState.RemoteOffline
                                    fetch == TripPageFetch.Failed -> TripPageState.Server
                                    fetch == TripPageFetch.NotFound -> TripPageState.NotFound
                                    else -> TripPageState.RemoteLoading
                                }
                            if (result != expected) problems += "$keep $stored $online $awaiting $fetch: $result"
                        }
                    }
                }
            }
        }
        assertThat(problems).isEqualTo(emptyList<String>())
        assertThat(TripPageState.resolve(true, true, noPlan = true, online = false, fetch = TripPageFetch.Failed))
            .isEqualTo(TripPageState.NoPlan)
        assertThat(TripPageState.resolve(true, true, online = false, fetch = TripPageFetch.Idle, tombstoned = true))
            .isEqualTo(TripPageState.NotFound)
        assertThat(
            TripPageState.resolve(false, false, online = false, fetch = TripPageFetch.Idle, missingAfterSync = true),
        ).isEqualTo(TripPageState.NotFound)
        val awaiting =
            TripPageState.resolve(
                false,
                false,
                online = true,
                fetch = TripPageFetch.Idle,
                missingAfterSync = true,
                awaitingSync = true,
            )
        assertThat(awaiting).isEqualTo(TripPageState.RemoteLoading)
    }

    @Test fun openingOrderAndReplacement() {
        val detail = tripDetail()
        val days = detail.plan.days
        val today = planDate(days[2].startUtc)!!

        fun open(
            day: String? = null,
            block: String? = null,
            kept: String? = null,
            now: Instant = today,
        ): DayView? = openingDay(days, detail.plan.blocks, day, block, kept, now)
        assertThat(open(day = days[0].id, kept = days[1].id)?.id).isEqualTo(days[0].id)
        val block =
            detail.plan.blocks.first {
                planDate(it.startUtc)!! >= today && planDate(it.startUtc)!! < planDate(days[2].endUtc)!!
            }
        assertThat(open(block = block.id, kept = days[0].id)?.id).isEqualTo(days[2].id)
        assertThat(open(kept = days[1].id)?.id).isEqualTo(days[1].id)
        assertThat(open()?.id).isEqualTo(days[2].id)
        assertThat(open(now = planDate(days.last().endUtc)!!)?.id).isEqualTo(days.last().id)
        assertThat(open(now = planDate(days[0].startUtc)!!.minusSeconds(60))?.id).isEqualTo(days[0].id)
        val renamed = days.toMutableList().also { it[2] = it[2].copy(id = "replacement") }
        val anchor = TripPageAnchor(days[2].id, days[2].labelDate, 180.0)
        assertThat(replacementAnchor(anchor, renamed).dayID).isEqualTo("replacement")
        assertThat(replacementAnchor(anchor, renamed).offset).isEqualTo(180.0)
        val duplicate = days.toMutableList().also { it[0] = it[0].copy(labelDate = days[2].labelDate) }
        assertThat(replacementAnchor(anchor, duplicate).dayID).isEqualTo(days[2].id)
        assertThat(replacementAnchor(anchor, renamed.takeLast(1)).dayID).isEqualTo(renamed.last().id)
    }

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
