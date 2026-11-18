package net.chronoflow.feature.auth.verify

import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Test

@Logic
class VerifyCopyTest {
    @Test fun countdownUsesAsciiMinutesAndSeconds() {
        assertEquals("0:42", countdownArgument(42))
        assertEquals("10:00", countdownArgument(600))
    }

    @Test fun signOutFailuresUseTheDesignedResourceIds() {
        assertEquals(R.string.auth_needs_a_connection, signOutErrorResource(DataFailure.Offline))
        assertEquals(R.string.auth_something_went_wrong, signOutErrorResource(DataFailure.Server(503)))
    }
}
