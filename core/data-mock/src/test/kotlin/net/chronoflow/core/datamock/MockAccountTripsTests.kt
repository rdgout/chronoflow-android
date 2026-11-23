package net.chronoflow.core.datamock

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.MutationReceipt
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.OutboxKind
import net.chronoflow.core.model.ProfileChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.AccountIdentity
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class MockAccountTripsTests {
    @Test fun readsIsolationAndSupportedMutations() =
        runTest(timeout = 60.seconds) {
            val source = mockSource()
            login(source)
            val snapshot = source.fetchAccount()
            assertEquals(demoID, snapshot.id)
            assertEquals(1, source.fetchTrips().size)
            assertEquals(
                "a".repeat(64),
                source
                    .fetchTripDetail(demoTripID)
                    .detail.checkState.planInputHash,
            )
            assertEquals(
                MutationReceipt.Identity(
                    AccountIdentity(demoID, "Changed", "demo@example.com", true, snapshot.settings.policies, false),
                ),
                source.send(OutboxChange.Profile(ProfileChange("Changed", "demo@example.com"))),
            )
            source.send(OutboxChange.Walkthrough)
            assertTrue(source.fetchIdentity().walkthroughSeen)
            source.send(OutboxChange.Policies("1.0"))
            assertNotNull(source.fetchIdentity().policies.acceptedTermsAt)
            val preferences =
                source.send(
                    OutboxChange.Settings(
                        SettingsSection.Preferences,
                        JsonObject(
                            mapOf("time_format" to JsonPrimitive("12")),
                        ),
                    ),
                ) as MutationReceipt.Account
            assertEquals(JsonPrimitive("12"), preferences.snapshot.settings.preferences["timeFormat"])
            val reminders =
                source.send(
                    OutboxChange.Settings(
                        SettingsSection.Reminders,
                        JsonObject(
                            mapOf("lead_min" to JsonPrimitive(15)),
                        ),
                    ),
                ) as MutationReceipt.Account
            assertEquals(JsonPrimitive(15), reminders.snapshot.settings.reminders["leadMin"])
            val body =
                source.send(
                    OutboxChange.Settings(
                        SettingsSection.BodyClock,
                        JsonObject(
                            mapOf("habitual_bed" to JsonPrimitive("22:00")),
                        ),
                    ),
                ) as MutationReceipt.Account
            assertEquals(JsonPrimitive("22:00"), body.snapshot.settings.bodyClock["habitualBed"])
            source.signIn(passwordInput(email = "twofactor@example.com"))
            source.passkeyLogin(passkeyInput(source.passkeyOptions(), "AQ"))
            assertTrue(source.fetchTrips().isEmpty())
            assertEquals("Demo Traveller", source.fetchIdentity().name)
            assertEquals(DataFailure.Server(404), failure { source.fetchTripDetail(demoTripID) })
            source.signOut()
            assertEquals(DataFailure.Unauthorized(401), failure { source.fetchIdentity() })
        }

    @Test fun absoluteChecksClearAndStaleHash() =
        runTest(timeout = 60.seconds) {
            val source = mockSource()
            login(source)
            val change = CheckOffChange(demoTripID, "a".repeat(64), BlockType.Sleep, "2026-10-13", 1, CheckStatus.Done)
            source.send(OutboxChange.CheckOff(change))
            source.send(OutboxChange.CheckOff(change))
            assertEquals(
                1,
                source
                    .fetchTripDetail(demoTripID)
                    .detail.checkState.marks.size,
            )
            source.send(OutboxChange.CheckOff(change.copy(status = null)))
            assertTrue(
                source
                    .fetchTripDetail(demoTripID)
                    .detail.checkState.marks
                    .isEmpty(),
            )
            assertEquals(
                DataFailure.Validation(
                    mapOf("form" to FieldMessage("This plan has changed. Refresh it before marking this step.")),
                    422,
                ),
                failure { source.send(OutboxChange.CheckOff(change.copy(status = null, planInputHash = "stale"))) },
            )
        }

    @Test fun unsupportedAndSensitivePatchRefusal() =
        runTest(timeout = 60.seconds) {
            val source = mockSource()
            login(source)

            suspend fun unsupported(kind: OutboxKind) {
                assertEquals(
                    DataFailure.Unsupported(kind),
                    failure {
                        source.send(OutboxChange.Unsupported(kind, JsonObject(emptyMap())))
                    },
                )
            }
            unsupported(OutboxKind.CompleteTrip)
            unsupported(OutboxKind.DataExport)
            unsupported(OutboxKind.Security)

            suspend fun sensitive(section: SettingsSection) {
                failure {
                    source.send(
                        OutboxChange.Settings(section, JsonObject(mapOf("password" to JsonPrimitive("secret")))),
                    )
                }
                failure { source.send(OutboxChange.Settings(section, JsonObject(emptyMap()))) }
            }
            sensitive(SettingsSection.BodyClock)
            sensitive(SettingsSection.Preferences)
            sensitive(SettingsSection.Reminders)
            failure { source.send(OutboxChange.Policies("stale")) }
            failure { source.send(OutboxChange.Profile(ProfileChange("Changed", "other@example.com"))) }
        }

    @Test fun claimIsAccountAndSecretBound() =
        runTest(timeout = 60.seconds) {
            val seed =
                scenarioState().copy(
                    claims =
                        mapOf("test-draft" to MockClaim.of(demoID, demoTripID, "test-secret")),
                )
            val source = mockSource(seed = seed)
            val input = passwordInput().copy(draftToken = "test-draft", guestSecret = "test-secret")
            assertEquals(demoTripID, (source.signIn(input) as SignInResult.Session).result.tripId)
            assertNull((source.signIn(input.copy(guestSecret = "wrong")) as SignInResult.Session).result.tripId)
            assertNull(
                source
                    .passkeyLogin(
                        passkeyInput(
                            source.passkeyOptions(),
                            "AQ",
                        ).copy(draftToken = "test-draft", guestSecret = "test-secret"),
                    ).tripId,
            )
        }

    @Test fun sameDeviceReplacesOnlyItsToken() =
        runTest(timeout = 60.seconds) {
            val clock = MockClock()
            val store = MemoryMockStateStore()
            val source = mockSource(clock, store)
            val first = login(source)
            val otherDevice = login(source, device = twoFactorID)
            login(source)
            assertEquals(
                DataFailure.Unauthorized(401),
                failure {
                    mockSource(clock, store, bearer = { first.token }).fetchIdentity()
                },
            )
            val retained = mockSource(clock, store, bearer = { otherDevice.token })
            assertEquals(demoID, retained.fetchIdentity().id)
            clock.advance(89 * 86400.0)
            retained.fetchIdentity()
            clock.advance(89 * 86400.0)
            retained.fetchIdentity()
            clock.advance(90 * 86400.0)
            assertEquals(DataFailure.Unauthorized(401), failure { retained.fetchIdentity() })
        }

    @Test fun invalidSettingsAndMergedBodyClock() =
        runTest(timeout = 60.seconds) {
            val source = mockSource()
            login(source)

            suspend fun invalid(
                section: SettingsSection,
                key: String,
                value: JsonElement,
            ) {
                failure {
                    source.send(
                        OutboxChange.Settings(
                            section,
                            JsonObject(
                                mapOf(key to value),
                            ),
                        ),
                    )
                }
            }
            invalid(SettingsSection.Preferences, "time_format", JsonPrimitive("13"))
            invalid(SettingsSection.Preferences, "hidden_categories", JsonArray(listOf(JsonPrimitive("fake"))))
            invalid(SettingsSection.Preferences, "show_optional_items", JsonNull)
            invalid(SettingsSection.Reminders, "light", JsonPrimitive("yes"))
            invalid(SettingsSection.Reminders, "lead_min", JsonPrimitive(10))
            invalid(SettingsSection.BodyClock, "habitual_bed", JsonPrimitive("25:00"))
            invalid(SettingsSection.BodyClock, "habitual_bed", JsonPrimitive("07:00"))
            invalid(SettingsSection.BodyClock, "cups_tea", JsonPrimitive(1))
            invalid(SettingsSection.BodyClock, "free_day_status", JsonPrimitive("declared"))
            invalid(SettingsSection.BodyClock, "cups_tea", JsonPrimitive(21))
            invalid(SettingsSection.BodyClock, "caffeine_habit", JsonPrimitive("cups"))
            invalid(SettingsSection.BodyClock, "caffeine_sensitive", JsonNull)
            invalid(SettingsSection.BodyClock, "blue_blockers", JsonPrimitive("rated"))
            source.send(
                OutboxChange.Settings(
                    SettingsSection.BodyClock,
                    JsonObject(
                        mapOf(
                            "free_day_status" to JsonPrimitive("declared"),
                            "free_day_bed" to JsonPrimitive("23:30"),
                            "free_day_wake" to JsonPrimitive("08:00"),
                            "caffeine_habit" to JsonPrimitive("cups"),
                            "cups_tea" to JsonPrimitive(1),
                            "blue_blockers" to JsonPrimitive("rated"),
                            "blue_blocker_mdfd" to JsonPrimitive(1.3),
                        ),
                    ),
                ),
            )
            assertEquals(JsonPrimitive("1.3"), source.fetchAccount().settings.bodyClock["blueBlockerMdfd"])
            source.send(
                OutboxChange.Settings(
                    SettingsSection.BodyClock,
                    JsonObject(
                        mapOf(
                            "free_day_status" to JsonPrimitive("same_as_workdays"),
                            "blue_blockers" to JsonPrimitive("no"),
                        ),
                    ),
                ),
            )
            assertEquals(JsonNull, source.fetchAccount().settings.bodyClock["freeDayBed"])
            assertEquals(JsonNull, source.fetchAccount().settings.bodyClock["blueBlockerMdfd"])
        }

    @Test fun checkReplayKeepsTimestampAndVersionAndRejectsMissingStep() =
        runTest(timeout = 60.seconds) {
            val clock = MockClock()
            val source = mockSource(clock)
            login(source)
            val change = CheckOffChange(demoTripID, "a".repeat(64), BlockType.Sleep, "2026-10-13", 1, CheckStatus.Done)
            val first = source.send(OutboxChange.CheckOff(change))
            clock.advance(3600.0)
            assertEquals(first, source.send(OutboxChange.CheckOff(change)))
            failure { source.send(OutboxChange.CheckOff(change.copy(ordinal = 2))) }
            failure { source.send(OutboxChange.CheckOff(change.copy(ordinal = 0))) }
        }
}
