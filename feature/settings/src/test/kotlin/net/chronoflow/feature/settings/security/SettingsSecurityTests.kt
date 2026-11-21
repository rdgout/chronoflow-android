@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.security

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.api.ApiRecoveryCodes
import net.chronoflow.core.model.api.ApiTwoFactorSetup
import net.chronoflow.feature.settings.testing.FakeSecretField
import net.chronoflow.feature.settings.testing.ScriptedSecurity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class SettingsSecurityTests {
    private val script = ScriptedSecurity()
    private val now = Instant.ofEpochSecond(100)
    private val model = SettingsSecurityModel(script::perform, now = { now })

    @Test fun promptWorksAgainAfterLeavingAndReturning() =
        runTest(timeout = 60.seconds) {
            script.returns(codes("one"))
            model.showPrompt(SettingsSecurityPrompt.Reveal)
            model.submitPrompt(FakeSecretField("pw"))
            assertEquals(listOf("one"), model.state.value.codes)
            model.clearSecrets()
            assertTrue(
                model.state.value.codes
                    .isEmpty(),
            )
            assertNull(model.state.value.prompt)
            script.returns(codes("one"))
            model.showPrompt(SettingsSecurityPrompt.Reveal)
            assertEquals(SettingsSecurityPrompt.Reveal, model.state.value.prompt)
            model.submitPrompt(FakeSecretField("pw"))
            assertEquals(2, script.changes.size)
            assertEquals(listOf("one"), model.state.value.codes)
        }

    @Test fun lateResultCannotRestoreSecretsOrFinishANewRequestStartNewRequestFalse() = late(false)

    @Test fun lateResultCannotRestoreSecretsOrFinishANewRequestStartNewRequestTrue() = late(true)

    private fun late(startNewRequest: Boolean) =
        runTest(timeout = 60.seconds) {
            val previousResponse = CompletableDeferred<AccountSecurityResult>()
            val currentResponse = CompletableDeferred<AccountSecurityResult>()
            script.responses.addLast { previousResponse.await() }
            script.responses.addLast { currentResponse.await() }
            model.showPrompt(SettingsSecurityPrompt.Reveal)
            val previous = launch { model.submitPrompt(FakeSecretField("pw")) }
            runCurrent()
            assertTrue(model.state.value.busy)
            model.clearSecrets()
            assertFalse(model.state.value.busy)
            assertNull(model.state.value.prompt)
            val current =
                if (startNewRequest) {
                    model.showPrompt(SettingsSecurityPrompt.Reveal)
                    launch { model.submitPrompt(FakeSecretField("pw")) }.also { runCurrent() }
                } else {
                    null
                }
            previousResponse.complete(codes("late"))
            previous.join()
            assertTrue(
                model.state.value.codes
                    .isEmpty(),
            )
            assertEquals(startNewRequest, model.state.value.busy)
            if (current != null) {
                assertEquals(SettingsSecurityPrompt.Reveal, model.state.value.prompt)
                currentResponse.complete(codes("current"))
                current.join()
                assertEquals(listOf("current"), model.state.value.codes)
                assertFalse(model.state.value.busy)
                assertNull(model.state.value.prompt)
            }
        }

    @Test fun passwordSuccessAndCancelPromptClearSecrets() =
        runTest(timeout = 60.seconds) {
            script.returns(AccountSecurityResult.Changed)
            val current = FakeSecretField("old")
            val next = FakeSecretField("New password1!")
            val confirmation = FakeSecretField("New password1!")
            model.changePassword(current, next, confirmation)
            assertEquals(
                listOf(
                    AccountSecurityChange.Password(
                        "old",
                        "New password1!",
                        "New password1!",
                    ),
                ),
                script.changes,
            )
            assertEquals("Password updated", model.state.value.message)
            assertTrue(current.cleared && next.cleared && confirmation.cleared)
            model.showPrompt(SettingsSecurityPrompt.Disable)
            model.cancelPrompt()
            assertNull(model.state.value.prompt)
            assertEquals(1, script.changes.size)
        }

    @Test fun passwordValidationAndSubmissionClearCredentialsAndReportOffline() =
        runTest(timeout = 60.seconds) {
            val current = FakeSecretField("old")
            val next = FakeSecretField("New password1!")
            val wrong = FakeSecretField("different")
            model.changePassword(current, next, wrong)
            assertTrue(script.changes.isEmpty())
            assertEquals("The passwords don't match.", model.state.value.errors["password_confirmation"])
            assertFalse(current.cleared || next.cleared || wrong.cleared)
            script.fails(DataFailure.Offline)
            val confirmation = FakeSecretField("New password1!")
            model.changePassword(current, next, confirmation)
            assertEquals(1, script.changes.size)
            assertTrue(current.cleared && next.cleared && confirmation.cleared)
            assertEquals("Needs a connection.", model.state.value.message)
            assertTrue(model.state.value.offline)
            assertFalse(model.state.value.busy)
        }

    @Test fun wrongPasswordKeepsPromptOpenAndSetupSurvivesConfirmationFailure() =
        runTest(timeout = 60.seconds) {
            script.fails(
                DataFailure.Validation(
                    mapOf("current_password" to FieldMessage("The provided password was incorrect.")),
                    422,
                ),
            )
            model.showPrompt(SettingsSecurityPrompt.Enable)
            val wrong = FakeSecretField("wrong")
            model.submitPrompt(wrong)
            assertEquals(SettingsSecurityPrompt.Enable, model.state.value.prompt)
            assertTrue(wrong.cleared)
            assertNotNull(model.state.value.errors["current_password"])
            script.returns(AccountSecurityResult.Setup(ApiTwoFactorSetup("secret", "otpauth://test")))
            model.submitPrompt(FakeSecretField("correct"))
            assertNull(model.state.value.prompt)
            assertEquals(
                "secret",
                model.state.value.setup
                    ?.secretKey,
            )
            val short = FakeSecretField("1")
            model.confirm(short)
            assertNotNull(model.state.value.errors["code"])
            assertFalse(short.cleared)
            script.fails(DataFailure.Server(503))
            val code = FakeSecretField("123456")
            model.confirm(code)
            assertTrue(code.cleared)
            assertEquals(
                "secret",
                model.state.value.setup
                    ?.secretKey,
            )
            assertTrue(
                model.state.value.codes
                    .isEmpty(),
            )
            assertTrue(model.state.value.twoFactorActivationFailed)
            assertTrue(
                model.state.value.errors
                    .isEmpty(),
            )
            assertFalse(model.state.value.busy)
            script.returns(codes("one", "two"))
            model.confirm(FakeSecretField("123456"))
            assertNull(model.state.value.setup)
            assertEquals(listOf("one", "two"), model.state.value.codes)
            assertEquals("Two-factor is on. Save your recovery codes now.", model.state.value.message)
            model.clearSecrets()
            assertTrue(
                model.state.value.codes
                    .isEmpty(),
            )
            assertNull(model.state.value.setup)
        }

    @Test fun failedTwoFactorSetupExplainsThatTheAccountIsUnchanged() =
        runTest(timeout = 60.seconds) {
            script.fails(DataFailure.Server(503))
            model.showPrompt(SettingsSecurityPrompt.Enable)
            val secret = FakeSecretField("password")
            model.submitPrompt(secret)
            assertTrue(secret.cleared)
            assertTrue(model.state.value.twoFactorActivationFailed)
            assertEquals(SettingsSecurityPrompt.Enable, model.state.value.prompt)
            assertNull(model.state.value.setup)
            assertTrue(
                model.state.value.codes
                    .isEmpty(),
            )
            assertTrue(
                model.state.value.errors
                    .isEmpty(),
            )
            assertFalse(model.state.value.busy)
            model.cancelPrompt()
            assertFalse(model.state.value.twoFactorActivationFailed)
        }

    @Test fun throttlingBlocksFurtherRequestsUntilDeadline() =
        runTest(timeout = 60.seconds) {
            script.fails(DataFailure.Throttled(60.0, 429))
            model.showPrompt(SettingsSecurityPrompt.Reveal)
            model.submitPrompt(FakeSecretField("password"))
            assertTrue(model.locked)
            assertEquals(30, model.remaining(Instant.ofEpochSecond(130)))
            model.submitPrompt(FakeSecretField("password"))
            assertEquals(1, script.changes.size)
        }

    private fun codes(vararg values: String) = AccountSecurityResult.Codes(ApiRecoveryCodes(values.toList()))
}
