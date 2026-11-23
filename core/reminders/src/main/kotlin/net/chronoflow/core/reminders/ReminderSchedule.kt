package net.chronoflow.core.reminders

import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.plan.CopyFormat
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.buildCheckState
import net.chronoflow.core.plan.checkIdentity
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.plan.withoutOptionalSteps
import net.chronoflow.core.plan.zoneLabel
import java.time.Instant
import java.util.Base64

const val REMINDER_LOOKBACK_SECONDS = 129_600L
const val REMINDER_MARGIN_SECONDS = 5L
const val REMINDER_WINDOW_SECONDS = 172_800L
const val REMINDER_MAX_REQUESTS = 60

data class ReminderPlan(
    val card: TripCardView,
    val detail: TripDetail,
    val marks: List<CheckMark> = emptyList(),
)

data class ReminderRequest(
    val identifier: String,
    val title: String,
    val body: String,
    val sendAt: Instant,
    val tripID: String,
    val blockID: String,
    val inputHash: String,
    val memberKeys: List<String>,
    val accountID: String? = null,
    val expiresAt: Instant,
)

fun reminderRequests(
    plans: List<ReminderPlan>,
    accountID: String,
    settings: ReminderSettings,
    ledger: List<ReminderLedgerEntry>,
    now: Instant,
    language: Language,
    timeFormat: TimeFormat,
    formatter: CopyLineFormatter,
    strings: ReminderStrings,
): List<ReminderRequest> {
    val elapsedLedger = ReminderLedger.elapsed(ledger, now)
    val requests = ArrayList<ReminderRequest>()
    for (value in plans) {
        if (value.card.progress.status == TripStatus.Completed) continue
        val detail = value.detail
        val plan = withoutOptionalSteps(detail.plan)
        val sentMemberKeys = buildCheckState(plan, value.marks).marks.map { checkIdentity(it.key) }.toMutableSet()
        val sentCountsByDay = HashMap<String, Int>()
        val elapsed =
            reminderDrafts(
                plan.blocks,
                settings,
                now.minusSeconds(REMINDER_LOOKBACK_SECONDS),
                ReminderSelectionOptions(timeline = detail.timeline),
            ).filter { it.sendAt <= now.plusSeconds(REMINDER_MARGIN_SECONDS) }
        for (draft in elapsed) {
            val day = reminderLocalDay(draft.blockStartUtc, draft.displayTz)
            sentCountsByDay[day] = (sentCountsByDay[day] ?: 0) + 1
            for (member in draft.members) {
                if (member.sendAt <= now.plusSeconds(REMINDER_MARGIN_SECONDS)) sentMemberKeys += member.identity
            }
        }
        for (entry in elapsedLedger) {
            if (entry.tripId == value.card.id && entry.inputHash == plan.meta.inputHash) {
                sentMemberKeys += entry.memberKeys
            }
        }
        val options =
            ReminderSelectionOptions(
                timeline = detail.timeline,
                sentMemberKeys = sentMemberKeys,
                sentCountsByDay = sentCountsByDay,
            )
        val upcoming =
            reminderDrafts(plan.blocks, settings, now, options).filter {
                it.sendAt > now.plusSeconds(REMINDER_MARGIN_SECONDS) &&
                    it.sendAt <= now.plusSeconds(REMINDER_WINDOW_SECONDS)
            }
        for (draft in upcoming) {
            val members = draft.members.sortedBy { it.blockStartUtc }
            val announced = HashSet<ReminderKind>()
            val format =
                CopyFormat(
                    language = language,
                    timeFormat = timeFormat,
                    displayZone = draft.displayTz,
                    annotateInstantZone = true,
                    trip = detail.trip,
                )
            val body =
                members
                    .mapNotNull { member ->
                        if (!announced.add(member.kind)) return@mapNotNull null
                        var time =
                            formatter.formatInstant(
                                value = member.blockStartUtc.toString(),
                                options = format,
                                zone = member.displayTz,
                            )
                        if (member.displayTz != draft.displayTz) {
                            time += " (${zoneLabel(member.displayTz, detail.trip)})"
                        }
                        strings.text(member.kind.copyKey, language, time).trimEnd('.') + "."
                    }.joinToString(" ")
            val keys = draft.members.map { it.identity }
            val identity = (listOf(accountID, value.card.id, plan.meta.inputHash) + keys).joinToString("\n")
            val identifier = "chronoflow.reminder." + Base64.getEncoder().encodeToString(identity.encodeToByteArray())
            val first = draft.members[0]
            val block =
                plan.blocks.firstOrNull { it.checkKey?.let(::checkIdentity) == first.identity }
                    ?: plan.blocks.firstOrNull {
                        it.type == first.blockType && planDate(
                            it.startUtc,
                        ) == first.blockStartUtc
                    }
            val from = value.card.from.city ?: value.card.from.iata
            val to = value.card.to.city ?: value.card.to.iata
            requests +=
                ReminderRequest(
                    identifier = identifier,
                    title = "$from → $to",
                    body = body,
                    sendAt = draft.sendAt,
                    tripID = value.card.id,
                    blockID = block?.id ?: "",
                    inputHash = plan.meta.inputHash,
                    memberKeys = keys,
                    accountID = accountID,
                    expiresAt = draft.expiresAt,
                )
        }
    }
    return requests
        .sortedWith(compareBy<ReminderRequest> { it.sendAt }.thenBy { it.identifier })
        .take(REMINDER_MAX_REQUESTS)
}
