package net.chronoflow.debug

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.EmailVerificationStatus
import net.chronoflow.feature.auth.model.VerificationContext
import net.chronoflow.feature.auth.model.VerifyEmailModel
import net.chronoflow.feature.auth.model.VerifyState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class VerifyAuditSeedTest(
    private val state: String,
) {
    @Test fun aliasPreparesTheExistingVerificationState() =
        runTest(timeout = 60.seconds) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val countdown = state in setOf("countdown", "throttle", "failedThrottle")
            val context = VerificationContext("fixture-account", "fixture@example.com", 1)
            val source =
                object : DataSource by auditSeedSource(dispatcher) {
                    override suspend fun verificationStatus() = EmailVerificationStatus(context.email, false, 0)

                    override suspend fun resendVerificationEmail(): EmailVerificationStatus {
                        if (state != "countdown") throw DataFailure.Throttled(60.0, 429)
                        return EmailVerificationStatus(context.email, false, 60)
                    }
                }
            val model =
                VerifyEmailModel(context, source, {
                    auditSeedNow.epochSecond.toDouble()
                }, { context }, { false }, {}, {}, scope = backgroundScope, main = dispatcher, io = dispatcher)
            try {
                model.activate()
                AuthAuditSeedState.verify(model, state)
                assertEquals(if (state.startsWith("failed")) VerifyState.Failed else VerifyState.Waiting, model.state)
                assertEquals(AuthAuditSeedState.failure(state), model.uiState.value.form.failure)
                assertEquals(if (countdown) 60 else 0, model.remaining)
                assertEquals(!countdown, model.canResend)
            } finally {
                model.cancel()
                model.scope.coroutineContext[Job]?.cancelAndJoin()
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            listOf(
                "waiting",
                "countdown",
                "failed",
                "server",
                "throttle",
                "offline",
                "failedServer",
                "failedThrottle",
                "failedOffline",
            ).map {
                arrayOf(it)
            }
    }
}
