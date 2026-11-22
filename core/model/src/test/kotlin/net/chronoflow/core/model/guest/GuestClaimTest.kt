package net.chronoflow.core.model.guest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GuestClaimTest {
    @Test fun toStringHidesTheSecret() {
        val claim = GuestClaim("token", 4, 7, "private-claim-value")
        assertFalse(claim.toString().contains(claim.guestSecret))
        assertEquals(
            "GuestClaim(draftToken=token, inputRevision=4, reviewRevision=7, guestSecret=***)",
            claim.toString(),
        )
    }
}
