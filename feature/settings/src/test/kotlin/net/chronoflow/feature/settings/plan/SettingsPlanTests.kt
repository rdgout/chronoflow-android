package net.chronoflow.feature.settings.plan

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.api.ApiJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SettingsPlanTests(
    private val input: String?,
    private val loaded: Boolean,
) {
    @Test fun loadingOfflineAndInvalidBalanceNeverInventCredits() {
        val credits = input?.let { JsonObject(mapOf("credits" to ApiJson.response.parseToJsonElement(it))) }
        val state = SettingsPlanState(credits, null, loaded, true)
        assertEquals(loaded, state.loaded)
        assertTrue(state.offline)
        assertFalse(state.hasBalance)
        assertNull(state.credits)
    }

    @Test fun cachedCreditsRemainAvailableOfflineAndCancellationUsesServerState() {
        val billing =
            ApiJson.response.parseToJsonElement(
                """{"subscription":{"canResume":true,
            "endsAt":"2026-12-01T00:00:00Z","paymentFailed":true,"tier":"annual"}}""",
            ) as JsonObject
        val state = SettingsPlanState(JsonObject(mapOf("credits" to JsonPrimitive(7))), billing, true, true)
        assertEquals(7, state.credits)
        assertTrue(state.hasBalance && state.offline && state.cancelled && state.paymentFailed)
        assertEquals("2026-12-01T00:00:00Z", state.date)
        assertEquals("yearly", state.interval)
        val unlimited = SettingsPlanState(JsonObject(mapOf("unlimited" to JsonPrimitive(true))), null, true, false)
        assertTrue(unlimited.hasBalance && unlimited.unlimited)
        assertNull(unlimited.credits)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "credits={0}-loaded={1}")
        fun cases() =
            listOf(
                arrayOf<Any?>(null, false),
                arrayOf<Any?>(null, true),
                arrayOf<Any?>("-1", true),
                arrayOf<Any?>("1.5", true),
                arrayOf<Any?>("\"7\"", true),
            )
    }
}
