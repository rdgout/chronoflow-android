package net.chronoflow.core.model.contracts

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.api.ApiJson
import org.junit.Test

class JsonValuesTests {
    private fun number(text: String): JsonElement = ApiJson.response.parseToJsonElement(text)

    @Test
    fun wholeNonNegativeNumbersOnly() {
        assertThat(number("2").int).isEqualTo(2)
        assertThat(number("0").int).isEqualTo(0)
        assertThat(JsonObject(emptyMap())["missing"].int).isNull()
        assertThat(number("1.5").int).isNull()
        assertThat(number("-1").int).isNull()
        assertThat(JsonPrimitive("2").int).isNull()
    }
}
