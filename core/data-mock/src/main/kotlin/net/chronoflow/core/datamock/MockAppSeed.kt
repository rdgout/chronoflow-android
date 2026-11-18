package net.chronoflow.core.datamock

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.api.AccountSettings
import net.chronoflow.core.model.api.AccountSnapshot
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiPasskey
import net.chronoflow.core.model.api.AuthLanguage
import net.chronoflow.core.model.contracts.TripCardNextStepView
import net.chronoflow.core.model.contracts.TripListItem
import net.chronoflow.core.model.contracts.TripProgress
import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.testing.LargeTimelineFixtures
import net.chronoflow.core.testing.Template
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import net.chronoflow.core.testing.Fixtures as TestFixtures

object MockAppSeed {
    @Serializable
    data class Fixtures(
        val settings: AccountSettings,
        val trips: List<Template>,
    )

    internal val decodedFixtures: Fixtures by lazy {
        ApiJson.response.decodeFromString(Fixtures.serializer(), TestFixtures.bytes("mock-seed.json").decodeToString())
    }

    fun make(
        now: Instant,
        unverified: Boolean = false,
        walkthroughSeen: Boolean = false,
        termsPending: Boolean = false,
        includeTimelinePerformanceTrips: Boolean = false,
        zone: ZoneId = ZoneId.systemDefault(),
    ): MockServerState {
        val fixtures = decodedFixtures
        val rebased = fixtures.trips.mapIndexed { index, fixture -> rebase(fixture, index, now) }
        val performance =
            if (includeTimelinePerformanceTrips) {
                listOf("large30", "large60").map {
                    LargeTimelineFixtures.fixture(it).let { f ->
                        Template(f.card, f.detail)
                    }
                }
            } else {
                emptyList()
            }
        val values = rebased + performance
        val trips = values.map { listItem(it, now) }
        val details = values.associate { it.card.id to it.detail }
        val registered =
            mockRegistration(
                MockScenarioValues.demoID,
                "Demo Traveller",
                MockScenarioValues.demoEmail,
                AuthLanguage.En,
                now,
            )
        val settings =
            if (termsPending) {
                AccountSettings(
                    fixtures.settings.bodyClock,
                    fixtures.settings.sleepAnswered,
                    fixtures.settings.preferences,
                    fixtures.settings.reminders,
                    fixtures.settings.planCredits,
                    AccountPolicies(fixtures.settings.policies.termsVersion, fixtures.settings.policies.privacyVersion),
                )
            } else {
                fixtures.settings
            }
        val security =
            JsonObject(
                mapOf(
                    "canManagePasskeys" to JsonPrimitive(true),
                    "canManageTwoFactor" to JsonPrimitive(true),
                    "passkeyCount" to JsonPrimitive(2),
                    "passwordRules" to JsonPrimitive("minlength: 10;"),
                    "twoFactorEnabled" to JsonPrimitive(false),
                    "twoFactorConfirmedAt" to JsonNull,
                    "requiresConfirmation" to JsonPrimitive(true),
                    "retryAfterSeconds" to JsonNull,
                ),
            )
        val snapshot =
            AccountSnapshot(
                registered.id,
                registered.name,
                registered.email,
                !unverified,
                settings,
                MockSettings.readiness(fixtures.settings.bodyClock),
                JsonObject(registered.billing + ("checkout" to JsonNull)),
                security,
            )
        val demo =
            MockServerAccount(
                snapshot,
                MockScenarioValues.password,
                walkthroughSeen = walkthroughSeen,
                trips = trips,
                details = details,
                passkeys = demoPasskeys(now),
            )
        val twoFactorSnapshot =
            AccountSnapshot(
                MockScenarioValues.twoFactorID,
                snapshot.name,
                MockScenarioValues.twoFactorEmail,
                snapshot.hasVerifiedEmail,
                snapshot.settings,
                snapshot.readiness,
                snapshot.billing,
                JsonObject(
                    snapshot.security +
                        mapOf(
                            "passkeyCount" to JsonPrimitive(0),
                            "twoFactorEnabled" to JsonPrimitive(true),
                            "twoFactorConfirmedAt" to JsonPrimitive(iso(now)),
                        ),
                ),
            )
        val twoFactor =
            MockServerAccount(
                twoFactorSnapshot,
                MockScenarioValues.password,
                MockScenarioValues.twoFactorCode,
                setOf(MockScenarioValues.recoveryCode),
                walkthroughSeen,
                trips = trips,
                details = details,
                passkeys = emptyList(),
            )
        return MockServerState(listOf(demo, twoFactor)).copy(seedAnchor = anchor(now, zone))
    }

    internal fun demoPasskeys(now: Instant): List<ApiPasskey> =
        listOf(
            ApiPasskey(1, "iPhone 16", null, iso(now.minusSeconds(60 * 86400L)), iso(now)),
            ApiPasskey(2, "MacBook Pro", null, iso(now.minusSeconds(42 * 86400L)), iso(now.minusSeconds(15 * 86400L))),
        )

