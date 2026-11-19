package net.chronoflow.core.testing.sync

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.api.AccountIdentity
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.api.AccountSettings
import net.chronoflow.core.model.api.AccountSnapshot
import net.chronoflow.core.model.contracts.AirportRef
import net.chronoflow.core.model.contracts.BlockParams
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.BlockView
import net.chronoflow.core.model.contracts.CheckKey
import net.chronoflow.core.model.contracts.CheckState
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.Lane
import net.chronoflow.core.model.contracts.PlanMeta
import net.chronoflow.core.model.contracts.PlanNoticesView
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.model.contracts.PlanView
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.model.contracts.SleepKind
import net.chronoflow.core.model.contracts.SleepParamsView
import net.chronoflow.core.model.contracts.SleepSetting
import net.chronoflow.core.model.contracts.TimelinePresentationView
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.contracts.TripDetailView
import net.chronoflow.core.model.contracts.TripListItem
import net.chronoflow.core.model.contracts.TripProgress
import net.chronoflow.core.model.contracts.TripStatus
import java.time.Instant

val syncAccount = "019a562b-8000-7000-8000-000000000001"
val syncOther = "019a562b-8000-7000-8000-000000000002"
val syncTrip = "019a562b-8000-7000-8000-000000000010"
val syncDate: Instant = Instant.ofEpochSecond(1_800_000_000)
val syncPolicies = AccountPolicies("1", "1")

fun identity(
    id: String = syncAccount,
    verified: Boolean = true,
): AccountIdentity = AccountIdentity(id, "Remote", "demo@example.com", verified, syncPolicies, false)

fun snapshot(id: String = syncAccount): AccountSnapshot =
    AccountSnapshot(
        id,
        "Remote",
        "demo@example.com",
        true,
        AccountSettings(
            JsonObject(mapOf("habitualBed" to JsonPrimitive("23:00"))),
            true,
            JsonObject(mapOf("timeFormat" to JsonPrimitive("24"))),
            JsonObject(emptyMap()),
            JsonObject(emptyMap()),
            syncPolicies,
        ),
        JsonObject(mapOf("ready" to JsonPrimitive(true))),
        JsonObject(emptyMap()),
        JsonObject(emptyMap()),
    )

fun check(
    hash: String = "hash",
    status: CheckStatus? = CheckStatus.Done,
): CheckOffChange = CheckOffChange(syncTrip, hash, BlockType.Sleep, "2026-11-10", 1, status)

fun card(
    id: String = syncTrip,
    hash: String = "hash",
    version: String = "v1",
    keep: Boolean = true,
): TripListItem =
    TripListItem(
        TripCardView(
            id,
            AirportRef("AMS", null, "Europe/Amsterdam"),
            AirportRef("JFK", null, "America/New_York"),
            "2026-11-10T11:00",
            "2026-11-10T13:00",
            "2026-11-10T10:00:00Z",
            "2026-11-10T18:00:00Z",
            null,
            emptyList(),
            -360,
            TripProgress(TripStatus.Active, null),
            false,
            false,
            false,
            false,
            false,
        ),
        hash,
        "2026-11-09T10:00:00Z",
        "Europe/Amsterdam",
        "2026-11-12T10:00:00Z",
        "2026-11-12",
        1,
        null,
        keep,
        version,
        null,
    )

fun detail(
    hash: String = "hash",
    version: String = "v1",
    blocks: Boolean = true,
): TripDetail =
    TripDetail(
        TripDetailView(
            syncTrip,
            card().card.from,
            card().card.to,
            "2026-11-10T11:00",
            "2026-11-10T10:00:00Z",
            "2026-11-12T10:00:00Z",
            -360,
            emptyList(),
        ),
        PlanView(
            PlanStatus.Ok,
            emptyList(),
            emptyList(),
            PlanMeta(hash, null, null),
            null,
            emptyList(),
            if (blocks) {
                listOf(
                    BlockView(
                        "sleep",
                        Lane.Sleep,
                        BlockType.Sleep,
                        "2026-11-10T20:00:00Z",
                        "2026-11-11T04:00:00Z",
                        "Europe/Amsterdam",
                        null,
                        Priority.Core,
                        CopyLine("Sleep", emptyList()),
                        null,
                        BlockParams.Sleep(SleepParamsView(SleepKind.Main, SleepSetting.Bed, 480)),
                        true,
                        CheckKey(BlockType.Sleep, "2026-11-10", 1),
                    ),
                )
            } else {
                emptyList()
            },
            emptyList(),
        ),
        PlanNoticesView(emptyList(), emptyList()),
        TimelinePresentationView(emptyList(), emptyList(), emptyList()),
        CheckState(hash, emptyList(), emptyList()),
        version,
    )
