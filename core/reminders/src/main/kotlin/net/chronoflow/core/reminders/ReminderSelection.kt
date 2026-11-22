package net.chronoflow.core.reminders

import net.chronoflow.core.model.contracts.BlockParams
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.BlockView
import net.chronoflow.core.model.contracts.CheckKeyView
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.model.contracts.SleepKind
import net.chronoflow.core.model.contracts.TimelinePresentationView
import net.chronoflow.core.plan.checkIdentity
import net.chronoflow.core.plan.planDate
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.zone.ZoneRules

data class ReminderMember(
    val kind: ReminderKind,
    val blockType: BlockType,
    val localDate: String,
    val ordinal: Int,
    val sendAt: Instant,
    val blockStartUtc: Instant,
    val displayTz: String,
    val expiresAt: Instant,
) {
    val identity: String get() = checkIdentity(CheckKeyView(blockType, localDate, ordinal))
}

data class ReminderDraft(
    val members: List<ReminderMember>,
    val sendAt: Instant,
    val expiresAt: Instant,
    val blockStartUtc: Instant,
    val displayTz: String,
    val priority: Priority,
)

data class ReminderSelectionOptions(
    val coalesceMinutes: Int = 15,
    val dailyCap: Int = 6,
    val expiryGraceMinutes: Int = 20,
    val sentMemberKeys: Set<String> = emptySet(),
    val sentCountsByDay: Map<String, Int> = emptyMap(),
    val timeline: TimelinePresentationView? = null,
)

fun reminderLocalDay(
    date: Instant,
    zone: String,
): String = reminderLocalDay(date, reminderZoneRules(zone))

private fun reminderZoneRules(zone: String): ZoneRules =
    try {
        ZoneId.of(zone).rules
    } catch (_: DateTimeException) {
        ZoneOffset.UTC.rules
    }

private fun reminderLocalDay(
    date: Instant,
    rules: ZoneRules,
): String {
    val parts = LocalDateTime.ofEpochSecond(date.epochSecond, 0, rules.getOffset(date))
    return parts.year.toString().padStart(4, '0') + "-" +
        parts.monthValue.toString().padStart(2, '0') + "-" + parts.dayOfMonth.toString().padStart(2, '0')
}

private data class ReminderBlock(
    val block: BlockView,
    val start: Instant,
    val end: Instant,
)

fun reminderDrafts(
    blocks: List<BlockView>,
    settings: ReminderSettings,
    now: Instant,
    options: ReminderSelectionOptions = ReminderSelectionOptions(),
): List<ReminderDraft> {
    val parsed =
        blocks.mapNotNull { block ->
            val start = planDate(block.startUtc) ?: return@mapNotNull null
            val end = planDate(block.endUtc) ?: return@mapNotNull null
            ReminderBlock(block, start, end)
        }
    val zones = HashMap<String, ZoneRules>()

    fun localDay(
        date: Instant,
        zone: String,
    ): String = reminderLocalDay(date, zones.getOrPut(zone) { reminderZoneRules(zone) })

    val keys = HashMap<String, CheckKeyView>()
    val ordinals = HashMap<String, Int>()
    for ((block, start) in parsed) {
        if (!block.actionable) continue
        val day = localDay(start, block.displayTz)
        val group = "${block.type.raw}|$day"
        val ordinal = (ordinals[group] ?: 0) + 1
        ordinals[group] = ordinal
        keys[block.id] = CheckKeyView(block.type, day, ordinal)
    }
    val sleeps = parsed.filter { it.block.type == BlockType.Sleep }.sortedBy { it.start }
    val ifYouCan =
        options.timeline
            ?.blockReasons
            .orEmpty()
            .filter { reason -> reason.lines.any { it.key == "If you can" } }
            .map { it.blockId }
            .toSet()
    val drafts = ArrayList<ReminderDraft>()
    for ((block, start, end) in parsed) {
        val fallback = keys[block.id] ?: continue
        if (block.priority != Priority.Core && block.priority != Priority.Recommended) continue
        val kind =
            when (block.type) {
                BlockType.SeekLight -> {
                    if (settings.light) ReminderKind.SeekLight else null
                }

                BlockType.AvoidLight -> {
                    if (settings.avoid && block.id !in ifYouCan) ReminderKind.AvoidLight else null
                }

                BlockType.NoCaffeine -> {
                    if (settings.caffeine) ReminderKind.CaffeineCutoff else null
                }

                BlockType.Sleep -> {
                    val mainSleep = (block.params as? BlockParams.Sleep)?.value?.kind == SleepKind.Main
                    if (mainSleep && settings.winddown) ReminderKind.WindDown else null
                }

                else -> {
                    null
                }
            } ?: continue
        val expires = minOf(end, start.plusSeconds(options.expiryGraceMinutes.toLong() * 60))
        if (expires <= now) continue
        var send = maxOf(start.minusSeconds(settings.leadMin.toLong() * 60), now)
        for (sleep in sleeps) {
            if (send >= sleep.start && send < sleep.end) send = sleep.end
        }
        if (send >= expires) continue
        val key = block.checkKey ?: fallback
        if (checkIdentity(key) in options.sentMemberKeys) continue
        val member = ReminderMember(kind, key.type, key.localDate, key.ordinal, send, start, block.displayTz, expires)
        drafts +=
            ReminderDraft(
                listOf(member),
                send,
                expires,
                start,
                block.displayTz,
                block.priority,
            )
    }
    val sorted = drafts.sortedWith(compareBy<ReminderDraft> { it.sendAt }.thenBy { it.blockStartUtc })
    val merged = ArrayList<ReminderDraft>()
    for (draft in sorted) {
        val previous = merged.lastOrNull()
        if (previous != null && draft.sendAt <= previous.sendAt.plusSeconds(options.coalesceMinutes.toLong() * 60)) {
            merged[merged.lastIndex] =
                previous.copy(
                    members = previous.members + draft.members,
                    expiresAt = maxOf(previous.expiresAt, draft.expiresAt),
                    priority = if (previous.priority == Priority.Core) Priority.Core else draft.priority,
                )
        } else {
            merged += draft
        }
    }
    val cappedOrder =
        merged.sortedWith { a, b ->
            if (a.priority == b.priority) {
                a.blockStartUtc.compareTo(b.blockStartUtc)
            } else if (a.priority == Priority.Core) {
                -1
            } else {
                1
            }
        }
    val counts = options.sentCountsByDay.toMutableMap()
    return cappedOrder
        .filter { draft ->
            val day = localDay(draft.blockStartUtc, draft.displayTz)
            val count = counts[day] ?: 0
            if (count >= options.dailyCap) {
                false
            } else {
                counts[day] = count + 1
                true
            }
        }.sortedBy { it.sendAt }
}
