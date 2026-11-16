package net.chronoflow.core.model.api

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import net.chronoflow.core.model.contracts.ApiReviewIdentity
import org.junit.Test

class SecretRedactionTest {
    private val secret = "s3cr3t-value"

    private fun check(value: Any) {
        assertThat(value.toString()).doesNotContain(secret)
        assertThat(value.toString()).contains("***")
    }

    @Test
    fun signInInput() = check(PasswordSignInInput("a@b.c", secret, "dev", "Pixel", guestSecret = secret))

    @Test
    fun registerInput() = check(RegisterInput("N", "a@b.c", secret, secret, "dev", "Pixel", guestSecret = secret))

    @Test
    fun resetPasswordInput() = check(ResetPasswordInput(secret, "a@b.c", secret, secret))

    @Test
    fun signedVerificationInput() = check(SignedVerificationInput("1", secret, 1L, secret))

    @Test
    fun twoFactorInputWithCode() = check(TwoFactorInput(secret, TwoFactorInput.Proof.Code(secret)))

    @Test
    fun twoFactorInputWithRecoveryCode() = check(TwoFactorInput(secret, TwoFactorInput.Proof.RecoveryCode(secret)))

    @Test
    fun twoFactorSignInResult() = check(SignInResult.TwoFactor(secret))

    @Test
    fun sessionResult() {
        val account = SessionAccount("id", "N", "a@b.c", true, AccountPolicies("1", "1"), false)
        check(SessionResult(secret, account))
    }

    @Test
    fun twoFactorSetup() = check(ApiTwoFactorSetup(secret, secret))

    @Test
    fun recoveryCodes() = check(ApiRecoveryCodes(listOf(secret)))

    @Test
    fun reviewIdentity() = check(ApiReviewIdentity("token", 1, 1, null, secret))

    @Test
    fun passkeyChallenge() =
        check(
            ApiPasskeyChallenge(secret, ApiPasskeyRequestOptions("c", "rp", 1, UserVerification.Required, emptyList())),
        )

    @Test
    fun passkeyLoginInput() {
        val assertion =
            ApiPasskeyAssertion("id", "raw", CredentialType.PublicKey, ApiPasskeyAssertionResponse("c", "a", "s"))
        check(ApiPasskeyLoginInput(secret, assertion, "dev", "Pixel", guestSecret = secret))
    }

    @Test
    fun guestReviewInputRedactsItsSecret() = check(guestReviewInput().copy(guestSecret = secret))
}
