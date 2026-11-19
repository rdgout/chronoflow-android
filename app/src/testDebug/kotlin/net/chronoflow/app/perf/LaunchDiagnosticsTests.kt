package net.chronoflow.app.perf

import net.chronoflow.app.LaunchDiagnostics
import net.chronoflow.app.StubConfig
import net.chronoflow.app.StubSession
import net.chronoflow.app.StubStored
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.feature.launch.LaunchMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class LaunchDiagnosticsTests {
    @Test fun testReadinessArgumentsHaveExplicitUnitsAndRejectInvalidValues() {
        val parsed =
            LaunchDiagnosticsParser.parse(
                mapOf(
                    "cf.session" to "in",
                    "cf.gate" to "terms",
                    "cf.stored" to "no",
                    "cf.readyDelay" to "3.5",
                ),
            )
        assertEquals(StubConfig(StubSession.SignedIn, Gate.Terms, StubStored.No, 3500.milliseconds), parsed.stub)
        for (invalid in listOf("-1", "nan", "inf", "61", "wrong")) {
            assertEquals(
                Duration.ZERO,
                LaunchDiagnosticsParser
                    .parse(
                        mapOf("cf.readyDelay" to invalid),
                    ).stub
                    ?.readyDelay,
            )
        }
        assertEquals(StubSession.Never, LaunchDiagnosticsParser.parse(mapOf("cf.session" to "never")).stub?.session)
        assertEquals(StubStored.Never, LaunchDiagnosticsParser.parse(mapOf("cf.stored" to "never")).stub?.stored)
        assertEquals(StubSession.SignedOut, LaunchDiagnosticsParser.parse(mapOf("cf.session" to "out")).stub?.session)
        assertEquals(StubSession.Native, LaunchDiagnosticsParser.parse(mapOf("cf.session" to "bad")).stub?.session)
    }

    @Test fun testDefaultsAndInvalidValues() {
        assertEquals(LaunchDiagnostics.None, LaunchDiagnosticsParser.parse(emptyMap()))
        for (invalid in listOf("nan", "-1", "inf", "60001")) {
            val parsed =
                LaunchDiagnosticsParser.parse(
                    mapOf(
                        "cf.freezeAt" to invalid,
                        "cf.mode" to "unknown",
                        "cf.readyAt" to invalid,
                    ),
                )
            assertNull(parsed.mode)
            assertEquals(4600.milliseconds, parsed.readyAt)
        }
        val slow =
            LaunchDiagnosticsParser.parse(
                mapOf(
                    "cf.mode" to "slow",
                    "cf.readyAt" to "3500",
                    "cf.mainThreadStall" to "500",
                    "cf.reduceMotion" to "1",
                ),
            )
        assertEquals(LaunchMode.Slow, slow.mode)
        assertEquals(3500.milliseconds, slow.readyAt)
        assertEquals(500.milliseconds, slow.mainThreadStall)
        assertEquals(true, slow.reduceMotion)
    }

    @Test fun usesInjectedReadinessOnlyWithAStubKey() {
        for (key in listOf("cf.session", "cf.gate", "cf.stored", "cf.readyDelay")) {
            val values = mapOf(key to "invalid")
            assertNotNull(LaunchDiagnosticsParser.parse(values).stub)
            assertNotNull(LaunchDiagnosticsParser.overrides(values).readiness)
        }
        assertNull(LaunchDiagnosticsParser.parse(mapOf("cf.mode" to "ready")).stub)
        assertNull(LaunchDiagnosticsParser.overrides(emptyMap()).readiness)
    }

    @Test fun welcomeModeIsDeferred() = assertNull(LaunchDiagnosticsParser.parse(mapOf("cf.mode" to "welcome")).mode)
}
