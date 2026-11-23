package net.chronoflow.app.guest

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.data.guest.GuestContinuationRepository
import net.chronoflow.core.data.guest.GuestSessionAcceptance
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.datamock.MockMail
import net.chronoflow.core.datamock.MockServerState
import net.chronoflow.core.datastore.DeviceIdentityStore
import net.chronoflow.core.model.api.AuthLanguage
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.auth.GuestSessionContinuation
import net.chronoflow.core.model.device.DeviceIdentityPersistence
import net.chronoflow.core.model.guest.GuestRecoveryLocation
import net.chronoflow.core.model.guest.MemoryGuestSecretStore
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.wizard.CheckingTestClock
import net.chronoflow.core.testing.wizard.wizardAirport
import net.chronoflow.core.wizard.generation.GuestWizardGeneration
import net.chronoflow.core.wizard.model.GuestWizardStore
import net.chronoflow.core.wizard.model.MemberWizardModel
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardAirportSheet
import net.chronoflow.core.wizard.model.WizardSaveOutcome
import net.chronoflow.core.wizard.model.answerQuestion
import net.chronoflow.core.wizard.model.currentQuestion
import net.chronoflow.core.wizard.model.finishFineTune
import net.chronoflow.core.wizard.model.finishOutboundStep
import net.chronoflow.core.wizard.model.finishRoute
import net.chronoflow.core.wizard.model.finishSleep
import net.chronoflow.core.wizard.model.optionalPrompts
import net.chronoflow.core.wizard.model.savePlan
import net.chronoflow.feature.auth.model.AuthNavigation
import net.chronoflow.feature.auth.model.SessionAcceptor
import net.chronoflow.feature.auth.model.SignUpModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class GuestWizardClaimIntegrationTests(
    private val language: AuthLanguage,
) {
    @Test fun guestJourneyClaimsTheReviewedTripAndWaitsForTheAccountGates() =
        runTest(timeout = 60.seconds) {
            val io = StandardTestDispatcher(testScheduler)
            val root = TestRoots.temporaryStoreRoot()
            val now = Instant.ofEpochSecond(1_800_000_000)
            val source = MockDataSource(seed = MockServerState(), now = { now }, delay = { 0.0 }, sleep = {}, io = io)
            val secrets = MemoryGuestSecretStore()
            val device = DeviceIdentityStore(MemoryGuestJourneyDevice(), clock = { now.epochSecond.toDouble() })
            val store = GuestWizardStore({ GuestRecoveryLocation.file(root, StorageNamespace.Mock) }, io, { now })
            val draft =
                store.start().let { initial ->
                    initial.copy(
                        state =
                            initial.state.copy(
                                profile =
                                    JsonObject(
                                        initial.state.profile +
                                            mapOf(
                                                "chronotype" to JsonPrimitive("intermediate"),
                                                "shift_work" to JsonPrimitive(false),
                                            ),
                                    ),
                            ),
                    )
                }
            val checking = CheckingTestClock(testScheduler)
            val generation =
                GuestWizardGeneration(
                    source,
                    secrets,
                    { device.identity() },
                    parentContext = backgroundScope.coroutineContext,
                )
            val model = MemberWizardModel(draft, store, backgroundScope, { now }, generation, checking.clock)
            val database = AccountDatabase(root, StorageNamespace.Mock, JdbcAccountDriverFactory(), io)
            try {
                model.chooseAirport(
                    wizardAirport("AMS", "Amsterdam", "Europe/Amsterdam"),
                    WizardAirportSheet(0, WizardAirportSheet.Side.from),
                )
                model.chooseAirport(
                    wizardAirport("HND", "Tokyo", "Asia/Tokyo"),
                    WizardAirportSheet(0, WizardAirportSheet.Side.to),
                )
                model.finishRoute()
                assertEquals(MemberWizardStep.outbound, model.draft.state.step)
                model.setDateTime("2030-10-12T14:40", 0, false)
                model.setDateTime("2030-10-13T09:15", 0, true)
                assertTrue(model.finishOutbound())
                model.finishOutboundStep()
                assertEquals(MemberWizardStep.sleep, model.draft.state.step)
                assertTrue(model.finishSleep())
                finishChecking(model, checking, 0)
                assertEquals(listOf("age_band"), model.currentQuestion?.fields)
                model.answerQuestion(JsonPrimitive("18_64"))
                runCurrent()
                assertTrue(checking.deadlines.contains(1_750))
                checking.advance(1_750)
                finishChecking(model, checking, 1_750)
                assertEquals(MemberWizardStep.fineTune, model.draft.state.step)
                assertTrue(model.optionalPrompts.isEmpty())
                model.finishFineTune()
                assertEquals(MemberWizardStep.ready, model.draft.state.step)
                model.savePlan()
                assertEquals(WizardSaveOutcome.signUpRequested, model.saveOutcome)
                assertNull(model.createdTripID)
                model.flush()
                val token =
                    checkNotNull(
                        model.draft.form.identity
                            ?.token,
                    )
                assertNotNull(secrets.get(token))
                var accepted: SessionResult? = null
                val signup =
                    SignUpModel(
                        source,
                        SessionAcceptor {},
                        { device.identity() },
                        { "phone" },
                        accepted = { result, method ->
                            database.open(result.account.id)
                            GuestSessionAcceptance(store, secrets, { now }, io).accept(result, method, database)
                            accepted = result
                        },
                        onBack = {},
                        claim = { store.claim(secrets) },
                    )
                signup.editName("Traveller")
                signup.editEmail("guest-journey@example.com")
                val password = GuestTestSecret()
                val confirmation = GuestTestSecret()
                assertEquals(
                    SignUpModel.Outcome.Accepted,
                    signup.submit(
                        password,
                        confirmation,
                        language,
                        requiresGuestClaim = true,
                    ) {
                    },
                )
                val result = checkNotNull(accepted)
                assertEquals(token, result.tripId)
                assertEquals(Route.Verify, AuthNavigation.route(result.account))
                assertEquals("", password.read())
                assertEquals("", confirmation.read())
                assertNull(store.recover())
                assertNull(secrets.get(token))
                assertFalse(GuestRecoveryLocation.file(root, StorageNamespace.Mock).exists())
                val continuation = GuestContinuationRepository(database)
                assertNull(continuation.consume(Gate.Verify))
                assertNull(continuation.consume(Gate.Terms))
                assertNull(continuation.consume(Gate.Walkthrough))
                val mail = source.inbox().last() as MockMail.Verification
                source.verifyEmail(mail.input)
                assertTrue(source.fetchIdentity().hasVerifiedEmail)
                assertEquals(GuestSessionContinuation.Trip(token), continuation.consume(Gate.App))
                assertTrue(TripRepository(database).trips().isEmpty())
                assertNull(continuation.consume(Gate.App))
                assertEquals(
                    token,
                    source
                        .fetchTripDetail(token)
                        .detail.trip.id,
                )
            } finally {
                withContext(NonCancellable) {
                    model.close()
                    generation.cancel()
                    database.close()
                    check(root.deleteRecursively())
                }
            }
        }

    private fun TestScope.finishChecking(
        model: MemberWizardModel,
        clock: CheckingTestClock,
        start: Long,
    ) {
        runCurrent()
        assertNull(model.checking.failure)
        assertTrue(clock.deadlines.contains(start + 1_200))
        clock.advance(start + 1_200)
        assertTrue(clock.deadlines.contains(start + 1_500))
        clock.advance(start + 1_500)
        assertNull(model.checking.failure)
        assertTrue(model.draft.state.step != MemberWizardStep.checking)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "language={0}")
        fun languages(): List<AuthLanguage> = listOf(AuthLanguage.En, AuthLanguage.Nl)
    }
}

private class MemoryGuestJourneyDevice : DeviceIdentityPersistence {
    private var value: String? = null

    override suspend fun readDeviceID(): String? = value

    override suspend fun writeDeviceID(value: String) {
        this.value = value
    }
}
