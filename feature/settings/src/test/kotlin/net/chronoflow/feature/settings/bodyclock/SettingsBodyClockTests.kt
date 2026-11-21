@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.bodyclock

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.ProfilePreferences
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.core.model.store.StoreFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

internal fun json(value: String): JsonObject = Json.parseToJsonElement(value) as JsonObject

class SettingsBodyClockTests {
    @Test fun invalidSleepStaysInDraftAndDoesNotReachOutboxUntilBothTimesAreValid() =
        runTest(timeout = 60.seconds) {
            val writes = mutableListOf<JsonObject>()
            val stored = json("""{"habitualBed":"23:00","habitualWake":"07:00","ageBand":"18_64"}""")
            val model = SettingsBodyClockModel(stored) { writes += it }
            model.change(BodyClockGroup.Sleep, mapOf("habitualWake" to JsonPrimitive("23:30")))
            assertTrue(writes.isEmpty())
            assertNotNull(model.state.value.errors["habitualWake"])
            model.receive(stored)
            assertEquals(JsonPrimitive("23:30"), model.state.value.answers["habitualWake"])
            model.change(BodyClockGroup.Sleep, mapOf("habitualBed" to JsonPrimitive("15:00")))
            assertEquals(1, writes.size)
            assertEquals(JsonPrimitive("15:00"), writes[0]["habitual_bed"])
            assertEquals(JsonPrimitive("23:30"), writes[0]["habitual_wake"])
            assertEquals(JsonNull, writes[0]["free_day_bed"])
            assertEquals(JsonPrimitive("18_64"), model.state.value.answers["ageBand"])
        }

    @Test fun normalizesDependentValuesAndValidatesCupsAndRating() =
        runTest(timeout = 60.seconds) {
            val writes = mutableListOf<JsonObject>()
            val model = SettingsBodyClockModel(json("{}")) { writes += it }
            model.change(BodyClockGroup.Caffeine, mapOf("caffeineHabit" to JsonPrimitive("cups")))
            assertNotNull(model.state.value.errors["cupsFilterCoffee"])
            assertTrue(writes.isEmpty())
            model.change(BodyClockGroup.Caffeine, mapOf("cupsFilterCoffee" to JsonPrimitive(2)))
            assertTrue(
                model.state.value.errors
                    .isEmpty(),
            )
            assertEquals(1, writes.size)
            model.change(BodyClockGroup.Caffeine, mapOf("cupsTea" to JsonPrimitive(21)))
            assertNotNull(model.state.value.errors["cupsFilterCoffee"])
            assertEquals(1, writes.size)
            model.change(BodyClockGroup.Caffeine, mapOf("caffeineHabit" to JsonPrimitive("non_user")))
            assertEquals(JsonNull, writes.last()["cups_filter_coffee"])
            model.change(
                BodyClockGroup.Light,
                mapOf("blueBlockers" to JsonPrimitive("rated"), "blueBlockerMdfd" to JsonPrimitive("10")),
            )
            assertNotNull(model.state.value.errors["blueBlockerMdfd"])
            model.change(BodyClockGroup.Light, mapOf("blueBlockerMdfd" to JsonPrimitive("2.1")))
            assertEquals(JsonPrimitive("2.1"), writes.last()["blue_blocker_mdfd"])
            model.change(BodyClockGroup.Light, mapOf("blueBlockers" to JsonPrimitive("no")))
            assertEquals(JsonNull, writes.last()["blue_blocker_mdfd"])
        }

    @Test fun failedLocalSaveRetainsAnswerAndAllowsRetry() =
        runTest(timeout = 60.seconds) {
            var failing = true
            val model = SettingsBodyClockModel(json("{}")) { if (failing) throw StoreFailure.Closed }
            model.change(BodyClockGroup.About, mapOf("ageBand" to JsonPrimitive("65_plus")))
            assertTrue(model.state.value.failure)
            assertFalse(BodyClockGroup.About in model.state.value.saved)
            assertEquals(JsonPrimitive("65_plus"), model.state.value.answers["ageBand"])
            failing = false
            model.change(BodyClockGroup.About, emptyMap())
            assertFalse(model.state.value.failure)
            assertTrue(BodyClockGroup.About in model.state.value.saved)
        }

