package net.chronoflow.core.plan

import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.model.contracts.TripDetail

val testFormatter: CopyLineFormatter by lazy { CopyLineFormatter(testCatalog) }

fun prepare(
    detail: TripDetail,
    language: Language = Language.En,
    timeFormat: TimeFormat = TimeFormat.Value24,
    marks: List<CheckMark> = emptyList(),
    pendingChecks: List<CheckOffChange> = emptyList(),
    layouts: Map<String, DayLayout> = layoutPlan(detail.plan, detail.timeline),
): PreparedPlan = preparePlan(detail, layouts, testFormatter, testPageCopy, language, timeFormat, marks, pendingChecks)
