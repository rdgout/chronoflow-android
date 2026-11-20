package net.chronoflow.core.data.wizard

import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.store.StoreApplication
import net.chronoflow.core.data.store.encoded
import net.chronoflow.core.data.store.objectValue
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.store.deleteSetting
import net.chronoflow.core.database.store.fetchSetting
import net.chronoflow.core.database.store.saveSetting
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.contracts.ProfilePreferences
import net.chronoflow.core.model.session.SessionMutationPermit
import net.chronoflow.core.model.session.SyncEpoch
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.model.store.StoreTime
import net.chronoflow.core.wizard.generation.TripGenerationEffects
import java.time.Instant

fun TripGenerationEffects.Companion.storing(
    database: AccountDatabase,
    source: DataSource,
    epoch: SyncEpoch,
    permit: SessionMutationPermit? = null,
    requestSync: suspend () -> Unit,
    now: () -> Instant,
): TripGenerationEffects =
    TripGenerationEffects(
        mergeProfile = { patch ->
            val child = epoch.child()
            database.validate(child)
            database.write(child, permit) { sql ->
                val stored = sql.fetchSetting("bodyClock")?.let { objectValue(it.value) }
                val body = (stored ?: JsonObject(emptyMap())).toMutableMap()

                fun merge(patch: JsonObject) {
                    ProfilePreferences.request(patch).forEach { (key, value) ->
                        body[ProfilePreferences.camel(key)] = value
                    }
                }
                merge(patch)
                StoreApplication.pending(sql).forEach { row ->
                    val change = OutboxPayload.change(row)
                    if (change is OutboxChange.Settings && change.section == SettingsSection.BodyClock) {
                        merge(change.patch)
                    }
                }
                val cleaned = ProfilePreferences.bodyClock(JsonObject(body))
                sql.saveSetting(SettingsRecord("bodyClock", encoded(cleaned)))
                sql.deleteSetting("readiness")
                if (patch.keys.any { it in sleepFields }) sql.saveSetting(SettingsRecord("sleepAnswered", "true"))
            }
        },
        pullCreatedTrip = { id ->
            val child = epoch.child()
            database.validate(child)
            val trips = source.fetchTrips()
            child.check()
            val at = StoreTime.seconds(now())
            database.write(child, permit) { sql -> TripRepository.applyCards(trips, at, sql) }
            val detail = source.fetchTripDetail(id)
            child.check()
            database.write(child, permit) { sql -> TripRepository.applyDetail(detail, id, at, sql) }
        },
        requestSync = { if (epoch.isValid) requestSync() },
    )

private val sleepFields =
    setOf(
        "habitual_bed",
        "habitual_wake",
        "free_day_status",
        "free_day_bed",
        "free_day_wake",
        "chronotype",
    )
