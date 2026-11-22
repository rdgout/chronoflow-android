package net.chronoflow.app.guest

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.app.AppDispatchers
import net.chronoflow.app.auth.AuthServices
import net.chronoflow.core.data.guest.GuestContinuationRepository
import net.chronoflow.core.data.guest.GuestSessionAcceptance
import net.chronoflow.core.data.session.AccountReadinessRepository
import net.chronoflow.core.data.session.SessionAccountDatabase
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.datamock.MemoryMockStateStore
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.guest.GuestRecoveryLocation
import net.chronoflow.core.model.guest.MemoryGuestSecretStore
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.toRoute
import net.chronoflow.core.sync.session.AccountReadinessSource
import net.chronoflow.core.sync.session.MemorySessionStore
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionTeardown
import net.chronoflow.core.sync.session.StoredDataReadiness
import net.chronoflow.core.testing.wizard.WizardTestClock
import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.wizard.generation.GuestWizardGeneration
import net.chronoflow.core.wizard.model.GuestWizardStore
import net.chronoflow.feature.auth.model.SecretField
import net.chronoflow.feature.auth.model.SessionAcceptor

internal class GuestAuthFixture(
    scope: TestScope,
) {
    val dispatcher = StandardTestDispatcher(scope.testScheduler)
    val dispatchers = AppDispatchers(dispatcher, dispatcher, dispatcher)
    val root = TestRoots.temporaryStoreRoot()
    val clock = WizardTestClock()
    val file = GuestRecoveryLocation.file(root, StorageNamespace.Mock)
    val guest = GuestWizardStore({ file }, dispatcher, { clock.now })
    val secrets = MemoryGuestSecretStore()
    val source =
        MockDataSource(
            store = MemoryMockStateStore(),
            seed = MockAppSeed.make(clock.now, walkthroughSeen = true),
            now = { clock.now },
            delay = { 0.0 },
            sleep = {},
            io = dispatcher,
        )
    val accounts = SessionAccountDatabase({ root }, StorageNamespace.Mock, JdbcAccountDriverFactory(), dispatcher)
    val controller = SessionController(MemorySessionStore(), accounts, SessionTeardown(sleep = {}), dispatcher)
    val readiness =
        StoredDataReadiness(
            controller,
            AccountReadinessSource {
                AccountReadinessRepository(checkNotNull(accounts.activeAccountDatabase())).values(it)
            },
            dispatcher,
        )
    val acceptance = GuestSessionAcceptance(guest, secrets, { clock.now }, dispatcher)
    val navigator = RootNavigator().apply { navigate(Route.GuestWizard) }
    val auth =
        AuthServices(
            navigator,
            scope.backgroundScope,
            dispatchers,
            source,
            SessionAcceptor(controller::accept),
            { DEVICE },
            { "phone" },
            routeAcceptedSession = { result, method ->
                acceptance.routeAcceptedSession(
                    result,
                    method,
                    controller,
                    accounts,
                    readiness,
                    scope.backgroundScope,
                    { navigator.navigate(it.toRoute()) },
                    { navigator.navigate(Route.Verify) },
                )
            },
            claim = { guest.claim(secrets) },
            guestReviewExpired = { guest.reviewExpired(it) },
        )
    val generation =
        GuestWizardGeneration(source, secrets, { DEVICE }, parentContext = scope.backgroundScope.coroutineContext)

    suspend fun seedGuestReview(): String {
        val draft = guest.start()
        val input =
            generationInput().let { input ->
                input.copy(
                    outbound =
                        input.outbound.copy(
                            legs =
                                listOf(
                                    input.outbound.legs.single().copy(
                                        departLocal = "2030-10-12T14:40",
                                        arriveLocal = "2030-10-13T09:15",
                                    ),
                                ),
                        ),
                )
            }
        val result =
            generation.review(
                draft.state.instance,
                ApiTripReviewInput(
                    input = input,
                    profile =
                        JsonObject(
                            mapOf(
                                "habitual_bed" to JsonPrimitive("23:00"),
                                "habitual_wake" to JsonPrimitive("07:00"),
                                "chronotype" to JsonPrimitive("intermediate"),
                                "age_band" to JsonPrimitive("18_64"),
                                "shift_work" to JsonPrimitive(false),
                            ),
                        ),
                ),
            )
        guest.persist(draft.copy(form = draft.form.copy(identity = result.review.identity)))
        return result.review.identity.token
    }

    suspend fun database() = checkNotNull(accounts.activeAccountDatabase())

    suspend fun continuation() = GuestContinuationRepository(database())

    suspend fun close() =
        withContext(NonCancellable + dispatcher) {
            generation.cancel()
            auth.signIn.clearSecrets()
            readiness.close()
            controller.close()
            accounts.close()
            check(root.deleteRecursively())
        }

    companion object {
        const val DEVICE = "0199a3c0-0000-7000-8000-000000000001"
    }
}

internal class GuestTestSecret(
    private var value: String = "NewPassword123!",
) : SecretField {
    override fun read(): String = value

    override fun clear() {
        value = ""
    }

    override fun toString(): String = "GuestTestSecret(***)"
}
