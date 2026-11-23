package net.chronoflow.core.datamock

import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.ApiPasskeyChallenge
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.api.ApiTripSaveInput
import net.chronoflow.core.model.api.ResetPasswordInput
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.model.api.SignedVerificationInput
import net.chronoflow.core.model.api.TwoFactorInput

/** Inputs and prerequisites for per-method controls (new). */
internal data class MockMethodCall(
    val setup: suspend (MockDataSource) -> Unit,
    val call: suspend (MockDataSource) -> Unit,
)

internal val MOCK_METHOD_CALLS: Map<MockMethod, MockMethodCall> = methodCalls()

private fun methodCalls(): Map<MockMethod, MockMethodCall> {
    val signedIn: suspend (MockDataSource) -> Unit = { login(it) }
    lateinit var passkey: ApiPasskeyChallenge
    lateinit var twoFactor: String
    lateinit var reset: ResetPasswordInput
    lateinit var verification: SignedVerificationInput
    lateinit var save: ApiTripSaveInput
    lateinit var edit: ApiTripSaveInput
    return mapOf(
        MockMethod.AccountSecurity to
            MockMethodCall(signedIn) { it.accountSecurity(AccountSecurityChange.ListPasskeys) },
        MockMethod.DeleteAccount to MockMethodCall(signedIn) { it.deleteAccount(fixturePassword) },
        MockMethod.PasskeyOptions to MockMethodCall({}) { it.passkeyOptions() },
        MockMethod.PasskeyLogin to
            MockMethodCall({ passkey = it.passkeyOptions() }) { it.passkeyLogin(passkeyInput(passkey)) },
        MockMethod.SignIn to MockMethodCall({}) { it.signIn(passwordInput()) },
        MockMethod.CompleteTwoFactor to
            MockMethodCall({
                twoFactor =
                    (
                        it.signIn(
                            passwordInput(email = MockScenarioValues.twoFactorEmail),
                        ) as SignInResult.TwoFactor
                    ).challengeId
            }) { it.completeTwoFactor(TwoFactorInput(twoFactor, TwoFactorInput.Proof.Code("123456"))) },
        MockMethod.SignUp to MockMethodCall({}) { it.signUp(registration()) },
        MockMethod.RequestPasswordReset to MockMethodCall({}) { it.requestPasswordReset("demo@example.com") },
        MockMethod.ResetPassword to
            MockMethodCall({ source ->
                source.requestPasswordReset("demo@example.com")
                reset =
                    (source.inbox().last() as MockMail.PasswordReset).input.copy(
                        password = newPassword,
                        passwordConfirmation = newPassword,
                    )
            }) { it.resetPassword(reset) },
        MockMethod.VerificationStatus to MockMethodCall(signedIn) { it.verificationStatus() },
        MockMethod.ResendVerificationEmail to MockMethodCall(signedIn) { it.resendVerificationEmail() },
        MockMethod.VerifyEmail to
            MockMethodCall({ source ->
                source.signUp(registration())
                verification =
                    (source.inbox().last() as MockMail.Verification).input
            }) { it.verifyEmail(verification) },
        MockMethod.SignOut to MockMethodCall(signedIn) { it.signOut() },
        MockMethod.FetchIdentity to MockMethodCall(signedIn) { it.fetchIdentity() },
        MockMethod.FetchAccount to MockMethodCall(signedIn) { it.fetchAccount() },
        MockMethod.FetchTrips to MockMethodCall(signedIn) { it.fetchTrips() },
        MockMethod.FetchTripDetail to MockMethodCall(signedIn) { it.fetchTripDetail(demoTripID) },
        MockMethod.Send to MockMethodCall(signedIn) { it.send(OutboxChange.Walkthrough) },
        MockMethod.SearchAirports to MockMethodCall({}) { it.searchAirports("AMS", null) },
        MockMethod.ReviewTrip to
            MockMethodCall(signedIn) { it.reviewTrip(ApiTripReviewInput(wizardInput(), profile = wizardProfile())) },
        MockMethod.ReviewGuestTrip to MockMethodCall({}) { it.reviewGuestTrip(guestReviewInput()) },
        MockMethod.ReviewTripEdit to
            MockMethodCall(signedIn) {
                it.reviewTripEdit(
                    ApiTripReviewInput(wizardInput(), profile = wizardProfile()),
                    demoTripID,
                    "a".repeat(64),
                )
            },
        MockMethod.SaveTrip to
            MockMethodCall({ source ->
                login(source)
                save =
                    ApiTripSaveInput.from(
                        source.reviewTrip(ApiTripReviewInput(wizardInput(), profile = wizardProfile())).identity,
                    )
            }) { it.saveTrip(save) },
        MockMethod.UpdateTrip to
            MockMethodCall({ source ->
                login(source)
                edit =
                    ApiTripSaveInput.from(
                        source
                            .reviewTripEdit(
                                ApiTripReviewInput(wizardInput(), profile = wizardProfile()),
                                demoTripID,
                                "a".repeat(64),
                            ).identity,
                    )
            }) { it.updateTrip(demoTripID, edit, "a".repeat(64)) },
        MockMethod.TripInput to MockMethodCall(signedIn) { it.tripInput(demoTripID) },
    )
}
