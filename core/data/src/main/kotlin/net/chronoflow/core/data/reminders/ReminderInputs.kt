package net.chronoflow.core.data.reminders

import kotlinx.serialization.SerializationException
import net.chronoflow.core.database.AccountSql
import net.chronoflow.core.database.store.fetchCheckMarks
import net.chronoflow.core.database.store.fetchPlanVersion
import net.chronoflow.core.database.store.fetchSetting
import net.chronoflow.core.database.store.fetchTrips
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckKeyView
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.model.store.CheckMarkRecord
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.model.store.TripRecord
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.reminders.ReminderPlan

data class ReminderInputs(
    val reminders: String?,
    val preferences: String?,
    val trips: List<TripRecord>,
    val versions: List<PlanVersionRecord>,
    val checks: List<CheckMarkRecord>,
) {
    suspend fun plans(decoder: PlanDecoder): List<ReminderPlan> =
        trips.mapNotNull { trip ->
            if (!trip.eligible()) return@mapNotNull null
            try {
                val card = ApiJson.response.decodeFromString(TripCardView.serializer(), trip.card)
                if (card.progress.status == TripStatus.Completed) return@mapNotNull null
                val version =
                    versions.firstOrNull { it.tripId == trip.id && it.inputHash == trip.currentInputHash }
                        ?: return@mapNotNull null
                val detail = decoder.detail(version)
                val marks =
                    checks
                        .filter { it.tripId == trip.id && it.inputHash == version.inputHash }
                        .mapNotNull marks@{ row ->
                            val status = CheckStatus.of(row.status)
                            if (status != CheckStatus.Done && status != CheckStatus.Skipped) return@marks null
                            val key = CheckKeyView(BlockType.of(row.type), row.localDate, row.ordinal)
                            CheckMark(key, status, row.markedAt)
                        }
                ReminderPlan(card, detail, marks)
            } catch (_: SerializationException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }
        }

    companion object {
        fun fetch(sql: AccountSql): ReminderInputs {
            val trips = sql.fetchTrips().sortedBy { it.id }
            val eligible = trips.filter { it.eligible() }
            return ReminderInputs(
                sql.fetchSetting("reminders")?.value,
                sql.fetchSetting("preferences")?.value,
                trips,
                eligible.mapNotNull { sql.fetchPlanVersion(it.id, checkNotNull(it.currentInputHash)) },
                eligible
                    .flatMap { sql.fetchCheckMarks(it.id, checkNotNull(it.currentInputHash)) }
                    .filter { it.status == CheckStatus.Done.raw || it.status == CheckStatus.Skipped.raw },
            )
        }

        private fun TripRecord.eligible(): Boolean =
            keepOffline && deletedAt == null && !deletedLocally && currentInputHash != null
    }
}
