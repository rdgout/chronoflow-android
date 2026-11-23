package net.chronoflow.core.model.store

import net.chronoflow.core.model.api.ApiJson
import org.junit.Assert.assertEquals
import org.junit.Test

class CanonicalJsonTests {
    @Test fun nestedKeysNullsAndNumbers() {
        val value = ApiJson.response.parseToJsonElement("""{ "z": [ { "b": null, "a": 1.000 } ], "a": true }""")
        assertEquals("""{"a":true,"z":[{"a":1.000,"b":null}]}""", CanonicalJson.encode(value))
    }
}
