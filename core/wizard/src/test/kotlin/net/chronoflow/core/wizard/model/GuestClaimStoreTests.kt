package net.chronoflow.core.wizard.model

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.ApiReviewIdentity
import net.chronoflow.core.model.guest.GuestClaim
import net.chronoflow.core.model.guest.GuestSecretStore
import net.chronoflow.core.model.guest.MemoryGuestSecretStore
import net.chronoflow.core.testing.wizard.WizardTestClock
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class GuestClaimStoreTests {
    @Test fun providerNeedsALiveDraftAndItsKeychainSecret() =
        runTest(timeout = 60.seconds) {
            withStore {
                val secrets = MemoryGuestSecretStore()
                assertNull(store.claim(secrets))
                val initial = store.start()
                assertNull(store.claim(secrets))
                store.persist(
                    initial.copy(
                        form = initial.form.copy(identity = ApiReviewIdentity("guest-token", 4, 7, null, null)),
                    ),
                )
                assertNull(store.claim(secrets))
                secrets.set("secret", "guest-token")
                assertEquals(GuestClaim("guest-token", 4, 7, "secret"), store.claim(secrets))
                clock.advance(1800.0)
                assertNull(store.claim(secrets))
            }
        }

    @Test fun expiredClaimReturnsToReadyAndInvalidatesTheReview() =
        runTest(timeout = 60.seconds) {
            withStore {
                val initial = store.start()
                val draft =
                    initial.copy(
                        form = initial.form.copy(identity = ApiReviewIdentity("token", 1, 2, null, null)),
                        state =
                            initial.state.copy(
                                step = MemberWizardStep.checking,
                                checkingAnswers = true,
                                reviewedAt = clock.now,
                                profile = JsonObject(initial.state.profile + ("age_band" to JsonPrimitive("18_64"))),
                            ),
                    )
                store.persist(draft)
                store.reviewExpired(GuestClaim("token", 1, 2, "secret"))
                val recovered = checkNotNull(store.recover())
                assertEquals(MemberWizardStep.ready, recovered.state.step)
                assertTrue(recovered.state.returnToReady == true)
                assertNull(recovered.state.checkingAnswers)
                assertNull(recovered.state.reviewedAt)
                assertEquals(draft.state.profile, recovered.state.profile)
                assertEquals(draft.form, recovered.form)
                assertEquals(draft.state.revision + 1, recovered.state.revision)
            }
        }

    @Test fun mismatchedClaimLeavesTheDraftUnchanged() =
        runTest(timeout = 60.seconds) {
            withStore {
                val initial = store.start()
                store.persist(
                    initial.copy(form = initial.form.copy(identity = ApiReviewIdentity("token", 1, 2, null, null))),
                )
                val before = file.readBytes()
                store.reviewExpired(GuestClaim("token", 1, 3, "secret"))
                assertArrayEquals(before, file.readBytes())
            }
        }

    @Test fun claimIsNullWhenTheDraftChangesDuringTheSecretRead() =
        runTest(timeout = 60.seconds) {
            withStore {
                val initial = store.start()
                val draft =
                    initial.copy(
                        form = initial.form.copy(identity = ApiReviewIdentity("token", 1, 2, null, null)),
                    )
                store.persist(draft)
                val secrets =
                    object : GuestSecretStore by MemoryGuestSecretStore() {
                        override suspend fun get(token: String): String {
                            val changed =
                                draft.copy(
                                    form = draft.form.copy(identity = ApiReviewIdentity("new-token", 2, 3, null, null)),
                                )
                            store.persist(changed)
                            return "secret"
                        }
                    }
                assertNull(store.claim(secrets))
            }
        }

    private class Fixture(
        val file: File,
        val clock: WizardTestClock,
        val store: GuestWizardStore,
    )

    private suspend fun TestScope.withStore(block: suspend Fixture.() -> Unit) {
        val root = File("build/guest-claim-tests/${UUID.randomUUID()}")
        val file = File(root, "guest-wizard.json")
        val clock = WizardTestClock()
        val store = GuestWizardStore({ file }, StandardTestDispatcher(testScheduler), { clock.now })
        try {
            Fixture(file, clock, store).block()
        } finally {
            root.deleteRecursively()
        }
    }
}
