package net.chronoflow.core.model.api

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.testing.Fixtures
import org.junit.Test

class ErrorEnvelopeTests {
    private fun envelope(path: String) =
        ApiJson.response.decodeFromString(ApiErrorEnvelope.serializer(), Fixtures.text(path))

    private fun JsonElement?.number(): Int? = (this as? JsonPrimitive)?.takeIf { !it.isString }?.content?.toInt()

    @Test
    fun validationFailedKeepsFieldKeysAndArgs() {
        val envelope = envelope("errors/validation_failed.json")
        assertThat(envelope.errors?.get("password")).isEqualTo("Use no more than :max characters.")
        assertThat(
            envelope.args
                ?.get("password")
                ?.get("max")
                .number(),
        ).isEqualTo(255)
    }

    @Test
    fun absentMembersAreNull() {
        val envelope = envelope("errors/throttled.json")
        assertThat(envelope.errors).isNull()
        assertThat(envelope.args).isNull()
    }

    @Test
    fun throttledCarriesRetryAfter() {
        assertThat(envelope("errors/throttled.json").retryAfterSeconds.number()).isEqualTo(37)
        assertThat(envelope("wizard/throttled.json").retryAfterSeconds.number()).isEqualTo(42)
    }

    @Test
    fun wizardErrorsKeepTheirFieldPaths() {
        assertThat(envelope("wizard/unknown-field.json").errors!!.keys).containsOnly("outbound.legs.0.surprise")
        assertThat(envelope("wizard/journey.json").errors!!.keys)
            .containsOnly(
                "outbound.legs.0.stay.bed_available_from_local",
                "return.legs.0.arrive_local",
                "trip_end_nights",
            )
        assertThat(envelope("wizard/profile.json").errors!!.keys)
            .containsOnly("profile", "profile.age_band", "profile.habitual_bed")
        val expired = envelope("wizard/expired.json")
        assertThat(expired.code).isEqualTo("validation_failed")
        assertThat(expired.errors!!.keys).containsOnly("review_expired")
        assertThat(envelope("wizard/credits.json").errors!!["credits"]).isEqualTo("Not enough credits")
        assertThat(envelope("wizard/capacity.json").errors!!["review"]).isEqualTo("Try saving again shortly.")
    }
}
