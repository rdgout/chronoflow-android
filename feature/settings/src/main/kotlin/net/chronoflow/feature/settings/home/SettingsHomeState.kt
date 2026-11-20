package net.chronoflow.feature.settings.home

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.model.store.LocalMutationFailure
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.plan.stepLabelKey
import java.text.BreakIterator
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Locale

// Port of SettingsHomeState.swift at f391a1fa; presentation copy is resolved by the screen.
data class SettingsHomeState(
    val account: AccountRecord? = null,
    val missingAnswers: Int = 0,
    val credits: Int? = null,
    val pendingCount: Int = 0,
    val pending: List<PendingChange> = emptyList(),
    val outboxLoaded: Boolean = false,
) {
    val initials: String
        get() {
            val words =
                account
                    ?.name
                    .orEmpty()
                    .split(Regex("\\s+"))
                    .filter(String::isNotEmpty)
            return (
                words.firstOrNull()?.firstGrapheme().orEmpty() +
                    if (words.size > 1) words.last().firstGrapheme() else ""
            ).uppercase(Locale.ROOT)
        }

    fun apply(settings: List<SettingsRecord>): SettingsHomeState {
        val readiness = settings.firstOrNull { it.section == "readiness" }?.value?.objectOrNull()
        val plan = settings.firstOrNull { it.section == "planCredits" }?.value?.objectOrNull()
        val number = (plan?.get("credits") as? JsonPrimitive)?.takeUnless { it.isString }?.doubleOrNull
        return copy(
            missingAnswers =
                if (readiness?.string("variant") == "needs-answers") {
                    (readiness["missing"] as? JsonArray)?.size.orZero() +
                        (readiness["invalidSections"] as? JsonArray)?.size.orZero()
                } else {
                    0
                },
            credits =
                if ((plan?.get("unlimited") as? JsonPrimitive)?.booleanOrNull == true) {
                    null
                } else {
                    number?.takeIf { it.isFinite() && it >= 0 && it < Int.MAX_VALUE }?.toInt()
                },
        )
    }

    @JvmName("applyOutbox")
    fun apply(outbox: List<OutboxRecord>): SettingsHomeState {
        val indices = mutableMapOf<String, Int>()
        val changes = mutableListOf<PendingChange>()
        for (row in outbox) {
            var identity = row.id
            var category: BlockType? = null
            var text: PendingText = PendingText.Generic
            var replace = false
            val change =
                try {
                    OutboxPayload.change(row)
                } catch (_: LocalMutationFailure) {
                    null
                }
            if (change is OutboxChange.CheckOff) {
                val check = change.change
                identity = "check:${check.tripID}:${check.localDate}:${check.type.raw}:${check.ordinal}"
                category = check.type
                replace = true
                if (check.status == CheckStatus.Done && stepLabelKey(check.type) != null) {
                    text = PendingText.CheckedOff(check.type)
                }
            } else if (row.kind == "settings") {
                val payload = row.payload.objectOrNull()
                val section = payload?.string("section")
                if (section != null) {
                    val subsection = payload.string("bodyClockSection")
                    identity = "settings:$section:${subsection.orEmpty()}"
                    val patch = payload["patch"] as? JsonObject
                    if (section == "bodyClock" && (
                            subsection == "sleep" ||
                                (
                                    patch != null && patch.isNotEmpty() &&
                                        patch.keys.all { it == "habitual_bed" || it == "habitual_wake" }
                                )
                        )
                    ) {
                        category = BlockType.Sleep
                        text = PendingText.Sleep
                    }
                }
            }
            val value = PendingChange(row.id, category, text)
            val index = indices[identity]
            if (index == null) {
                indices[identity] = changes.size
                changes += value
            } else if (replace) {
                changes[index] = value
            }
        }
        return copy(pendingCount = outbox.size, outboxLoaded = true, pending = changes)
    }

    fun syncLine(
        now: Instant,
        syncing: Boolean = false,
        offline: Boolean = false,
        zone: ZoneId,
    ): SyncLine? {
        if (syncing) return SyncLine.Syncing
        if (pendingCount > 0) return SyncLine.Pending(pendingCount)
        if (offline) return SyncLine.Offline
        val raw = account?.syncedAt ?: return null
        val date =
            try {
                Instant.parse(raw)
            } catch (_: DateTimeParseException) {
                try {
                    OffsetDateTime.parse(raw).toInstant()
                } catch (_: DateTimeParseException) {
                    return null
                }
            }
        val minutes = (Duration.between(date, now).seconds / 60).coerceAtLeast(0)
        if (minutes < 1) return SyncLine.JustNow
        if (minutes < 60) return SyncLine.Minutes(minutes.toInt())
        if (minutes < 1440) return SyncLine.Hours((minutes / 60).toInt())
        val days =
            ChronoUnit.DAYS
                .between(
                    date.atZone(zone).toLocalDate(),
                    now.atZone(zone).toLocalDate(),
                ).coerceAtLeast(1)
        if (days == 1L) return SyncLine.Yesterday
        if (days < 7) return SyncLine.Days(days.toInt())
        return SyncLine.On(raw)
    }
}

data class PendingChange(
    val id: String,
    val category: BlockType?,
    val text: PendingText,
)

sealed interface PendingText {
    data object Generic : PendingText

    data class CheckedOff(
        val type: BlockType,
    ) : PendingText

    data object Sleep : PendingText
}

sealed interface SyncLine {
    data object Syncing : SyncLine

    data class Pending(
        val n: Int,
    ) : SyncLine

    data object Offline : SyncLine

    data object JustNow : SyncLine

    data class Minutes(
        val n: Int,
    ) : SyncLine

    data class Hours(
        val n: Int,
    ) : SyncLine

    data object Yesterday : SyncLine

    data class Days(
        val n: Int,
    ) : SyncLine

    data class On(
        val raw: String,
    ) : SyncLine
}

private fun Int?.orZero(): Int = this ?: 0

private fun String.firstGrapheme(): String {
    val iterator = BreakIterator.getCharacterInstance(Locale.ROOT)
    iterator.setText(this)
    return substring(0, iterator.next())
}

private fun JsonObject.string(key: String): String? = (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content

private fun String.objectOrNull(): JsonObject? =
    try {
        ApiJson.response.parseToJsonElement(this) as? JsonObject
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
