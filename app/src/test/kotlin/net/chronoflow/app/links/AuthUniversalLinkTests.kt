package net.chronoflow.app.links

import net.chronoflow.app.PendingLaunchIntents
import net.chronoflow.core.model.api.SignedVerificationInput
import net.chronoflow.core.model.auth.AuthLink
import net.chronoflow.core.model.auth.ResetPasswordLink
import net.chronoflow.core.testing.AuthLinkUrls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthUniversalLinkTests {
    @Test fun testSignedColdAndWarmBrowsingActivitiesReachTheSameRouter() {
        val pending = PendingLaunchIntents()
        val received = mutableListOf<AuthLink>()
        val dispatcher = AuthLinkDispatcher(pending, received::add)
        val input =
            SignedVerificationInput(
                "0199a3c0-0000-7000-8000-000000000001",
                "a".repeat(40),
                1_800_000_000,
                "b".repeat(64),
            )
        val url = AuthLinkUrls.verify(input.id, input.hash, input.expires, input.signature)
        pending.captureLink(url)
        dispatcher.deliver()
        assertTrue(received.isEmpty())
        dispatcher.attach()
        assertEquals(listOf(AuthLink.Verify(input)), received)
        pending.captureLink(url)
        dispatcher.deliver()
        assertEquals(listOf(AuthLink.Verify(input), AuthLink.Verify(input)), received)
    }

    @Test fun testColdCaptureAndWarmActivityPreserveResetInputWithoutAScheme() {
        val pending = PendingLaunchIntents()
        val received = mutableListOf<AuthLink>()
        val dispatcher = AuthLinkDispatcher(pending, received::add)
        val cold = "https://app.chronoflow.net/reset-password/token%252F?email=a%2Bb%40example.com"
        pending.captureLink(cold)
        assertEquals(AuthLink.Reset(ResetPasswordLink("token%2F", "a+b@example.com")), AuthLink.parse(cold))
        pending.captureLink("https://app.chronoflow.net/reset-password?email=expired%40example.com")
        dispatcher.attach()
        assertEquals(listOf(AuthLink.Reset(ResetPasswordLink("", "expired@example.com"))), received)
    }
}
