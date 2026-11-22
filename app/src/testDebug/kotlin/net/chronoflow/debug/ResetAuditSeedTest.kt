package net.chronoflow.debug

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.datamock.MockMail
import net.chronoflow.core.datamock.MockScenarioValues
import net.chronoflow.core.model.auth.ResetPasswordLink
import net.chronoflow.feature.auth.model.AuthNavigation
import net.chronoflow.feature.auth.model.ResetPasswordModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class ResetAuditSeedTest(
    private val state: String,
) {
    @Test fun aliasPreparesTheExistingResetModelState() =
        runTest(timeout = 60.seconds) {
            val source = auditSeedSource(StandardTestDispatcher(testScheduler))
            source.requestPasswordReset(MockScenarioValues.demoEmail)
            val input = (source.inbox().last() as MockMail.PasswordReset).input
            val link = ResetPasswordLink(if (state == "expired") "" else input.token, input.email)
            val model = ResetPasswordModel(link, source, AuthNavigation(), {})
            try {
                AuthAuditSeedState.reset(model, state)
                when (state) {
                    "done" -> {
                        assertEquals(ResetPasswordModel.State.Done, model.state.value)
                    }

                    "expired" -> {
                        assertEquals(ResetPasswordModel.State.Expired, model.state.value)
                    }

                    "password" -> {
                        assertTrue(
                            model.form.state.value.fields
                                .containsKey("password"),
                        )
                    }

                    "confirmation" -> {
                        assertTrue(
                            model.form.state.value.fields
                                .containsKey("password_confirmation"),
                        )
                    }

                    "form" -> {
                        assertEquals(ResetPasswordModel.State.Form, model.state.value)
                    }

                    else -> {
                        assertEquals(AuthAuditSeedState.failure(state), model.form.state.value.failure)
                    }
                }
                assertFalse(model.form.busy)
            } finally {
                model.cancel()
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            listOf("form", "password", "confirmation", "expired", "done", "server", "throttle", "offline").map {
                arrayOf(it)
            }
    }
}
