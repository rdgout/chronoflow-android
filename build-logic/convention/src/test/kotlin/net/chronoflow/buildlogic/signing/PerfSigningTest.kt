package net.chronoflow.buildlogic.signing

import org.junit.Assert.assertThrows
import org.junit.Test

class PerfSigningTest {
    @Test fun benchmarkRejectsAnInheritedUploadKey() {
        assertThrows(IllegalStateException::class.java) { PerfSigning.verify("benchmarkRelease", "upload") }
    }

    @Test fun nonMinifiedRejectsAnInheritedUploadKey() {
        assertThrows(IllegalStateException::class.java) { PerfSigning.verify("nonMinifiedRelease", "upload") }
    }

    @Test fun benchmarkRequiresAnExplicitDebugKey() {
        assertThrows(IllegalStateException::class.java) { PerfSigning.verify("benchmarkRelease", null) }
        PerfSigning.verify("benchmarkRelease", "debug")
    }

    @Test fun nonMinifiedRequiresAnExplicitDebugKey() {
        assertThrows(IllegalStateException::class.java) { PerfSigning.verify("nonMinifiedRelease", null) }
        PerfSigning.verify("nonMinifiedRelease", "debug")
    }

    @Test fun releaseCanUseUploadOrRemainUnsigned() {
        PerfSigning.verify("release", "upload")
        PerfSigning.verify("release", null)
    }
}
