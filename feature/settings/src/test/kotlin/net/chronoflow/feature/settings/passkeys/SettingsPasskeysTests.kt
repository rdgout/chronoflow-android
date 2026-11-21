package net.chronoflow.feature.settings.passkeys

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.passkey.DeterministicRegistrationProvider
import net.chronoflow.core.model.passkey.PasskeyFailure
import net.chronoflow.core.model.passkey.PasskeyRegistrationProviding
import net.chronoflow.feature.settings.security.SettingsSecurityModel
import net.chronoflow.feature.settings.security.SettingsSecurityPrompt
import net.chronoflow.feature.settings.security.passkeyName
import net.chronoflow.feature.settings.testing.FakeSecretField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class SettingsPasskeysTests {
    private val server = PasskeyServer()
    private var now = Instant.ofEpochSecond(1000)

    private fun model(provider: PasskeyRegistrationProviding? = DeterministicRegistrationProvider()) =
        SettingsSecurityModel(server::perform, { now }, provider, { "Rick's Pixel" })

    private val model = model()
    private val field = FakeSecretField("pw")

    private fun add() = model.showPrompt(SettingsSecurityPrompt.AddPasskey)

    private suspend fun remove() {
        model.loadPasskeys()
        model.showPrompt(SettingsSecurityPrompt.RemovePasskey, server.passkeys.first())
    }

    @Test fun loadShowsTheListAndAnEmptyAccount() =
        runTest(timeout = 60.seconds) {
            assertNull(model.state.value.passkeys)
            assertFalse(model.state.value.passkeysUnavailable)
            model.loadPasskeys()
            assertEquals(
                listOf("iPhone 16", "MacBook Pro"),
                model.state.value.passkeys
                    ?.map { it.name },
            )
            server.passkeys = emptyList()
            model.loadPasskeys()
            assertEquals(emptyList<Any>(), model.state.value.passkeys)
        }

    @Test fun offlineLoadIsRetryableAndKeepsAnExistingListOnLaterFailure() =
        runTest(timeout = 60.seconds) {
            server.failure = DataFailure.Offline
            model.loadPasskeys()
            assertNull(model.state.value.passkeys)
            assertTrue(model.state.value.passkeysUnavailable && model.state.value.offline)
            server.failure = null
            model.loadPasskeys()
            assertEquals(
                2,
                model.state.value.passkeys
                    ?.size,
            )
            assertFalse(model.state.value.passkeysUnavailable || model.state.value.offline)
            server.failure = DataFailure.Offline
            model.loadPasskeys()
            assertEquals(
                2,
                model.state.value.passkeys
                    ?.size,
            )
            assertFalse(model.state.value.passkeysUnavailable)
            assertTrue(model.state.value.offline)
        }

    @Test fun addChecksThePasswordAtOptionsThenRegistersWithTheDeviceNameAndRefreshesTheList() =
        runTest(timeout = 60.seconds) {
            model.loadPasskeys()
            add()
            model.submitPrompt(field)
            assertEquals("pw", server.optionsPassword)
            assertEquals("Rick's Pixel", server.added?.name)
            assertEquals("c1", server.added?.challengeId)
            assertEquals(
                listOf("internal"),
                server.added
                    ?.credential
                    ?.response
                    ?.transports,
            )
            assertNull(model.state.value.prompt)
            assertEquals("Passkey added", model.state.value.message)
            assertTrue(
                field.cleared &&
                    model.state.value.errors
                        .isEmpty(),
            )
            assertEquals(
                3L,
                model.state.value.passkeys
                    ?.first()
                    ?.id,
            )
            assertEquals(
                3,
                model.state.value.passkeys
                    ?.size,
            )
            assertFalse(model.state.value.busy)
        }

    @Test fun cancellingTheSystemSheetShowsNoErrorAndKeepsThePromptReady() =
        runTest(timeout = 60.seconds) {
            val model = model(DeterministicRegistrationProvider { throw PasskeyFailure.Canceled })
            model.showPrompt(SettingsSecurityPrompt.AddPasskey)
            model.submitPrompt(field)
            assertNull(model.state.value.message)
            assertTrue(
                model.state.value.errors
                    .isEmpty(),
            )
            assertFalse(model.state.value.busy || field.cleared)
            assertEquals("pw", field.read())
            assertEquals(SettingsSecurityPrompt.AddPasskey, model.state.value.prompt)
            assertNull(server.added)
        }

    @Test fun serverRejectionOfTheCredentialShowsTheSameMessage() =
        runTest(timeout = 60.seconds) {
            add()
            server.failsOnAdd = true
            server.failure = DataFailure.Validation(mapOf("credential" to FieldMessage("rejected")), 422)
            model.submitPrompt(field)
            assertNotNull(server.added)
            assertEquals(SettingsSecurityModel.REGISTRATION_FAILURE, model.state.value.message)
            assertNull(model.state.value.prompt)
            assertTrue(
                field.cleared &&
                    model.state.value.errors
                        .isEmpty(),
            )
            assertFalse(model.state.value.busy)
        }

    @Test fun wrongPasswordFailsAtOptionsBeforeTheSystemSheetAndStaysInThePrompt() =
        runTest(timeout = 60.seconds) {
            var providerCalls = 0
            val model =
                model(
                    DeterministicRegistrationProvider {
                        providerCalls++
                        throw PasskeyFailure.Unavailable
                    },
                )
            model.showPrompt(SettingsSecurityPrompt.AddPasskey)
            server.failsOnOptions = true
            server.failure =
                DataFailure.Validation(
                    mapOf("current_password" to FieldMessage("The provided password was incorrect.")),
                    422,
                )
            model.submitPrompt(field)
            assertEquals("pw", server.optionsPassword)
            assertEquals(0, providerCalls)
            assertNull(server.added)
            assertNull(model.state.value.message)
            assertEquals(SettingsSecurityPrompt.AddPasskey, model.state.value.prompt)
            assertEquals("The provided password was incorrect.", model.state.value.errors["current_password"])
            assertTrue(field.cleared)
        }

    @Test fun expiredChallengeShowsTheExpiryMessageAndClosesThePrompt() =
        runTest(timeout = 60.seconds) {
            add()
            server.failsOnAdd = true
            server.failure = DataFailure.Validation(mapOf("challengeId" to FieldMessage("expired")), 422)
            model.submitPrompt(field)
            assertEquals(SettingsSecurityModel.REGISTRATION_EXPIRED, model.state.value.message)
            assertNull(model.state.value.prompt)
            assertTrue(
                model.state.value.errors
                    .isEmpty(),
            )
            assertFalse(model.state.value.busy)
        }

    @Test fun offlineAddKeepsThePromptAndDisablesFurtherAdds() =
        runTest(timeout = 60.seconds) {
            add()
            server.failsOnAdd = true
            server.failure = DataFailure.Offline
            model.submitPrompt(field)
            assertNotNull(server.added)
            assertEquals("Needs a connection.", model.state.value.message)
            assertTrue(
                model.state.value.offline &&
                    model.state.value.errors
                        .isEmpty(),
            )
            assertEquals(SettingsSecurityPrompt.AddPasskey, model.state.value.prompt)
            assertFalse(model.state.value.busy)
        }

    @Test fun addThrottleLocksAfterTheCredentialWasSent() =
        runTest(timeout = 60.seconds) {
            add()
            server.failsOnAdd = true
            server.failure = DataFailure.Throttled(30.0, 429)
            model.submitPrompt(field)
            assertNotNull(server.added)
            assertTrue(model.locked)
            assertEquals("Too many attempts.", model.state.value.message)
            assertEquals(SettingsSecurityPrompt.AddPasskey, model.state.value.prompt)
        }

    @Test fun removeSendsThePasswordDropsTheRowAndRefreshes() =
        runTest(timeout = 60.seconds) {
            remove()
            model.submitPrompt(field)
            assertTrue(AccountSecurityChange.RemovePasskey(1, "pw") in server.calls)
            assertEquals(
                listOf(2L),
                model.state.value.passkeys
                    ?.map { it.id },
            )
            assertNull(model.state.value.prompt)
            assertNull(model.state.value.removal)
            assertTrue(field.cleared)
            assertEquals("Passkey removed", model.state.value.message)
        }

    @Test fun removeThrottleLocksUntilTheCountdownEnds() =
        runTest(timeout = 60.seconds) {
            remove()
            server.failure = DataFailure.Throttled(30.0, 429)
            model.submitPrompt(field)
            assertTrue(model.locked)
            assertEquals("Too many attempts.", model.state.value.message)
            assertEquals(
                2,
                model.state.value.passkeys
                    ?.size,
            )
            val calls = server.calls.size
            model.submitPrompt(FakeSecretField("pw"))
            assertEquals(calls, server.calls.size)
            now = now.plusSeconds(31)
            model.refreshLock()
            assertFalse(model.locked)
        }

    @Test fun removeWrongPasswordKeepsTheRowAndThePrompt() =
        runTest(timeout = 60.seconds) {
            remove()
            server.failure =
                DataFailure.Validation(
                    mapOf("current_password" to FieldMessage("The provided password was incorrect.")),
                    422,
                )
            model.submitPrompt(field)
            assertEquals(SettingsSecurityPrompt.RemovePasskey, model.state.value.prompt)
            assertEquals(
                2,
                model.state.value.passkeys
                    ?.size,
            )
            assertEquals("The provided password was incorrect.", model.state.value.errors["current_password"])
        }

    @Test fun leavingTheScreenClearsThePromptAndPasswords() =
        runTest(timeout = 60.seconds) {
            remove()
            model.clearSecrets()
            field.clear()
            assertNull(model.state.value.prompt)
            assertNull(model.state.value.removal)
            assertTrue(field.cleared)
        }

    @Test fun alreadyRegisteredShowsTheRegistrationMessage() =
        runTest(timeout = 60.seconds) {
            val model = model(DeterministicRegistrationProvider { throw PasskeyFailure.AlreadyRegistered })
            model.showPrompt(SettingsSecurityPrompt.AddPasskey)
            model.submitPrompt(field)
            assertEquals(SettingsSecurityModel.REGISTRATION_FAILURE, model.state.value.message)
            assertNull(model.state.value.prompt)
            assertNull(server.added)
        }

    @Test fun cancellationExceptionIsRethrownAndClearsBusy() =
        runTest(timeout = 60.seconds) {
            val model = model(DeterministicRegistrationProvider { throw CancellationException() })
            model.showPrompt(SettingsSecurityPrompt.AddPasskey)
            try {
                model.submitPrompt(field)
                fail("Expected cancellation")
            } catch (_: CancellationException) {
            }
            assertFalse(model.state.value.busy)
            assertNull(model.state.value.message)
        }

    @Test fun passkeyNameIsTrimmedAndCappedAt255CodePoints() {
        assertEquals("Rick's Pixel", passkeyName("  Rick's Pixel \n"))
        val result = passkeyName("😀".repeat(256))
        assertEquals(255, result.codePointCount(0, result.length))
        assertEquals("😀".repeat(255), result)
    }

    @Test fun rowsShowDatesInTheBoardFormat() {
        val formatter =
            net.chronoflow.core.plan.CopyLineFormatter(
                net.chronoflow.feature.settings.support.SettingsFixture.catalog,
            )
        val now = Instant.parse("2026-11-12T12:00:00Z")

        fun meta(
            used: String?,
            dutch: Boolean = false,
        ): String? =
            SettingsPasskeyText.meta(
                server.passkeys[1].copy(lastUsedAt = used),
                if (dutch) Language.Nl else Language.En,
                now,
                java.time.ZoneId.of("UTC"),
                formatter,
            ) { key, args ->
                when (key) {
                    "Added {{date}}" -> (if (dutch) "Toegevoegd " else "Added ") + args["date"]
                    "used {{date}}" -> (if (dutch) "gebruikt " else "used ") + args["date"]
                    "today" -> if (dutch) "vandaag" else "today"
                    else -> error("Unexpected key")
                }
            }
        assertEquals("Added 2 Oct 2026 · used today", meta("2026-11-12T08:00:00Z"))
        assertEquals("Added 2 Oct 2026 · used 28 Oct", meta("2026-10-28T08:00:00Z"))
        assertEquals("Added 2 Oct 2026", meta(null))
        assertEquals("Toegevoegd 2 okt 2026 · gebruikt vandaag", meta("2026-11-12T08:00:00Z", true))
    }
}
