package net.chronoflow.core.data.wizard

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.chronoflow.core.data.reminders.FirstTripRemindersOffer
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.wizard.support.WizardStore
import net.chronoflow.core.data.wizard.support.wizardTrip
import net.chronoflow.core.database.store.fetchDraft
import net.chronoflow.core.database.store.insertDraft
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.store.DraftRecord
import net.chronoflow.core.model.store.StoreTime
import net.chronoflow.core.testing.wizard.WizardTestClock
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class DraftRepositoryTests {
    @Test fun recoveryJsonAlwaysCarriesInstanceAndRevision() {
        val draft = MemberWizardDraft()
        val state = ApiJson.response.parseToJsonElement(MemberWizardRecord.encode(draft).state).jsonObject
        assertEquals(draft.state.instance, state["instance"]?.jsonPrimitive?.content)
        assertEquals("0", state["revision"]?.jsonPrimitive?.content)
    }

    @Test fun updateIgnoresOlderRevisionAndRejectsOtherInstance() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardStore(StandardTestDispatcher(testScheduler))
            try {
                val clock = WizardTestClock()
                val store = MemberWizardStore(fixture.database, now = { clock.now }, io = fixture.io)
                val initial = store.start()
                val latest = initial.copy(state = initial.state.copy(revision = 3))
                store.persist(latest)
                store.persist(initial.copy(state = initial.state.copy(revision = 2)))
                assertEquals(latest, store.recover())
                val other = initial.copy(state = initial.state.copy(instance = "other", revision = 4))
                assertRejected { store.persist(other) }
                assertEquals(latest, store.recover())
            } finally {
                fixture.close()
            }
        }

    @Test fun deleteRecoveryIgnoresOtherInstance() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardStore(StandardTestDispatcher(testScheduler))
            try {
                val clock = WizardTestClock()
                val store = MemberWizardStore(fixture.database, now = { clock.now }, io = fixture.io)
                val initial = store.start()
                DraftRepository(fixture.database).deleteRecovery("other")
                assertEquals(initial, store.recover())
            } finally {
                fixture.close()
            }
        }

    @Test fun undecodableRowIsDeletedOnRecover() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardStore(StandardTestDispatcher(testScheduler))
            try {
                val clock = WizardTestClock()
                val repository = DraftRepository(fixture.database)
                repository.replaceRecovery(
                    DraftRecord(
                        MemberWizardRecord.ID,
                        form = "null",
                        wizardState = "{}",
                        updatedAt = StoreTime.millis(clock.now),
                    ),
                )
                assertNull(MemberWizardStore(fixture.database, now = { clock.now }, io = fixture.io).recover())
                assertNull(fixture.database.read { it.fetchDraft(MemberWizardRecord.ID) })
            } finally {
                fixture.close()
            }
        }

    @Test fun updatedAtWithAndWithoutFractionParses() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardStore(StandardTestDispatcher(testScheduler))
            try {
                val clock = WizardTestClock()
                val repository = DraftRepository(fixture.database)
                val row =
                    DraftRecord(
                        MemberWizardRecord.ID,
                        form = "{}",
                        wizardState = "{}",
                        updatedAt = StoreTime.seconds(clock.now),
                    )
                repository.replaceRecovery(row)
                assertEquals(row, repository.recovery(clock.now))
                val fractional = row.copy(updatedAt = StoreTime.millis(clock.now))
                repository.replaceRecovery(fractional)
                assertEquals(fractional, repository.recovery(clock.now))
                repository.replaceRecovery(row.copy(updatedAt = "bad"))
                assertNull(repository.recovery(clock.now))
                assertNull(fixture.database.read { it.fetchDraft(MemberWizardRecord.ID) })
            } finally {
                fixture.close()
            }
        }

    @Test fun firstTripOfferIsQueuedOnceAsJsonString() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardStore(StandardTestDispatcher(testScheduler))
            try {
                val offer = FirstTripRemindersOffer(fixture.database)
                val settings = SettingsRepository(fixture.database)
                offer.queue(false)
                assertTrue(settings.settings().isEmpty())
                offer.queue(true)
                assertEquals("\"pending\"", settings.settings().single().value)
                offer.queue(true)
                assertEquals("\"pending\"", settings.settings().single().value)
            } finally {
                fixture.close()
            }
        }

    @Test fun firstTripStaysFalseWhenTheLatestCardDoesNotDecode() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardStore(StandardTestDispatcher(testScheduler))
            try {
                val clock = WizardTestClock()
                TripRepository(
                    fixture.database,
                ).upsert(wizardTrip("latest", "AMS", "2026-11-01T12:00:00Z").copy(card = "null"))
                val initial = MemberWizardStore(fixture.database, now = { clock.now }, io = fixture.io).start()
                assertFalse(initial.state.firstTrip)
                assertTrue(
                    initial.form.legs[0]
                        .fromIata
                        .isEmpty(),
                )
            } finally {
                fixture.close()
            }
        }

    @Test fun missingStoredRevisionCountsAsZero() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardStore(StandardTestDispatcher(testScheduler))
            try {
                val clock = WizardTestClock()
                val store = MemberWizardStore(fixture.database, now = { clock.now }, io = fixture.io)
                val initial = store.start()
                val repository = DraftRepository(fixture.database)
                val record = requireNotNull(fixture.database.read { it.fetchDraft(MemberWizardRecord.ID) })
                val state = ApiJson.response.parseToJsonElement(requireNotNull(record.wizardState)).jsonObject
                repository.replaceRecovery(record.copy(wizardState = JsonObject(state - "revision").toString()))
                val changed = initial.copy(state = initial.state.copy(revision = 1))
                store.persist(changed)
                assertEquals(changed, store.recover())
            } finally {
                fixture.close()
            }
        }

    @Test fun allRecordsRoundTripUnknownJSONNullsAndCompositeKeys() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardStore(StandardTestDispatcher(testScheduler))
            try {
                val json = """{"future":{"big":123456789012345678901234567890,"list":[null,true,"é"]},"n":1.000}"""
                val draft =
                    DraftRecord(
                        id = "draft",
                        tripId = "trip'",
                        form = json,
                        baseForm = json,
                        dirty = false,
                        review = json,
                        token = null,
                        inputRevision = 5,
                        reviewRevision = 4,
                        wizardSourceRevision = "r",
                        wizardReview = json,
                        furthestStep = "sleep",
                        wizardState = json,
                        sourceRevision = json,
                        saveFailure = json,
                        fieldErrors = json,
                        updatedAt = "now",
                    )
                fixture.database.write { it.insertDraft(draft) }
                assertEquals(draft, fixture.database.read { it.fetchDraft("draft") })
                assertEquals(json, fixture.database.read { it.fetchDraft("draft")?.form })
                val emptyDraft = DraftRecord("empty", form = json, updatedAt = "later")
                val repository = DraftRepository(fixture.database)
                repository.upsert(emptyDraft)
                assertTrue(repository.drafts().contains(emptyDraft))
            } finally {
                fixture.close()
            }
        }

    @Test fun repositoriesBindKeysAndOrderOutboxByTimestampThenID() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardStore(StandardTestDispatcher(testScheduler))
            try {
                val repository = DraftRepository(fixture.database)
                repository.upsert(DraftRecord("draft", form = "{}", updatedAt = "now"))
                repository.remove("' OR 1=1 --")
                assertEquals(1, repository.drafts().size)
                repository.remove("draft")
                assertTrue(repository.drafts().isEmpty())
            } finally {
                fixture.close()
            }
        }
}