    @Test fun completionOfAnOlderSaveDoesNotMarkANewerInvalidDraftAsSaved() =
        runTest(timeout = 60.seconds) {
            val began = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val model =
                SettingsBodyClockModel(json("{}")) {
                    began.complete(Unit)
                    release.await()
                }
            val first =
                launch {
                    model.change(BodyClockGroup.Sleep, json("""{"habitualBed":"23:00","habitualWake":"07:00"}"""))
                }
            began.await()
            model.change(BodyClockGroup.Sleep, mapOf("habitualWake" to JsonPrimitive("23:30")))
            release.complete(Unit)
            first.join()
            assertFalse(BodyClockGroup.Sleep in model.state.value.saved)
            assertNotNull(model.state.value.errors["habitualWake"])
            assertEquals(JsonPrimitive("23:30"), model.state.value.answers["habitualWake"])
        }

    @Test fun preferencesSaveNonNullableBooleansWithoutMedicalFields() =
        runTest(timeout = 60.seconds) {
            val writes = mutableListOf<JsonObject>()
            val model = SettingsBodyClockModel(json("{}")) { writes += it }
            assertEquals(JsonPrimitive(false), model.state.value.answers["excludeCaffeine"])
            assertEquals(JsonPrimitive(false), model.state.value.answers["flightMovementBreaks"])
            model.change(BodyClockGroup.Caffeine, mapOf("excludeCaffeine" to JsonPrimitive(true)))
            assertEquals(JsonPrimitive(true), writes.last()["exclude_caffeine"])
            model.change(BodyClockGroup.Health, mapOf("flightMovementBreaks" to JsonPrimitive(true)))
            assertEquals(JsonPrimitive(true), writes.last()["flight_movement_breaks"])
            assertEquals(JsonNull, writes.last()["shift_work"])
            val count = writes.size
            model.change(BodyClockGroup.Health, mapOf("pregnancy" to JsonPrimitive(false)))
            assertEquals(count, writes.size)
            assertNull(model.state.value.answers["pregnancy"])
            assertTrue(writes.all { ProfilePreferences.requestFields.containsAll(it.keys) })
        }

    @Test fun sleepDurationChecksClockBoundsAndCrossesMidnight() {
        assertEquals(480, SettingsBodyClockModel.sleepMinutes("23:00", "07:00"))
        assertNull(SettingsBodyClockModel.sleepMinutes("24:00", "07:00"))
        assertNull(SettingsBodyClockModel.sleepMinutes("1:00", "07:00"))
    }

    @Test fun foreignCancellationIsSilent() =
        runTest(timeout = 60.seconds) {
            val model = SettingsBodyClockModel(json("{}")) { throw StaleSessionException() }
            model.change(BodyClockGroup.About, mapOf("chronotype" to JsonPrimitive("late")))
            assertFalse(model.state.value.failure)
            assertTrue(
                model.state.value.saved
                    .isEmpty(),
            )
        }

    @Test fun incomingEqualEditClearsOverlayButAbsentDoesNotClearExplicitNull() =
        runTest(timeout = 60.seconds) {
            val model = SettingsBodyClockModel(json("{}")) {}
            model.change(BodyClockGroup.About, mapOf("chronotype" to JsonNull))
            model.receive(json("{}"))
            assertEquals(JsonNull, model.state.value.answers["chronotype"])
            model.receive(json("""{"chronotype":null}"""))
            model.receive(json("""{"chronotype":"early"}"""))
            assertEquals(JsonPrimitive("early"), model.state.value.answers["chronotype"])
        }
}
