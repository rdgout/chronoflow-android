package net.chronoflow.core.model.api

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.ProfilePreferences
import net.chronoflow.core.testing.Fixtures
import org.junit.Test

class AccountContractsTests {
    private fun snapshot() =
        ApiJson.response.decodeFromString(AccountSnapshot.serializer(), Fixtures.text("account/snapshot.json"))

    @Test
    fun snapshotDecodesWithTheNeutralBodyClock() {
        val body = snapshot().settings.bodyClock
        val allowed =
            ProfilePreferences.requestFields.map(ProfilePreferences::camel) +
                listOf("excludeCaffeine", "flightMovementBreaks", "invalidDrafts")
        assertThat(body.keys - allowed.toSet()).isEmpty()
        assertThat(body["invalidDrafts"]).isEqualTo(JsonObject(emptyMap()))
        assertThat(body["excludeCaffeine"]).isEqualTo(JsonPrimitive(false))
        assertThat(body["flightMovementBreaks"]).isEqualTo(JsonPrimitive(false))
    }

    @Test
    fun readinessIsRecomputedFromTheBodyClock() {
        assertThat(snapshot().readiness["variant"]).isEqualTo(JsonPrimitive("needs-answers"))
    }

    @Test
    fun encodeThenDecodeIsEqual() {
        val value = snapshot()
        val text = ApiJson.response.encodeToString(AccountSnapshot.serializer(), value)
        assertThat(ApiJson.response.decodeFromString(AccountSnapshot.serializer(), text)).isEqualTo(value)
    }
}
