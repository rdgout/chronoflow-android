package net.chronoflow.feature.launch.model

import net.chronoflow.feature.launch.LaunchController
import net.chronoflow.feature.launch.LaunchOverrides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class LaunchProcessPolicyTests(
    private val saved: Boolean,
    private val token: String?,
    private val first: Boolean,
    private val expected: LaunchKind,
) {
    @Test fun classifyMatchesTheProcessAndSavedState() =
        assertEquals(expected, LaunchProcessPolicy.classify(saved, token, "same", first))

    @Test fun claimsOverlayOnlyForColdProcesses() {
        assertTrue(LaunchProcessPolicy.claimsOverlay(LaunchKind.Cold))
        assertTrue(LaunchProcessPolicy.claimsOverlay(LaunchKind.ProcessDeathRestore))
        assertFalse(LaunchProcessPolicy.claimsOverlay(LaunchKind.Recreated))
        assertFalse(LaunchProcessPolicy.claimsOverlay(LaunchKind.Warm))
    }

    @Test fun splashExitFadesOnlyUnclaimedWarmActivities() {
        for (kind in LaunchKind.entries) {
            for (claimed in listOf(true, false)) {
                assertEquals(
                    if (kind == LaunchKind.Warm &&
                        !claimed
                    ) {
                        SplashExit.Fade300
                    } else {
                        SplashExit.RemoveNow
                    },
                    SplashExitPolicy.forActivity(kind, claimed),
                )
            }
        }
    }

    @Test fun firstConnectionIsProcessScoped() {
        val process = LaunchController(moon = { error("no drawing") })
        assertTrue(process.claimOverlay(LaunchOverrides.None))
        assertFalse(process.claimOverlay(LaunchOverrides.None))
        assertFalse(process.claimOverlay(LaunchOverrides.None))
        val nextProcess = LaunchController(moon = { error("no drawing") })
        assertTrue(nextProcess.claimOverlay(LaunchOverrides.None))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "saved={0}, token={1}, first={2}: {3}")
        fun cases(): List<Array<Any?>> =
            listOf(
                arrayOf(false, null, true, LaunchKind.Cold),
                arrayOf(false, null, false, LaunchKind.Warm),
                arrayOf(true, "same", true, LaunchKind.Recreated),
                arrayOf(true, "same", false, LaunchKind.Recreated),
                arrayOf(true, null, true, LaunchKind.ProcessDeathRestore),
                arrayOf(true, null, false, LaunchKind.ProcessDeathRestore),
                arrayOf(true, "other", true, LaunchKind.ProcessDeathRestore),
                arrayOf(true, "other", false, LaunchKind.ProcessDeathRestore),
            )
    }
}
