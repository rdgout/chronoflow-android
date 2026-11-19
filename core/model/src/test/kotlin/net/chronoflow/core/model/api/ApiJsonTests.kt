package net.chronoflow.core.model.api

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.junit.Test

class ApiJsonTests {
    @Serializable
    private data class Optional(
        val a: String,
        val b: String?,
    )

    @Serializable
    private data class Required(
        val a: String,
    )

    @Serializable
    private data class Request(
        val a: String?,
        val b: String? = null,
    )

    @Test
    fun responseIgnoresUnknownKeysAndTreatsAbsentNullableAsNull() {
        val value = ApiJson.response.decodeFromString(Optional.serializer(), """{"a":"x","extra":1}""")
        assertThat(value.a).isEqualTo("x")
        assertThat(value.b).isNull()
    }

    @Test
    fun responseFailsOnAbsentRequiredKey() {
        assertFailure {
            ApiJson.response.decodeFromString(
                Required.serializer(),
                "{}",
            )
        }.isInstanceOf<SerializationException>()
    }

    @Test
    fun requestWritesRequiredNullsAndOmitsOptionalKeys() {
        assertThat(ApiJson.request.encodeToString(Request.serializer(), Request(null))).isEqualTo("""{"a":null}""")
        assertThat(ApiJson.request.encodeToString(Request.serializer(), Request(null, "y")))
            .isEqualTo("""{"a":null,"b":"y"}""")
    }
}
