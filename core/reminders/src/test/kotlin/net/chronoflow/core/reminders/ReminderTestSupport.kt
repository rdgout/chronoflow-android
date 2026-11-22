package net.chronoflow.core.reminders

import kotlinx.serialization.json.JsonNull
import net.chronoflow.core.model.contracts.AirportRef
import net.chronoflow.core.model.contracts.AvoidLightParamsView
import net.chronoflow.core.model.contracts.AvoidSetting
import net.chronoflow.core.model.contracts.BlockParams
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.BlockView
import net.chronoflow.core.model.contracts.CheckKeyView
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckState
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.Lane
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.NoCaffeineParamsView
import net.chronoflow.core.model.contracts.PlanMeta
import net.chronoflow.core.model.contracts.PlanNoticesView
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.model.contracts.PlanView
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.model.contracts.SeekLightParamsView
import net.chronoflow.core.model.contracts.SeekSource
import net.chronoflow.core.model.contracts.SleepKind
import net.chronoflow.core.model.contracts.SleepParamsView
import net.chronoflow.core.model.contracts.SleepSetting
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.model.contracts.TimelinePresentationView
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.contracts.TripDetailView
import net.chronoflow.core.model.contracts.TripProgress
import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.plan.CopyCatalog
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.testing.reminders.FakeReminderStrings
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val reminderNow = checkNotNull(planDate("2026-10-14T00:00:00Z"))
internal val reminderAccount = "01900000-0000-7000-8000-000000000001"
private val reminderStartFormat =
    DateTimeFormatter
        .ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT)
        .withZone(ZoneOffset.UTC)
private val reminderEndFormat =
    DateTimeFormatter
        .ofPattern("uuuu-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT)
        .withZone(ZoneOffset.UTC)

internal fun reminderBlock(
    hours: Double = 1.0,
    type: BlockType = BlockType.SeekLight,
    ordinal: Int = 1,
    zone: String = "UTC",
): BlockView {
    val start = reminderNow.plusMillis(Math.round(hours * 3_600_000.0))
    val params =
        when (type) {
            BlockType.Sleep -> BlockParams.Sleep(SleepParamsView(SleepKind.Main, SleepSetting.Bed, 60))
            BlockType.SeekLight -> BlockParams.SeekLight(SeekLightParamsView(SeekSource.Outdoor, 30))
            BlockType.AvoidLight -> BlockParams.AvoidLight(AvoidLightParamsView(AvoidSetting.Home, false))
            BlockType.NoCaffeine -> BlockParams.NoCaffeine(NoCaffeineParamsView(null, null))
            else -> BlockParams.Unknown(JsonNull)
        }
    return BlockView(
        id = "${type.raw}-$hours",
        lane = Lane.Light,
        type = type,
        startUtc = reminderStartFormat.format(start),
        endUtc = reminderEndFormat.format(start.plusSeconds(3600)),
        displayTz = zone,
        secondaryTz = null,
        priority = Priority.Recommended,
        text = CopyLine("test", emptyList()),
        secondaryText = null,
        params = params,
        actionable = true,
        checkKey = CheckKeyView(type, reminderLocalDay(start, zone), ordinal),
    )
}

internal fun reminderPlan(
    blocks: List<BlockView> = listOf(reminderBlock()),
    id: String = "trip",
    hash: String = "hash",
    marks: List<CheckMark> = emptyList(),
): ReminderPlan {
    val from = AirportRef("AMS", "Amsterdam", "Europe/Amsterdam")
    val to = AirportRef("HND", "Tokyo", "Asia/Tokyo")
    val card =
        TripCardView(
            id = id,
            from = from,
            to = to,
            departLocal = "2026-10-14T00:00",
            arriveLocal = "2026-10-14T14:00",
            departureUtc = "2026-10-14T00:00:00Z",
            arrivalUtc = "2026-10-14T05:00:00Z",
            flightNumber = null,
            connections = emptyList(),
            offsetDifferenceMinutes = 540,
            progress = TripProgress(TripStatus.Active, null),
            canComplete = false,
            canReopen = false,
            canEdit = false,
            canDuplicate = false,
            canDelete = false,
        )
    return ReminderPlan(
        card,
        TripDetail(
            trip =
                TripDetailView(
                    id,
                    from,
                    to,
                    card.departLocal,
                    card.departureUtc,
                    "2026-10-17T00:00:00Z",
                    540,
                    emptyList(),
                ),
            plan =
                PlanView(
                    status = PlanStatus.Ok,
                    reasons = emptyList(),
                    prompts = emptyList(),
                    meta = PlanMeta(hash, null, null),
                    summary = null,
                    days = emptyList(),
                    blocks = blocks,
                    warnings = emptyList(),
                ),
            notices = PlanNoticesView(emptyList(), emptyList()),
            timeline = TimelinePresentationView(emptyList(), emptyList(), emptyList()),
            checkState = CheckState(hash, emptyList(), emptyList()),
            detailVersion = "1",
        ),
        marks,
    )
}

internal fun reminderFormatter(): CopyLineFormatter {
    val data = checkNotNull(CopyCatalog::class.java.getResourceAsStream("/plan_copy.json")).use { it.readBytes() }
    return CopyLineFormatter(CopyCatalog(data))
}

abstract class ReminderTestSupport(
    private val formatter: CopyLineFormatter,
) {
    protected fun scheduled(
        plans: List<ReminderPlan>,
        now: Instant = reminderNow,
        ledger: List<ReminderLedgerEntry> = emptyList(),
        settings: ReminderSettings = ReminderSettings(),
        language: Language = Language.En,
        timeFormat: TimeFormat = TimeFormat.Value24,
        strings: ReminderStrings = FakeReminderStrings(),
    ): List<ReminderRequest> =
        reminderRequests(
            plans,
            reminderAccount,
            settings,
            ledger,
            now,
            language,
            timeFormat,
            formatter,
            strings,
        )
}
