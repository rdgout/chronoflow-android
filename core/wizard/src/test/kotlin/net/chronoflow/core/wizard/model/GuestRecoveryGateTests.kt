package net.chronoflow.core.wizard.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class GuestRecoveryGateTests {
    private val updated = Instant.ofEpochSecond(1_800_000_000)

    @Test fun noRecordDoesNotRecover() {
        for (signingIn in listOf(false, true)) {
            assertEquals(
                "signingIn=$signingIn",
                GuestRecoveryGate.Decision.none,
                GuestRecoveryGate.decision(null, updated, true, true, signingIn),
            )
        }
    }

    @Test fun liveRecordResumesItsDestination() {
        for (age in listOf(0L, 1_740_000L, 1_799_999L)) {
            assertEquals(
                "age=$age",
                GuestRecoveryGate.Decision.wizard,
                GuestRecoveryGate.decision(updated, updated.plusMillis(age), true, true, false),
            )
            assertEquals(
                "age=$age signingIn",
                GuestRecoveryGate.Decision.signIn,
                GuestRecoveryGate.decision(updated, updated.plusMillis(age), true, true, true),
            )
        }
    }

    @Test fun waitsForBothLaunchStages() {
        for (handoff in listOf(false, true)) {
            for (intro in listOf(false, true)) {
                for (signingIn in listOf(false, true)) {
                    val expected =
                        if (!handoff ||
                            !intro
                        ) {
                            GuestRecoveryGate.Decision.wait
                        } else if (signingIn) {
                            GuestRecoveryGate.Decision.signIn
                        } else {
                            GuestRecoveryGate.Decision.wizard
                        }
                    assertEquals(
                        "handoff=$handoff intro=$intro signingIn=$signingIn",
                        expected,
                        GuestRecoveryGate.decision(updated, updated.plusSeconds(60), handoff, intro, signingIn),
                    )
                }
            }
        }
    }

    @Test fun expiryWinsDuringTheIntroAndInTheBackground() {
        for (age in listOf(1800L, 1860L)) {
            for (finished in listOf(false, true)) {
                for (signingIn in listOf(false, true)) {
                    assertEquals(
                        "age=$age finished=$finished signingIn=$signingIn",
                        GuestRecoveryGate.Decision.expire,
                        GuestRecoveryGate.decision(updated, updated.plusSeconds(age), finished, finished, signingIn),
                    )
                }
            }
        }
    }
}
