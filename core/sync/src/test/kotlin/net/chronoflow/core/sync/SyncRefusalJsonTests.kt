package net.chronoflow.core.sync

import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.BlockType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SyncRefusalJsonTests {
    @Test fun sleepTimes() = roundTrip(SyncRefusalNotice.SleepTimes, """{"sleepTimes":{}}""")

    @Test fun stepDone() = roundTrip(SyncRefusalNotice.StepDone(BlockType.Sleep), """{"stepDone":{"_0":"sleep"}}""")

    private fun roundTrip(
        notice: SyncRefusalNotice,
        expected: String,
    ) {
        val text = ApiJson.response.encodeToString(SyncRefusalNotice.serializer(), notice)
        assertEquals(expected, text)
        assertEquals(notice, ApiJson.response.decodeFromString(SyncRefusalNotice.serializer(), text))
        val refusal = SyncRefusal("id", "check-off", failedAt = "then", lastError = "validation", notice = notice)
        val encoded = ApiJson.response.encodeToString(SyncRefusal.serializer(), refusal)
        assertFalse(encoded.contains("status"))
        assertEquals(refusal, ApiJson.response.decodeFromString(SyncRefusal.serializer(), encoded))
    }

    @Test fun legacyWithoutNotice() {
        val value =
            ApiJson.response.decodeFromString(
                SyncRefusal.serializer(),
                """{"id":"old","kind":"settings","failedAt":"then","lastError":"validation"}""",
            )
        assertNull(value.notice)
        assertNull(value.status)
    }
}
