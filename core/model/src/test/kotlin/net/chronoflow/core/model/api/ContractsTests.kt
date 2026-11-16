package net.chronoflow.core.model.api

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.FieldMessage
import org.junit.Test

class ContractsTests {
    @Test
    fun unknownJSONAndFailureInterpolation() {
        val text = """{"future":{"null":null,"list":[true,3.25,"x"]}}"""
        val element = ApiJson.response.parseToJsonElement(text)
        assertThat(element is JsonObject).isEqualTo(true)
        assertThat(ApiJson.response.encodeToString(JsonObject.serializer(), element as JsonObject)).isEqualTo(text)
        val max = FieldMessage("Use no more than :max characters.", mapOf("max" to JsonPrimitive(255)))
        assertThat(max.interpolationKey).isEqualTo("Use no more than %lld characters.")
        val min = FieldMessage("At least {{min}}.", mapOf("min" to JsonPrimitive(12)))
        assertThat(min.interpolationKey).isEqualTo("At least %lld.")
    }

    @Test
    fun failureInterpolationOrdersArgumentsByName() {
        val message =
            FieldMessage(
                "From :min to {{max}} for :attribute.",
                mapOf("min" to JsonPrimitive(1), "max" to JsonPrimitive(12), "attribute" to JsonPrimitive("name")),
            )
        assertThat(message.interpolationKey).isEqualTo("From %3\$lld to %2\$lld for %1\$@.")
        assertThat(message.catalogArguments).containsExactly(JsonPrimitive("name"), JsonPrimitive(12), JsonPrimitive(1))
    }
}
