package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.Test
import java.time.Instant

class PlanDateTest {
    private fun parsed(value: String) = planDate(value)?.toString()

    @Test fun acceptedRowsParseToTheListedInstant() {
        val rows =
            mapOf(
                "2026-10-15" to "2026-10-15T12:00:00Z",
                "2026-10-14T23:45Z" to "2026-10-14T23:45:00Z",
                "2026-01-01T00:00:00.000Z" to "2026-01-01T00:00:00Z",
                "2026-01-15T00:05:00.123Z" to "2026-01-15T00:05:00.123Z",
                "2026-01-15T00:05:00Z" to "2026-01-15T00:05:00Z",
                "2026-01-15T00:05:00+02:00" to "2026-01-14T22:05:00Z",
                "2026-01-15T00:05:00.1Z" to "2026-01-15T00:05:00.100Z",
                "2026-01-15T00:05:00.123456789Z" to "2026-01-15T00:05:00.123456789Z",
                "2026-01-15T00:05:00+0200" to "2026-01-14T22:05:00Z",
                "2026-01-15T00:05:00+02" to "2026-01-14T22:05:00Z",
                "2026-01-15T00:05:00-05:00" to "2026-01-15T05:05:00Z",
                "2026-01-15T00:05:00z" to "2026-01-15T00:05:00Z",
                "2026-02-30T00:00:00Z" to "2026-03-02T00:00:00Z",
                "2026-01-15T24:00:00Z" to "2026-01-16T00:00:00Z",
                "2026-01-15T00:05:60Z" to "2026-01-15T00:06:00Z",
            )
        val mismatches =
            rows.mapNotNull { (input, expected) ->
                parsed(input).takeIf { it != expected }?.let { "$input: $it" }
            }
        assertThat(mismatches).isEqualTo(emptyList<String>())
    }

    @Test fun rejectedRowsAreNull() {
        val rows =
            listOf(
                "2026-01-15T00:05+02:00",
                "2026-01-15T00:05:00.1234567891Z",
                "2026-01-15T00:05:00.Z",
                "2026-01-15t00:05:00Z",
                "2026-01-15T00:05:00",
                "2026-01-15T00:05:00 Z",
                "20260115T000500Z",
                "2026-1-15",
                "2026-13-01",
                "",
                "garbage",
            )
        assertThat(rows.filter { planDate(it) != null }).isEqualTo(emptyList<String>())
    }

    @Test fun preEpochDatesParse() {
        assertThat(planDate("1969-12-31T23:30:00Z")).isEqualTo(Instant.ofEpochSecond(-1800))
        assertThat(planDate("nonsense-")).isNull()
    }
}
