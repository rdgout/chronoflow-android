package net.chronoflow.core.model.auth

import net.chronoflow.core.model.api.SignedVerificationInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AuthLinkTests {
    private val origin = "https://app.chronoflow.net"
    private val id = "0199a3c0-0000-7000-8000-000000000001"
    private val hash = "a".repeat(40)
    private val signature = "b".repeat(64)
    private val verify get() = "$origin/email/verify/$id/$hash"

    @Test fun exactSignedVerificationShapeAndMalformedInputs() {
        assertEquals(
            AuthLink.Verify(SignedVerificationInput(id, hash, 1_800_000_000, signature)),
            AuthLink.parse("$verify?signature=$signature&expires=1800000000"),
        )
        assertEquals(AuthLink.Verify(null), AuthLink.parse("$origin/email/verify"))
        assertEquals(AuthLink.Verify(null), AuthLink.parse("$origin/email/verify/bad/hash?expires=1&signature=x"))
        assertEquals(AuthLink.Verify(null), AuthLink.parse("$verify?expires=1&expires=2&signature=$signature"))
        assertEquals(AuthLink.Verify(null), AuthLink.parse("$verify?expires=-1&signature=$signature"))
    }

    @Test fun exactResetURLDecodesOnceAndKeepsPlusPercentAndUnicode() {
        assertEquals(
            AuthLink.Reset(ResetPasswordLink("a/b%2F", "rick+100%@example.com")),
            AuthLink.parse("$origin/reset-password/a%2Fb%252F?email=rick%2B100%25%40example.com"),
        )
        assertFalse(reset("/reset-password").isValid)
        assertFalse(reset("/reset-password/").isValid)
        assertFalse(reset("/reset-password/token").isValid)
        assertFalse(reset("/reset-password/token?email=a&email=b").isValid)
        assertFalse(reset("/reset-password/a/b?email=a").isValid)
        assertNull(AuthLink.parse("http://app.chronoflow.net/reset-password/a?email=b"))
        assertNull(AuthLink.parse("https://evil.test/reset-password/a?email=b"))
        assertNull(AuthLink.parse("https://app.chronoflow.net:443/reset-password/a?email=b"))
        assertNull(AuthLink.parse("https://user@app.chronoflow.net/reset-password/a?email=b"))
        assertNull(AuthLink.parse("$origin/reset-password/a?email=b#c"))
        assertNull(AuthLink.parse("chronoflow://reset/a"))
        assertNull(AuthLink.parse("$origin/other"))
        assertEquals(
            ResetPasswordLink("é", "李@example.com"),
            reset("/reset-password/%C3%A9?email=%E6%9D%8E%40example.com"),
        )
    }

    @Test fun plusInQueryStaysPlus() {
        assertEquals("a+b@example.com", reset("/reset-password/t?email=a+b@example.com").email)
    }

    @Test fun lenientUuidFormIsRejected() {
        assertEquals(
            AuthLink.Verify(null),
            AuthLink.parse("$origin/email/verify/1-1-1-1-1/$hash?expires=1&signature=$signature"),
        )
    }

    @Test fun upperCaseHexAndUuidAreAccepted() {
        val upperID = "0199A3C0-ABCD-7000-8000-000000000001"
        assertEquals(
            AuthLink.Verify(SignedVerificationInput(upperID, hash.uppercase(), 1, signature.uppercase())),
            AuthLink.parse(
                "$origin/email/verify/$upperID/${hash.uppercase()}?expires=1&signature=${signature.uppercase()}",
            ),
        )
    }

    @Test fun expiresOverflowIsMalformed() {
        assertEquals(AuthLink.Verify(null), AuthLink.parse("$verify?expires=99999999999999999999&signature=$signature"))
    }

    @Test fun trailingSlashVerifyIsMalformed() {
        assertEquals(AuthLink.Verify(null), AuthLink.parse("$verify/?expires=1&signature=$signature"))
    }

    @Test fun emptyFragmentIsRejected() {
        assertNull(AuthLink.parse("$origin/reset-password/t?email=a#"))
    }

    @Test fun emptyQueryMeansNoItems() {
        assertFalse(reset("/reset-password/t?").isValid)
    }

    @Test fun malformedPercentEncodingIsMissing() {
        assertEquals(ResetPasswordLink("t", ""), reset("/reset-password/t?email=%E0%A4"))
    }

    @Test fun unparseableUrlIsNotALink() {
        assertNull(AuthLink.parse("$origin/reset-password/a b"))
    }

    @Test fun redactedToString() {
        assertEquals(
            "ResetPasswordLink(token=<redacted>, email=<redacted>)",
            ResetPasswordLink("secret-token", "private@example.com").toString(),
        )
        val signed = SignedVerificationInput(id, "secret-hash", 1, "secret-signature").toString()
        assertFalse(signed.contains("secret-hash"))
        assertFalse(signed.contains("secret-signature"))
    }

    private fun reset(path: String): ResetPasswordLink = (AuthLink.parse(origin + path) as AuthLink.Reset).link
}
