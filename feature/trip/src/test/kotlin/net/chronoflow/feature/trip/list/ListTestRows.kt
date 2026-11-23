package net.chronoflow.feature.trip.list

import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.PageCopy
import net.chronoflow.core.plan.PageCopyTable
import net.chronoflow.core.plan.trippage.ListRows
import net.chronoflow.feature.trip.timeline.TimelineTestPlans

object ListTestRows {
    val days by lazy {
        ListRows.build(
            TimelineTestPlans.prepared("S3").plan,
            PageCopy(Language.En, PageCopyTable.load()),
        )
    }
    val row get() = days.flatMap { it.rows }.first { !it.zone && it.priorityTag != null }
    val zone get() = days.flatMap { it.rows }.first { it.zone }
}