    fun anchor(
        now: Instant,
        zone: ZoneId,
    ): String = now.atZone(zone).toLocalDate().toString()

    fun refresh(
        stored: MockServerState,
        now: Instant,
        zone: ZoneId,
    ): MockServerState {
        val anchor = anchor(now, zone)
        var accounts = stored.accounts
        for (accountID in listOf(MockScenarioValues.demoID, MockScenarioValues.twoFactorID)) {
            var account = accounts[accountID] ?: continue
            val passkeys =
                account.passkeys ?: if (accountID == MockScenarioValues.demoID) demoPasskeys(now) else emptyList()
            val snapshot = account.snapshot
            account =
                account.copy(
                    passkeys = passkeys,
                    snapshot =
                        AccountSnapshot(
                            snapshot.id,
                            snapshot.name,
                            snapshot.email,
                            snapshot.hasVerifiedEmail,
                            snapshot.settings,
                            snapshot.readiness,
                            snapshot.billing,
                            JsonObject(snapshot.security + ("passkeyCount" to JsonPrimitive(passkeys.size))),
                        ),
                )
            for ((index, fixture) in decodedFixtures.trips.withIndex()) {
                val id = fixture.card.id
                val tripIndex = account.trips.indexOfFirst { it.card.id == id }
                val detail = account.details[id]
                if (tripIndex < 0 || detail == null) continue
                var trip = Template(account.trips[tripIndex].card, detail)
                if (stored.seedAnchor != anchor) {
                    trip = rebase(fixture, index, now)
                    val refreshed = trip.detail
                    val marks =
                        detail.checkState.marks.map { mark ->
                            val block = detail.plan.blocks.firstOrNull { it.checkKey == mark.key }
                            val key =
                                block?.let { b ->
                                    refreshed.plan.blocks
                                        .firstOrNull { it.id == b.id }
                                        ?.checkKey
                                }
                            mark.copy(key = key ?: mark.key)
                        }
                    trip =
                        Template(
                            trip.card,
                            refreshed.copy(
                                checkState = refreshed.checkState.copy(marks = marks, days = detail.checkState.days),
                            ),
                        )
                }
                account =
                    account.copy(
                        trips =
                            account.trips.mapIndexed { i, item ->
                                if (i ==
                                    tripIndex
                                ) {
                                    listItem(trip, now)
                                } else {
                                    item
                                }
                            },
                        details = account.details + (id to trip.detail),
                    )
            }
            accounts = accounts + (accountID to account)
        }
        return stored.copy(accounts = accounts, seedAnchor = anchor)
    }

    internal fun listItem(
        fixture: Template,
        now: Instant,
    ): TripListItem {
        val plan = fixture.detail.plan
        val first = plan.days.first()
        val last = plan.days.last()
        val start = first.startUtc
        val end = last.endUtc
        val status =
            when {
                now >= instant(end) -> TripStatus.Completed
                now < instant(start) -> TripStatus.Upcoming
                else -> TripStatus.Active
            }
        val next = plan.blocks.firstOrNull { it.actionable && it.checkKey != null && instant(it.endUtc) > now }
        val card =
            fixture.card.copy(
                progress = TripProgress(status, next?.let { TripCardNextStepView(it.text, it.displayTz) }),
                canComplete =
                    status != TripStatus.Completed,
            )
        val keepOffline =
            status == TripStatus.Active ||
                (
                    status != TripStatus.Completed && instant(start) >= now &&
                        instant(start) <= now.plusSeconds(7 * 86400L)
                )
        return TripListItem(
            card,
            fixture.detail.checkState.planInputHash,
            start,
            first.zones.first().tz,
            end,
            last.labelDate,
            plan.summary?.preparation?.emitted ?: 0,
            plan.summary?.adaptation,
            keepOffline,
            if (keepOffline) fixture.detail.detailVersion else null,
            if (keepOffline) fixture.detail.checkState else null,
        )
    }

    internal fun rebase(
        fixture: Template,
        index: Int,
        now: Instant,
    ): Template {
        val zone = ZoneId.of(fixture.card.from.zone)
        val source =
            if (index ==
                2
            ) {
                fixture.detail.plan.days
                    .last()
                    .endUtc
            } else {
                fixture.detail.plan.days
                    .first()
                    .startUtc
            }
        val distance =
            if (index == 0) {
                -1
            } else if (index == 2) {
                -3
            } else {
                3 + index
            }
        val target = now.atZone(zone).plusDays(distance.toLong())
        val days = ChronoUnit.DAYS.between(instant(source).atZone(zone).toLocalDate(), target.toLocalDate()).toInt()
        return MockTripFixtures.rebase(fixture, days)
    }
}
