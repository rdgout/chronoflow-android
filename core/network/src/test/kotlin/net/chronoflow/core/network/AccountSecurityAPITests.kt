@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiRecoveryCodes
import net.chronoflow.core.model.api.ApiTwoFactorSetup
import net.chronoflow.core.network.support.MockApi
import net.chronoflow.core.network.support.RecordingDeadline
import net.chronoflow.core.network.support.failure
import net.chronoflow.core.network.support.json
import net.chronoflow.core.network.support.requestJson
import net.chronoflow.core.network.support.response
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class AccountSecurityAPITests {
    @get:Rule val api = MockApi()

    @Test fun everySecurityRouteUsesTheRealMethodAndBody() =
        runTest(timeout = 60.seconds) {
            val source = testSource(api.client())
            route("POST", "/v1/auth/forgot-password", """{"email":"demo@example.com"}""", 204)
            route(
                "PUT",
                "/v1/account/password",
                """{"current_password":"old","password":"new","password_confirmation":"new"}""",
                204,
            )
            route(
                "POST",
                "/v1/account/two-factor",
                """{"current_password":"old"}""",
                200,
                """{"secretKey":"secret","otpauthUrl":"otpauth://test"}""",
            )
            route(
                "POST",
                "/v1/account/two-factor/confirmation",
                """{"code":"123456"}""",
                200,
                """{"recoveryCodes":["code"]}""",
            )
            route(
                "POST",
                "/v1/account/two-factor/recovery-codes/reveal",
                """{"current_password":"old"}""",
                200,
                """{"recoveryCodes":["code"]}""",
            )
            route(
                "POST",
                "/v1/account/two-factor/recovery-codes",
                """{"current_password":"old"}""",
                200,
                """{"recoveryCodes":["code"]}""",
            )
            route(
                "DELETE",
                "/v1/account/two-factor",
                """{"current_password":"old"}""",
                200,
                json("account/snapshot.json"),
            )
            assertThat(
                source.accountSecurity(AccountSecurityChange.RequestPasswordReset("demo@example.com")),
            ).isEqualTo(AccountSecurityResult.Changed)
            assertThat(
                source.accountSecurity(AccountSecurityChange.Password("old", "new", "new")),
            ).isEqualTo(AccountSecurityResult.Changed)
            assertThat(
                source.accountSecurity(AccountSecurityChange.EnableTwoFactor("old")),
            ).isEqualTo(AccountSecurityResult.Setup(ApiTwoFactorSetup("secret", "otpauth://test")))
            val codes = AccountSecurityResult.Codes(ApiRecoveryCodes(listOf("code")))
            assertThat(source.accountSecurity(AccountSecurityChange.ConfirmTwoFactor("123456"))).isEqualTo(codes)
            assertThat(source.accountSecurity(AccountSecurityChange.RevealRecoveryCodes("old"))).isEqualTo(codes)
            assertThat(source.accountSecurity(AccountSecurityChange.RegenerateRecoveryCodes("old"))).isEqualTo(codes)
            assertThat(
                source.accountSecurity(AccountSecurityChange.DisableTwoFactor("old")) is AccountSecurityResult.Snapshot,
            ).isEqualTo(true)
            assertThat(api.server.requestCount).isEqualTo(7)
        }

    private fun route(
        method: String,
        path: String,
        expected: String,
        status: Int,
        body: String = "",
    ) {
        api.route(method, path) {
            assertThat(requestJson(it)).isEqualTo(ApiJson.response.parseToJsonElement(expected))
            assertThat(
                it.headers["Authorization"],
            ).isEqualTo(if (path.contains("forgot-password")) null else "Bearer artificial-old-token")
            response(status, body)
        }
    }

    @Test fun backendConflictKeysSurviveErrorMapping() =
        runTest(timeout = 60.seconds) {
            conflict("Two-factor authentication is already on.")
            conflict("Two-factor authentication is off.")
            conflict("Start two-factor setup again.")
            assertThat(api.server.requestCount).isEqualTo(3)
        }

    private suspend fun conflict(key: String) {
        api.reply("POST", "/v1/account/two-factor", 409, """{"code":"conflict","message":"$key"}""")
        assertThat(
            failure { testSource(api.client()).accountSecurity(AccountSecurityChange.EnableTwoFactor("password")) },
        ).isEqualTo(DataFailure.Validation(mapOf("form" to FieldMessage(key)), 409))
    }
}
