package net.chronoflow.core.data.wizard

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.wizard.support.WizardStore
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.ApiReviewIdentity
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.testing.wizard.WizardTestClock
import net.chronoflow.core.testing.wizard.wizardAirport
import net.chronoflow.core.wizard.model.MemberWizardDrafts
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.core.wizard.model.WizardSleepOverride
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class MemberWizardStoreSeedTest {
    @Test fun seedStartsAtCheckingWithReturnToReady() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardStore(StandardTestDispatcher(testScheduler))
            try {
                val clock = WizardTestClock()
                val settings = SettingsRepository(fixture.database)
                val bodyClock = JsonObject(mapOf("chronotype" to JsonPrimitive("intermediate")))
                settings.upsert(SettingsRecord("bodyClock", bodyClock.toString()))
                settings.upsert(SettingsRecord("sleepAnswered", "true"))
                val guest =
                    MemberWizardDrafts.guest().let {
                        it.copy(
                            form = it.form.copy(identity = ApiReviewIdentity("token", 4, 7, null, null)),
                            state =
                                it.state.copy(
                                    step = MemberWizardStep.ready,
                                    reviewedAt = clock.now,
                                    checkingAnswers = true,
                                    checkedInput = "checked",
                                    airports = mapOf("AMS" to wizardAirport("AMS", "Amsterdam", "Europe/Amsterdam")),
                                ),
                        )
                    }
                val store = MemberWizardStore(fixture.database, now = { clock.now }, io = fixture.io)
                val seed = store.start(guest)
                assertEquals(guest.form.copy(identity = null, override = WizardSleepOverride()), seed.form)
                assertEquals(guest.airports, seed.airports)
                assertEquals(WizardMode.member, seed.state.mode)
                assertEquals(MemberWizardStep.checking, seed.state.step)
                assertTrue(seed.state.returnToReady == true)
                assertTrue(seed.state.entered)
                assertFalse(seed.state.fromLastTrip)
                assertTrue(seed.state.profile.isEmpty())
                assertNull(seed.state.review)
                assertNull(seed.state.questions)
                assertNull(seed.state.checkedInput)
                assertNull(seed.state.reviewedAt)
                assertNull(seed.state.checkingAnswers)
                assertTrue(seed.fieldErrors.isEmpty())
                val storedBodyClock = settings.settings().first { it.section == "bodyClock" }.value
                assertEquals(ApiJson.response.parseToJsonElement(storedBodyClock), seed.state.savedProfile)
                assertFalse(seed.state.sleepNeeded)
                assertTrue(seed.state.firstTrip)
                assertNotEquals(guest.state.instance, seed.state.instance)
                assertEquals(seed, store.recover())
            } finally {
                fixture.close()
            }
        }

    @Test fun seedReplacesTheExistingRowAndRejectsItsWriter() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardStore(StandardTestDispatcher(testScheduler))
            try {
                val clock = WizardTestClock()
                val store = MemberWizardStore(fixture.database, now = { clock.now }, io = fixture.io)
                val old = store.start()
                val seeded = store.start(MemberWizardDrafts.guest())
                assertRejected { store.persist(old) }
                assertEquals(seeded, store.recover())
                assertEquals(
                    1L,
                    fixture.database.read {
                        it.queries.draftsQueries
                            .draftById("member-wizard")
                            .executeAsList()
                            .size
                            .toLong()
                    },
                )
            } finally {
                fixture.close()
            }
        }
}
