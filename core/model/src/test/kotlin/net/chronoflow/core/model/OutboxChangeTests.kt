package net.chronoflow.core.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.model.api.ApiJson
import org.junit.Test

class OutboxChangeTests {
    @Test fun profileChangeIsExactlyNameAndEmail() {
        val encoded =
            ApiJson.request.encodeToString(
                ProfileChange.serializer(),
                ProfileChange("Test", "demo@example.com"),
            )
        assertThat(ApiJson.response.parseToJsonElement(encoded)).isEqualTo(
            ApiJson.response.parseToJsonElement("""{"name":"Test","email":"demo@example.com"}"""),
        )
    }
}
