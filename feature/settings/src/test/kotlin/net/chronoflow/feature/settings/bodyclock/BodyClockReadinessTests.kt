package net.chronoflow.feature.settings.bodyclock

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BodyClockReadinessTests {
    @Test fun readinessVariantsAndUnknownKeys() {
        assertNull(BodyClockReadiness.of(null))
        assertNull(BodyClockReadiness.of(json("""{"variant":"future"}""")))
        assertEquals(
            BodyClockReadiness.Ready(false, emptyList<String>()),
            BodyClockReadiness.of(json("""{"variant":"ready"}""")),
        )
        assertEquals(
            BodyClockReadiness.Ready(true, listOf("future adjustment")),
            BodyClockReadiness.of(
                json(
                    """
{
  "variant": "adjusted",
  "adjustments": [
    {
      "key": "future adjustment"
    },
    {
      "key": true
    },
    null
  ]
}
""",
                ),
            ),
        )
        assertEquals(
            BodyClockReadiness.Excluded(listOf("future exclusion")),
            BodyClockReadiness.of(
                json("""{"variant":"excluded","exclusions":[{"key":"future exclusion"},{},"bad"]}"""),
            ),
        )
    }

    @Test fun requiredAndInvalidAnswersDetermineProgress() {
        val value =
            json(
                """
{
  "variant": "needs-answers",
  "missing": [
    {
      "question": null
    },
    {
      "question": "shift_work"
    }
  ],
  "invalidSections": [
    "sleep"
  ]
}
""",
            )
        assertEquals(BodyClockReadiness.NeedsAnswers(3, 3, 0), BodyClockReadiness.of(value))
        assertEquals(listOf("ageBand", "shiftWork"), BodyClockReadiness.missingFields(value))
        assertEquals("ageBand", BodyClockReadiness.firstMissing(value))
    }

    @Test fun singleAnswerUsesItsSeparateCatalogKey() {
        val value = json("""{"variant":"needs-answers","missing":[{}]}""")
        val state = BodyClockReadiness.of(value) as BodyClockReadiness.NeedsAnswers
        assertEquals(BodyClockReadiness.NeedsAnswers(1, 2, 1), state)
        assertEquals("We need 1 more answer before we can build a plan", state.titleKey)
    }

    @Test fun firstMissingFallsBackToInvalidGroupThenAgeBand() {
        assertEquals("habitualBed", BodyClockReadiness.firstMissing(json("""{"invalidSections":["sleep"]}""")))
        assertEquals("ageBand", BodyClockReadiness.firstMissing(json("""{"invalidSections":["future"]}""")))
        assertEquals("ageBand", BodyClockReadiness.firstMissing(null))
    }

    @Test fun declaredFreeDaysCopyHabitualTimes() {
        val answers =
            json(
                """
{
  "habitualBed": "23:00",
  "habitualWake": "07:00",
  "freeDayBed": null,
  "freeDayWake": null
}
""",
            )
        assertEquals(
            json("""{"freeDayStatus":"declared","freeDayBed":"23:00","freeDayWake":"07:00"}"""),
            BodyClockChoice.patch(answers, "freeDayStatus", "declared"),
        )
        assertEquals(
            JsonPrimitive("00:00"),
            BodyClockChoice.patch(
                json("""{"freeDayBed":"00:00","habitualBed":"23:00"}"""),
                "freeDayStatus",
                "declared",
            )["freeDayBed"],
        )
    }

    @Test fun notSureClearsChronotype() {
        assertEquals(mapOf("chronotype" to JsonNull), BodyClockChoice.patch(json("{}"), "chronotype", ""))
    }

    @Test fun yesNoUsesOnlyJsonBooleans() {
        assertEquals("yes", BodyClockChoice.yesNoSelection(JsonPrimitive(true)))
        assertEquals("no", BodyClockChoice.yesNoSelection(JsonPrimitive(false)))
        assertEquals("", BodyClockChoice.yesNoSelection(JsonPrimitive("true")))
        assertEquals(mapOf("shiftWork" to JsonPrimitive(true)), BodyClockChoice.yesNoPatch("shiftWork", "yes"))
        assertEquals(mapOf("shiftWork" to JsonPrimitive(false)), BodyClockChoice.yesNoPatch("shiftWork", "no"))
    }
}
