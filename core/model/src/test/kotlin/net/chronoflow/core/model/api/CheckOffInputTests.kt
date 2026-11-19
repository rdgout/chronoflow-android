package net.chronoflow.core.model.api

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckStatus
import org.junit.Test

class CheckOffInputTests {
    private fun encode(input: ApiCheckOffInput) =
        ApiJson.request.parseToJsonElement(
            ApiJson.request.encodeToString(ApiCheckOffInput.serializer(), input),
        ) as JsonObject

    @Test
    fun statusIsAlwaysOnTheWire() {
        val cleared = encode(ApiCheckOffInput("h", BlockType.Sleep, "2026-11-10", 1, null))
        assertThat(cleared["status"]).isEqualTo(JsonNull)
        assertThat(cleared.keys).isEqualTo(setOf("plan_input_hash", "type", "local_date", "ordinal", "status"))
        val done = encode(ApiCheckOffInput("h", BlockType.Sleep, "2026-11-10", 1, CheckStatus.Done))
        assertThat(done["status"]).isEqualTo(JsonPrimitive("done"))
        val future = encode(ApiCheckOffInput("h", BlockType.Unknown("future_step"), "2026-11-10", 1, null))
        assertThat(future["type"]).isEqualTo(JsonPrimitive("future_step"))
    }
}
