@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.network.support.MockApi
import net.chronoflow.core.network.support.RecordingDeadline
import net.chronoflow.core.network.support.failure
import net.chronoflow.core.network.support.json
import net.chronoflow.core.network.support.requestJson
import net.chronoflow.core.network.support.response
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SettingsDeleteAPITests {
    @get:Rule val api = MockApi()

    @Test fun deletionSendsPasswordUnderTheBackendFieldAndPreservesBillingFailure() =
        runTest(timeout = 60.seconds) {
            val source = testSource(api.client())
            api.reply("DELETE", "/v1/account", 204)
            source.deleteAccount("password")
            assertThat(requestJson(api.requests.single())).isEqualTo(buildJsonObject { put("password", "password") })
            val key = "Your subscription could not be cancelled. Please try deleting your account again."
            api.reply(
                "DELETE",
                "/v1/account",
                422,
                """{"code":"validation_failed","message":"Invalid","errors":{"billing":"$key"}}""",
            )
            assertThat(failure { source.deleteAccount("password") }).isEqualTo(
                DataFailure.Validation(
                    mapOf("billing" to FieldMessage(key)),
                    422,
                ),
            )
            assertThat(api.server.requestCount).isEqualTo(2)
        }
}
