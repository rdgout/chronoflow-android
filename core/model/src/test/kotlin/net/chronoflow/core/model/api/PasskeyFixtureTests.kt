package net.chronoflow.core.model.api

import assertk.assertThat
import assertk.assertions.hasLength
import assertk.assertions.isEqualTo
import net.chronoflow.core.testing.Fixtures
import org.junit.Test

class PasskeyFixtureTests {
    @Test
    fun passkeyOptionsFixtureDecodes() {
        val challenge =
            ApiJson.response.decodeFromString(
                ApiPasskeyChallenge.serializer(),
                Fixtures.text("auth/passkey-options.json"),
            )
        assertThat(challenge.challengeId).hasLength(64)
        assertThat(challenge.options.rpId).isEqualTo("app.chronoflow.net")
        assertThat(challenge.options.timeout).isEqualTo(60_000)
        assertThat(challenge.options.allowCredentials.size).isEqualTo(0)
    }
}
