package net.chronoflow.debug

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.feature.auth.model.AuthNavigation
import net.chronoflow.feature.auth.model.ForgotPasswordModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class ForgotAuditSeedTest(
    private val state: String,
) {
    @Test fun aliasPreparesTheExistingForgotModelState() =
        runTest(timeout = 60.seconds) {
            val model =
                ForgotPasswordModel(
                    auditSeedSource(StandardTestDispatcher(testScheduler)),
                    AuthNavigation(),
                    {},
                )
            try {
                AuthAuditSeedState.forgot(model, state)
                when (state) {
                    "form" -> {
                        assertEquals("", model.email.value)
                    }

                    "sent" -> {
                        assertEquals("unknown@example.com", model.sentEmail.value)
                    }

                    "field" -> {
                        assertTrue(
                            model.form.state.value.fields
                                .containsKey("email"),
                        )
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
            listOf("form", "sent", "field", "server", "throttle", "offline").map { arrayOf(it) }
    }
}
