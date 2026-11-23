package net.chronoflow.core.model.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
class GuestSessionContinuationCodecTest {
    @RunWith(Parameterized::class)
    class Valid(
        private val destination: GuestSessionContinuation,
        private val json: String,
    ) {
        @Test fun roundTripsEveryCase() {
            assertEquals(json, destination.encode())
            assertEquals(destination, GuestSessionContinuation.decode(json))
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{index}: {1}")
            fun cases(): List<Array<Any>> =
                listOf(
                    arrayOf(GuestSessionContinuation.Trip("t1"), "{\"kind\":\"trip\",\"tripId\":\"t1\"}"),
                    arrayOf(GuestSessionContinuation.MemberWizard, "{\"kind\":\"memberWizard\"}"),
                    arrayOf(GuestSessionContinuation.Trips, "{\"kind\":\"trips\"}"),
                )
        }
    }

    @RunWith(Parameterized::class)
    class Invalid(
        private val json: String,
    ) {
        @Test fun unknownOrMalformedDecodesToNull() {
            assertNull(GuestSessionContinuation.decode(json))
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{index}: {0}")
            fun cases(): List<String> =
                listOf(
                    "{\"kind\":\"x\"}",
                    "{\"kind\":\"trip\"}",
                    "{\"kind\":\"trip\",\"tripId\":\"\"}",
                    "{bad",
                    "[]",
                    "{\"kind\":\"trip\",\"tripId\":\"   \"}",
                    "{\"kind\":\"trip\",\"tripId\":1}",
                )
        }
    }
}
