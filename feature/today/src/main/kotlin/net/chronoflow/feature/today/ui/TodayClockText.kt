package net.chronoflow.feature.today.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.tnum
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.calendarDaysUntil
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.clock.MinuteClock
import net.chronoflow.feature.today.TodayRowUi
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.ceil

internal fun relativeMinutes(
    target: Instant,
    now: Instant,
): Int = ceil(Duration.between(now, target).toMillis() / 60_000.0).toInt().coerceAtLeast(0)

internal fun completedDate(
    endDate: String,
    today: LocalDate,
): String = minOf(endDate, today.toString())

internal fun languageLocale(language: Language): Locale =
    if (language ==
        Language.Nl
    ) {
        Locale.forLanguageTag("nl")
    } else {
        Locale.ENGLISH
    }

@Composable
internal fun RowRelativeText(
    row: TodayRowUi,
    clock: MinuteClock,
    language: Language,
    formatter: CopyLineFormatter,
    modifier: Modifier = Modifier,
) {
    val todayProbe = LocalTodayProbe.current
    androidx.compose.runtime.SideEffect { todayProbe?.composed(TodayProbe.Kind.LEAF, "row.${row.blockId}") }
    val minutes by remember(row.target, clock) { derivedStateOf { relativeMinutes(row.target, clock.now) } }
    val duration = formatter.formatMinutes(minutes.toDouble(), language, duration = true)
    val key = if (row.running) R.string.screens_duration_left else R.string.screens_in_duration
    val relative = stringResource(key, duration)
    val text =
        buildString {
            if (row.running) append("→ ")
            append(row.timeText)
                .append(" · ")
                .append(row.zone)
                .append(" · ")
                .append(relative)
        }
    Text(text, modifier, color = CfTheme.colors.mutedForeground, style = CfTheme.type.screenBody.tnum())
}

@Composable
internal fun DaysUntilText(
    at: String,
    zone: String,
    clock: MinuteClock,
    id: String,
    modifier: Modifier = Modifier,
) {
    val todayProbe = LocalTodayProbe.current
    androidx.compose.runtime.SideEffect { todayProbe?.composed(TodayProbe.Kind.LEAF, "days.$id") }
    val days by remember(at, zone, clock) { derivedStateOf { calendarDaysUntil(at, clock.now, zone) } }
    val text =
        if (days == 0) {
            stringResource(R.string.screens_today)
        } else {
            pluralStringResource(R.plurals.screens_in_count_days, days, days)
        }
    Text(
        text,
        modifier,
        color = CfTheme.colors.mutedForeground,
        style = CfTheme.type.style(CfTypeScale.screenMeta).tnum(),
    )
}

@Composable
internal fun PlanStartsEyebrow(
    at: String,
    zone: String,
    clock: MinuteClock,
    id: String,
    language: Language,
    formatter: CopyLineFormatter,
    modifier: Modifier = Modifier,
) {
    val todayProbe = LocalTodayProbe.current
    androidx.compose.runtime.SideEffect { todayProbe?.composed(TodayProbe.Kind.LEAF, "starts.$id") }
    val days by remember(at, zone, clock) { derivedStateOf { calendarDaysUntil(at, clock.now, zone) } }
    val date = formatter.formatWeekdayDate(at, language, zone)
    val text =
        when (days) {
            0 -> stringResource(R.string.screens_plan_starts_today)
            1 -> stringResource(R.string.screens_plan_starts_tomorrow)
            else -> stringResource(R.string.screens_plan_starts_on_date, date)
        }
    Text(
        text.uppercase(languageLocale(language)),
        modifier,
        color = CfTheme.colors.dawnText,
        style = CfTheme.type.style(CfTypeScale.screenEyebrow),
    )
}

@Composable
internal fun CompletedBodyText(
    route: String,
    endDate: String,
    clock: MinuteClock,
    language: Language,
    formatter: CopyLineFormatter,
    modifier: Modifier = Modifier,
) {
    val todayProbe = LocalTodayProbe.current
    androidx.compose.runtime.SideEffect { todayProbe?.composed(TodayProbe.Kind.LEAF, "completed.body") }
    val todayEpoch by remember(clock) {
        derivedStateOf {
            clock.now
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toEpochDay()
        }
    }
    val today = LocalDate.ofEpochDay(todayEpoch)
    val date = completedDate(endDate, today)
    val formatted =
        if (date.take(4) == today.year.toString()) {
            formatter.formatDayMonth(date, language)
        } else {
            formatter.formatDate(date, language)
        }
    Text(
        stringResource(R.string.screens_your_last_trip_route_ended_on_dat_d59ad3, route, formatted),
        modifier,
        color = CfTheme.colors.mutedForeground,
        style = CfTheme.type.style(CfTypeScale.screenBodyLg),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}
