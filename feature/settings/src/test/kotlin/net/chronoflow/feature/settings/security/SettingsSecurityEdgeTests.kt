@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.security

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.api.ApiTwoFactorSetup
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.feature.settings.testing.FakeSecretField
import net.chronoflow.feature.settings.testing.ScriptedSecurity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class SettingsSecurityEdgeTests {
    private val script = ScriptedSecurity()
    private var now = Instant.ofEpochSecond(100)
    private val model = SettingsSecurityModel(script::perform) { now }

    @Test fun offlineInPromptKeepsPromptOpen() =
        runTest(timeout = 60.seconds) {
            script.fails(DataFailure.Offline)
            submit()
            assertEquals(SettingsSecurityPrompt.Reveal, model.state.value.prompt)
            assertEquals("Needs a connection.", model.state.value.message)
        }

    @Test fun sendResetSetsCheckYourInbox() =
        runTest(timeout = 60.seconds) {
            script.returns(AccountSecurityResult.Changed)
            model.sendReset("rick@example.com")
            assertEquals("Check your inbox.", model.state.value.message)
            assertEquals(1, script.changes.size)
        }

    @Test fun staleSessionIsSilent() =
        runTest(timeout = 60.seconds) {
            script.fails(StaleSessionException())
            submit()
            assertNull(model.state.value.message)
            assertFalse(model.state.value.busy)
        }

    @Test fun cancellationIsRethrownAndBusyResets() =
        runTest(timeout = 60.seconds) {
            script.responses.addLast { awaitCancellation() }
            val job = launch { submit() }
            runCurrent()
            assertTrue(model.state.value.busy)
            job.cancelAndJoin()
            assertTrue(job.isCancelled)
            assertFalse(model.state.value.busy)
        }

    @Test fun clearSecretsKeepsRetryAt() =
        runTest(timeout = 60.seconds) {
            script.fails(DataFailure.Throttled(0.5, 429))
            submit()
            val retryAt = model.state.value.retryAt
            assertEquals(now.plusMillis(500), retryAt)
            assertEquals(1, model.remaining(now))
            model.clearSecrets()
            assertEquals(retryAt, model.state.value.retryAt)
            now = now.plusSeconds(1)
            model.refreshLock()
            assertNull(model.state.value.retryAt)
            assertFalse(model.locked)
        }

    @Test fun feedbackRevisionIncrementsPerMessage() =
        runTest(timeout = 60.seconds) {
            script.returns(AccountSecurityResult.Changed)
            model.sendReset("rick@example.com")
            val first = model.state.value.feedbackRevision
            script.returns(AccountSecurityResult.Changed)
            model.sendReset("rick@example.com")
            assertEquals(first + 1, model.state.value.feedbackRevision)
        }

    @Test fun stateToStringRedactsSecrets() {
        val state =
            SecurityState(
                setup =
                    ApiTwoFactorSetup(
                        "secret-key",
                        "otpauth://secret",
                    ),
                codes = listOf("secret-code"),
            )
        assertFalse(state.toString().contains("secret"))
        assertFalse(state.toString().contains("otpauth"))
    }

    @Test fun emptyPromptPasswordMakesNoCall() =
        runTest(timeout = 60.seconds) {
            model.showPrompt(SettingsSecurityPrompt.Enable)
            model.submitPrompt(FakeSecretField(""))
            assertTrue(script.changes.isEmpty())
        }

    private suspend fun submit() {
        model.showPrompt(SettingsSecurityPrompt.Reveal)
        model.submitPrompt(FakeSecretField("password"))
    }
}
