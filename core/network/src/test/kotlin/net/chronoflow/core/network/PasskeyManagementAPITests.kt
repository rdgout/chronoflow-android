@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiPasskey
import net.chronoflow.core.model.api.ApiPasskeyRegistrationInput
import net.chronoflow.core.network.support.MockApi
import net.chronoflow.core.network.support.RecordingDeadline
import net.chronoflow.core.network.support.failure
import net.chronoflow.core.network.support.json
import net.chronoflow.core.network.support.requestJson
import net.chronoflow.core.network.support.response
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class PasskeyManagementAPITests {
    @get:Rule val api = MockApi()

    @Test fun everyPasskeyRouteUsesThePhase20MethodPathBodyAndStatus() =
        runTest(timeout = 60.seconds) {
            api.reply("GET", "/v1/account/passkeys", 200, PASSKEY_LIST)
            api.reply("POST", "/v1/account/passkeys/options", 200, PASSKEY_CREATION)
            api.reply("POST", "/v1/account/passkeys", 201, PASSKEY_ADDED)
            api.reply("DELETE", "/v1/account/passkeys/12", 204)
            val source = testSource(api.client())
            assertThat(
                source.accountSecurity(AccountSecurityChange.ListPasskeys),
            ).isEqualTo(
                AccountSecurityResult.Passkeys(
                    listOf(ApiPasskey(1, "iPhone", "Apple Passwords", "2026-10-02T10:00:00.000000Z", null)),
                ),
            )
            val challenge =
                (
                    source.accountSecurity(
                        AccountSecurityChange.PasskeyCreationOptions("pw"),
                    ) as AccountSecurityResult.PasskeyCreation
                ).challenge
            assertThat(challenge.challengeId).isEqualTo("c1")
            assertThat(challenge.options.excludeCredentials.map { it.id }).isEqualTo(listOf("AQID"))
            assertThat(source.accountSecurity(AccountSecurityChange.AddPasskey(attestationInput())))
                .isEqualTo(
                    AccountSecurityResult.PasskeyAdded(
                        ApiPasskey(12, "iPhone", "Apple Passwords", "2026-11-12T09:30:00.000000Z", null),
                    ),
                )
            assertThat(
                source.accountSecurity(AccountSecurityChange.RemovePasskey(12, "pw")),
            ).isEqualTo(AccountSecurityResult.Changed)
            assertThat(requestJson(api.requests[0])).isEqualTo(null)
            val password = buildJsonObject { put("current_password", "pw") }
            assertThat(requestJson(api.requests[1])).isEqualTo(password)
            assertThat(requestJson(api.requests[3])).isEqualTo(password)
            assertThat(
                requestJson(api.requests[2]),
            ).isEqualTo(
                ApiJson.request.encodeToJsonElement(ApiPasskeyRegistrationInput.serializer(), attestationInput()),
            )
            assertThat(
                api.requests.all { it.headers["Authorization"] == "Bearer artificial-old-token" },
            ).isEqualTo(true)
            assertThat(api.server.requestCount).isEqualTo(4)
        }

    @Test fun storeAnsweringAnythingButCreatedIsAFailure() =
        runTest(timeout = 60.seconds) {
            api.reply("POST", "/v1/account/passkeys", 200, PASSKEY_ADDED)
            assertThat(
                failure {
                    testSource(
                        api.client(),
                    ).accountSecurity(AccountSecurityChange.AddPasskey(attestationInput()))
                },
            ).isEqualTo(DataFailure.Server(200))
        }

    @Test fun validationAndThrottleErrorsKeepTheirMapping() =
        runTest(timeout = 60.seconds) {
            api.reply(
                "DELETE",
                "/v1/account/passkeys/1",
                422,
                """{"code":"validation_failed","message":"The given data was invalid.","errors":{"current_password":"The provided password was incorrect."}}""",
            )
            api.reply(
                "POST",
                "/v1/account/passkeys/options",
                429,
                """{"code":"throttled","message":"Slow down.","retryAfterSeconds":30}""",
            )
            val source = testSource(api.client())
            assertThat(failure { source.accountSecurity(AccountSecurityChange.RemovePasskey(1, "bad")) })
                .isEqualTo(
                    DataFailure.Validation(
                        mapOf("current_password" to FieldMessage("The provided password was incorrect.")),
                        422,
                    ),
                )
            assertThat(failure { source.accountSecurity(AccountSecurityChange.PasskeyCreationOptions("pw")) })
                .isEqualTo(DataFailure.Throttled(30.0, 429))
        }
}
