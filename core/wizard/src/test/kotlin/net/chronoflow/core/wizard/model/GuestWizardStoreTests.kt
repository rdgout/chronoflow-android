package net.chronoflow.core.wizard.model

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.ProfilePreferences
import net.chronoflow.core.model.guest.GuestRecoveryLocation
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.testing.wizard.WizardTestClock
import net.chronoflow.core.testing.wizard.generationReview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class GuestWizardStoreTests {
    @Test fun roundTripAndInitialValues() =
        runTest(timeout = 60.seconds) {
            withStore {
                var draft = store.start()
                assertEquals(MemberWizardStep.whereTo, draft.state.step)
                assertEquals("one_way", draft.form.end.kind)
                assertEquals(3, draft.form.preparation.days)
                assertNull(draft.state.savedProfile)
                assertFalse(draft.state.firstTrip)
                assertEquals(JsonPrimitive("23:00"), draft.state.profile["habitual_bed"])
                assertEquals(JsonPrimitive("07:00"), draft.state.profile["habitual_wake"])
                draft =
                    draft.copy(
                        state =
                            draft.state.copy(
                                step = MemberWizardStep.sleep,
                                revision = 2,
                                sheet = WizardAirportSheet(0, WizardAirportSheet.Side.to),
                            ),
                        fieldErrors =
                            mapOf("review" to FieldMessage("Check your flight dates.")),
                    )
                store.persist(draft)
                assertEquals(draft, reopen().recover())
                assertFalse(GuestRecoveryLocation.file(root, StorageNamespace.Api).exists())
            }
        }

    @Test fun recoveryExpiry() =
        runTest(timeout = 60.seconds) {
            for (elapsed in listOf(1740.0, 1799.999, 1800.0, 1860.0)) {
                withStore {
                    val draft = store.start()
                    clock.advance(elapsed)
                    assertEquals("elapsed=$elapsed", if (elapsed < 1800) draft else null, store.recover())
                    if (elapsed >= 1800) rejected { store.persist(draft) }
                }
            }
        }

    @Test fun stripsRetiredValuesAndSecretsOnWriteAndLegacyRecovery() =
        runTest(timeout = 60.seconds) {
            withStore {
                val initial = store.start()
                val review = generationReview().let { it.copy(identity = it.identity.copy(guestSecret = "SECRET")) }
                val retired = JsonObject(initial.state.profile + ("pregnancy" to JsonPrimitive(true)))
                val draft =
                    initial.copy(
                        form = initial.form.copy(identity = review.identity),
                        state = initial.state.copy(review = review, profile = retired),
                    )
                store.persist(draft)
                assertClean(file.readText())
                val json = ApiJson.response.parseToJsonElement(file.readText()).jsonObject
                val state = json.getValue("state").jsonObject
                val form = json.getValue("form").jsonObject
                val legacyProfile = state.getValue("profile").jsonObject.toMutableMap()
                legacyProfile["pregnancy"] = JsonPrimitive(true)
                val legacyIdentity = form.getValue("identity").jsonObject.toMutableMap()
                legacyIdentity["guestSecret"] = JsonPrimitive("LEGACY_SECRET")
                val legacy =
                    JsonObject(
                        json +
                            mapOf(
                                "state" to JsonObject(state + ("profile" to JsonObject(legacyProfile))),
                                "form" to JsonObject(form + ("identity" to JsonObject(legacyIdentity))),
                            ),
                    )
                file.writeText(legacy.toString())
                val recovered = checkNotNull(store.recover())
                assertNull(recovered.state.profile["pregnancy"])
                assertNull(recovered.form.identity?.guestSecret)
                assertNull(
                    recovered.state.review
                        ?.identity
                        ?.guestSecret,
                )
                assertClean(file.readText())
            }
        }

    @Test fun recoveryRemembersSignInWithoutExtendingTheExpiry() =
        runTest(timeout = 60.seconds) {
            withStore {
                val initial = store.start()
                store.persist(initial.copy(state = initial.state.copy(step = MemberWizardStep.ready, signingIn = true)))
                clock.advance(1740.0)
                val reopened = reopen()
                assertEquals(GuestRecoveryGate.Decision.wait, reopened.recoveryDecision(true, false))
                assertEquals(GuestRecoveryGate.Decision.signIn, reopened.recoveryDecision(true, true))
                assertEquals(MemberWizardStep.ready, reopened.recover()?.state?.step)
                clock.advance(60.0)
                assertEquals(GuestRecoveryGate.Decision.none, reopened.recoveryDecision(true, true))
                assertFalse(file.exists())
            }
        }

    @Test fun guardsRevisionsInstancesDiscardAndSave() =
        runTest(timeout = 60.seconds) {
            withStore {
                val old = store.start()
                val first = old.copy(state = old.state.copy(revision = 3))
                store.persist(first)
                store.persist(old)
                assertEquals(first, store.recover())
                store.discard(first.state.instance)
                rejected { store.persist(first) }
                val second = store.start()
                store.discard(first.state.instance)
                rejected { store.persist(first) }
                assertEquals(second, store.recover())
                store.didSave(second.state.instance, true)
                assertNull(store.recover())
                store.discard(second.state.instance)
            }
        }

    @Test fun oneWayOnly() =
        runTest(timeout = 60.seconds) {
            withStore {
                store.start()
                val json = ApiJson.response.parseToJsonElement(file.readText()).jsonObject
                val form = json.getValue("form").jsonObject
                val end = form.getValue("end").jsonObject
                val changedEnd = JsonObject(end + ("kind" to JsonPrimitive("return_legs")))
                val changedForm = JsonObject(form + ("end" to changedEnd))
                file.writeText(JsonObject(json + ("form" to changedForm)).toString())
                assertNull(store.recover())
                assertFalse(file.exists())
            }
        }

    @Test fun writeIsAtomic() =
        runTest(timeout = 60.seconds) {
            withStore {
                val draft = store.start()
                File(file.path + ".tmp").writeText("interrupted")
                assertEquals(draft, store.recover())
                assertFalse(File(file.path + ".tmp").exists())
            }
        }

    @Test fun corruptRecordIsDeleted() =
        runTest(timeout = 60.seconds) {
            withStore {
                store.start()
                file.writeText("{broken")
                assertNull(store.recover())
                assertFalse(file.exists())
            }
        }

    private fun assertClean(bytes: String) {
        assertFalse(bytes.contains("SECRET"))
        assertFalse(ProfilePreferences.containsRetiredValues(bytes))
    }

    private suspend fun rejected(block: suspend () -> Unit) {
        try {
            block()
            fail("stale writer accepted")
        } catch (_: RecoveryRejected) {
        }
    }

    private class Fixture(
        val root: File,
        val clock: WizardTestClock,
        val reopen: () -> GuestWizardStore,
    ) {
        val file = GuestRecoveryLocation.file(root, StorageNamespace.Mock)
        val store = reopen()
    }

    private suspend fun TestScope.withStore(block: suspend Fixture.() -> Unit) {
        val root = File("build/guest-store-tests/${UUID.randomUUID()}")
        val clock = WizardTestClock()
        val io = StandardTestDispatcher(testScheduler)
        val fixture =
            Fixture(root, clock) {
                GuestWizardStore({ GuestRecoveryLocation.file(root, StorageNamespace.Mock) }, io, { clock.now })
            }
        try {
            fixture.block()
        } finally {
            root.deleteRecursively()
        }
    }
}
