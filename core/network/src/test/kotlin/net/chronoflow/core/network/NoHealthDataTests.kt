@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.network.support.MockApi
import net.chronoflow.core.network.support.RecordingDeadline
import net.chronoflow.core.network.support.failure
import net.chronoflow.core.network.support.json
import net.chronoflow.core.network.support.requestJson
import net.chronoflow.core.network.support.response
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class NoHealthDataTests {
    @get:Rule val api = MockApi()

    @Test fun patchEncodingSendsOnlyNeutralPreferencesAndRejectsHealthFieldsBeforeTransport() =
        runTest(timeout = 60.seconds) {
            api.reply("PATCH", "/v1/account/body-clock", 200, json("account/snapshot.json"))
            val source = testSource(api.client())
            val patch =
                buildJsonObject {
                    put("exclude_caffeine", true)
                    put("flight_movement_breaks", false)
                }
            source.send(OutboxChange.Settings(SettingsSection.BodyClock, patch))
            assertThat(requestJson(api.requests.single())).isEqualTo(patch)
            rejects(source, "bipolar")
            rejects(source, "eye_disease")
            rejects(source, "pregnancy")
            rejects(source, "vte")
            rejects(source, "sleep_phase_disorder")
            rejects(source, "oral_contraception")
            assertThat(api.server.requestCount).isEqualTo(1)
        }

    private suspend fun rejects(
        source: ApiDataSource,
        key: String,
    ) {
        val result =
            failure {
                source.send(
                    OutboxChange.Settings(SettingsSection.BodyClock, buildJsonObject { put(key, false) }),
                )
            }
        assertThat(result is DataFailure.Validation).isEqualTo(true)
    }
}
