package net.chronoflow.core.model.auth

import net.chronoflow.core.model.api.SignedVerificationInput
import net.chronoflow.core.testing.AuthLinkUrls
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthLinkUrlsTest {
    @Test fun resetRoundTripsExactly() {
        assertEquals(
            AuthLink.Reset(ResetPasswordLink("a/b+", "x+1@example.com")),
            AuthLink.parse(AuthLinkUrls.reset("a/b+", "x+1@example.com")),
        )
    }

    @Test fun utf8AndPercentRoundTripExactly() {
        assertEquals(
            AuthLink.Reset(ResetPasswordLink("é% /", "李+1@example.com")),
            AuthLink.parse(AuthLinkUrls.reset("é% /", "李+1@example.com")),
        )
    }

    @Test fun verifyRoundTripsExactly() {
        val input =
            SignedVerificationInput(
                "0199a3c0-0000-7000-8000-000000000001",
                "a".repeat(40),
                1_800_000_000,
                "b".repeat(64),
            )
        assertEquals(
            AuthLink.Verify(input),
            AuthLink.parse(AuthLinkUrls.verify(input.id, input.hash, input.expires, input.signature)),
        )
    }

    @Test fun usesRfc3986UppercaseEscapes() {
        assertEquals(
            "https://app.chronoflow.net/reset-password/AZaz09-._~%2F%2B%20%C3%A9?email=a%40b.c",
            AuthLinkUrls.reset("AZaz09-._~/+ é", "a@b.c"),
        )
    }
}
