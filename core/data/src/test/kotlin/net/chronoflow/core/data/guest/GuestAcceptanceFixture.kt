package net.chronoflow.core.data.guest

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.data.session.AccountReadinessRepository
import net.chronoflow.core.data.session.AccountWiper
import net.chronoflow.core.data.session.GuestSecrets
import net.chronoflow.core.data.session.SessionAccountDatabase
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.api.SessionAccount
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.contracts.ApiReviewIdentity
import net.chronoflow.core.model.guest.GuestRecoveryLocation
import net.chronoflow.core.model.guest.MemoryGuestSecretStore
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.sync.session.AccountReadinessSource
import net.chronoflow.core.sync.session.MemorySessionStore
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionTeardown
import net.chronoflow.core.sync.session.StoredDataReadiness
import net.chronoflow.core.testing.wizard.WizardTestClock
import net.chronoflow.core.testing.wizard.wizardAirport
import net.chronoflow.core.wizard.model.GuestWizardStore
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardSleepOverride
import java.io.File

internal class GuestAcceptanceFixture(
    val io: CoroutineDispatcher,
) {
    val root: File = TestRoots.temporaryStoreRoot()
    val clock = WizardTestClock()
    val secrets = MemoryGuestSecretStore()
    val credentials = MemorySessionStore()
    val accounts = SessionAccountDatabase({ root }, StorageNamespace.Mock, JdbcAccountDriverFactory(), io)
    val guest = GuestWizardStore({ GuestRecoveryLocation.file(root, StorageNamespace.Mock) }, io, { clock.now })
    private val wiper =
        AccountWiper(
            root,
            StorageNamespace.Mock,
            credentials,
            object : GuestSecrets {
                override suspend fun deleteAll() = secrets.deleteAll()
            },
            io,
        )
    val controller =
        SessionController(
            credentials,
            accounts,
            SessionTeardown(
                sleep = { awaitCancellation() },
                beginWipe = wiper::begin,
                erase = wiper::erase,
                finishWipe = wiper::finish,
            ),
            io,
        )
    val readiness =
        StoredDataReadiness(
            controller,
            AccountReadinessSource {
                AccountReadinessRepository(checkNotNull(accounts.activeAccountDatabase())).values(it)
            },
            io,
        )
    val acceptance = GuestSessionAcceptance(guest, secrets, { clock.now }, io)

    fun result(
        tripID: String? = null,
        verified: Boolean = true,
        accepted: Boolean = true,
        seen: Boolean = true,
    ): SessionResult =
        SessionResult(
            "session-token",
            SessionAccount(
                ACCOUNT,
                "Traveller",
                "test@example.com",
                verified,
                AccountPolicies("2", "2", if (accepted) "2" else null),
                seen,
            ),
            tripID,
        )

    suspend fun seedGuest(): MemberWizardDraft {
        val initial = guest.start()
        val draft =
            initial.copy(
                form =
                    initial.form.copy(
                        legs =
                            listOf(
                                initial.form.legs[0].copy(
                                    fromIata = "AMS",
                                    toIata = "HND",
                                    departLocal = "2027-03-28T12:00",
                                    arriveLocal = "2027-03-29T09:00",
                                ),
                            ),
                        preparation = initial.form.preparation.copy(days = 2),
                        identity = ApiReviewIdentity("guest-token", 4, 7, null, null),
                        override = WizardSleepOverride(enabled = true, habitualBed = "22:00"),
                    ),
                state =
                    initial.state.copy(
                        profile =
                            JsonObject(
                                initial.state.profile +
                                    mapOf(
                                        "age_band" to JsonPrimitive("65_plus"),
                                        "shift_work" to JsonPrimitive(false),
                                    ),
                            ),
                        step = MemberWizardStep.ready,
                        checkedInput = "01",
                        reviewedAt = clock.now,
                        airports = mapOf("AMS" to wizardAirport("AMS", "Amsterdam", "Europe/Amsterdam")),
                    ),
            )
        guest.persist(draft)
        secrets.set("secret", "guest-token")
        return draft
    }

    suspend fun database() = checkNotNull(accounts.activeAccountDatabase())

    suspend fun continuation() = GuestContinuationRepository(database())

    suspend fun close() =
        withContext(NonCancellable + io) {
            readiness.close()
            controller.close()
            accounts.close()
            check(root.deleteRecursively())
        }

    companion object {
        const val ACCOUNT = "019a562b-8000-7000-8000-000000000001"
        const val OTHER = "019a562b-8000-7000-8000-000000000002"
    }
}
