package net.chronoflow.app.perf

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.chronoflow.core.model.api.ApiJson
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class DiagnosticTripPageInputTest(
    private val name: String,
) {
    @Test fun diagnosticEntryOpensTheFirstFixtureDay() {
        val input = DiagnosticTripPageInput.load(name)
        val detail = ApiJson.response.parseToJsonElement(input.bytes.decodeToString()).jsonObject
        val firstDay =
            detail
                .getValue("plan")
                .jsonObject
                .getValue("days")
                .jsonArray
                .first()
                .jsonObject
        assertEquals(firstDay.getValue("id").jsonPrimitive.content, input.args.dayId)
        assertEquals("fixture-$name", input.args.tripId)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = listOf(arrayOf("S3"), arrayOf("large60"))
    }
}
