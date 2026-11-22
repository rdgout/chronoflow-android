package net.chronoflow.core.data.guest

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.wizard.DraftRepository
import net.chronoflow.core.data.wizard.MemberWizardStore
import net.chronoflow.core.data.wizard.assertRejected
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.auth.AuthenticationMethod
import net.chronoflow.core.model.auth.GuestSessionContinuation
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.core.wizard.model.WizardSleepOverride
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class GuestSessionAcceptanceLoginTests(
    private val method: AuthenticationMethod,
) {
    @Test fun loginWithoutTripSeedsOnlyJourneyIntoTheAccountWizard() =
        runTest(timeout = 60.seconds) {
            val fixture = GuestAcceptanceFixture(StandardTestDispatcher(testScheduler))
            try {
                val guest = fixture.seedGuest()
                val result = fixture.result()
                fixture.controller.accept(result)
                val database = fixture.database()
                val settings = SettingsRepository(database)
                settings.upsert(SettingsRecord("bodyClock", """{"habitualBed":"21:00","habitualWake":"05:00"}"""))
                settings.upsert(SettingsRecord("sleepAnswered", "true"))
                val saved = settings.settings()
                val body = ApiJson.response.parseToJsonElement(saved.first { it.section == "bodyClock" }.value)
                val store = MemberWizardStore(database, now = { fixture.clock.now }, io = fixture.io)
                val replaced = store.start()
                fixture.acceptance.accept(result, method, database)
                val seeded = checkNotNull(store.recover())
                assertEquals(guest.form.copy(identity = null, override = WizardSleepOverride()), seeded.form)
                assertEquals(guest.airports, seeded.airports)
                assertEquals(WizardMode.member, seeded.state.mode)
                assertEquals(MemberWizardStep.checking, seeded.state.step)
                assertTrue(seeded.state.returnToReady == true)
                assertTrue(seeded.state.entered)
                assertFalse(seeded.state.fromLastTrip)
                assertTrue(seeded.state.profile.isEmpty())
                assertNull(seeded.state.review)
                assertNull(seeded.state.questions)
                assertNull(seeded.state.checkedInput)
                assertNull(seeded.state.reviewedAt)
                assertNull(seeded.state.checkingAnswers)
                assertTrue(seeded.fieldErrors.isEmpty())
                assertEquals(body, seeded.state.savedProfile)
                assertFalse(seeded.state.sleepNeeded)
                assertNotEquals(guest.state.instance, seeded.state.instance)
                assertNotEquals(replaced.state.instance, seeded.state.instance)
                assertRejected { store.persist(replaced) }
                assertNull(fixture.guest.recover())
                assertNull(fixture.secrets.get("guest-token"))
                assertEquals(GuestSessionContinuation.MemberWizard, fixture.continuation().consume(Gate.App))
                assertTrue(DraftRepository(database).drafts().isEmpty())
                assertEquals(saved, settings.settings())
            } finally {
                fixture.close()
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "method={0}")
        fun methods(): List<AuthenticationMethod> = listOf(AuthenticationMethod.Password, AuthenticationMethod.Passkey)
    }
}
