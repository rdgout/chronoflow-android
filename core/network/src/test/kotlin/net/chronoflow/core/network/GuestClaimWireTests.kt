@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.api.ApiPasskeyLoginInput
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.api.RegisterInput
import net.chronoflow.core.network.support.MockApi
import net.chronoflow.core.network.support.RecordingDeadline
import net.chronoflow.core.network.support.failure
import net.chronoflow.core.network.support.json
import net.chronoflow.core.network.support.requestJson
import net.chronoflow.core.network.support.response
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class GuestClaimWireTests(
    private val label: String,
    private val method: String,
    private val live: Boolean,
) {
    @get:Rule val api = MockApi()

    @Test fun requestUsesOnlyTheClaimFieldsForItsMethod() =
        runTest(timeout = 60.seconds) {
            val source = testSource(api.client())
            val draft = if (live) "guest-token" else null
            val secret = if (live) "secret" else null
            val path =
                when (method) {
                    "signIn" -> "login"
                    "signUp" -> "register"
                    else -> "passkey/login"
                }
            api.reply("POST", "/v1/auth/$path", 200, json("auth/session.json"))
            when (method) {
                "signIn" -> {
                    source.signIn(
                        PasswordSignInInput("demo@example.com", "Password123!", "device", "phone", draft, secret),
                    )
                }

                "signUp" -> {
                    source.signUp(
                        RegisterInput(
                            "Test",
                            "demo@example.com",
                            "Password123!",
                            "Password123!",
                            "device",
                            "phone",
                            draftToken = draft,
                            inputRevision = if (live) 4 else null,
                            reviewRevision = if (live) 7 else null,
                            guestSecret = secret,
                        ),
                    )
                }

                else -> {
                    source.passkeyLogin(ApiPasskeyLoginInput("fake", assertion(), "device", "phone", draft, secret))
                }
            }
            val request = api.requests.single()
            val body = requestJson(request)!!
            assertThat(request.headers["Authorization"]).isEqualTo(null)
            assertThat(body["draft_token"]).isEqualTo(draft?.let { JsonPrimitive(it) })
            assertThat(body["guest_secret"]).isEqualTo(secret?.let { JsonPrimitive(it) })
            assertThat(body["input_revision"]).isEqualTo(if (live && method == "signUp") JsonPrimitive(4) else null)
            assertThat(body["review_revision"]).isEqualTo(if (live && method == "signUp") JsonPrimitive(7) else null)
            assertThat(body["device_id"]).isEqualTo(JsonPrimitive("device"))
            assertThat(body["device_name"]).isEqualTo(JsonPrimitive("phone"))
            assertThat(body["password"]).isEqualTo(
                if (method ==
                    "passkeyLogin"
                ) {
                    null
                } else {
                    JsonPrimitive("Password123!")
                },
            )
            assertThat(body["password_confirmation"]).isEqualTo(
                if (method ==
                    "signUp"
                ) {
                    JsonPrimitive("Password123!")
                } else {
                    null
                },
            )
            assertThat(api.server.requestCount).isEqualTo(1)
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf("signIn live", "signIn", true),
                arrayOf("signIn absent", "signIn", false),
                arrayOf("signUp live", "signUp", true),
                arrayOf("signUp absent", "signUp", false),
                arrayOf("passkeyLogin live", "passkeyLogin", true),
                arrayOf("passkeyLogin absent", "passkeyLogin", false),
            )
    }
}
