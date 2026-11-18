package net.chronoflow.feature.auth.links

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.model.api.SignedVerificationInput
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.feature.auth.model.VerificationContext
import net.chronoflow.feature.auth.model.VerifyEmailModel
import net.chronoflow.feature.auth.model.VerifyState
import net.chronoflow.feature.auth.testing.AUTH_DEVICE_ID
import net.chronoflow.feature.auth.testing.authTestSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class VerificationLinkInboxTests {
    private val account = "0199a3c0-0000-7000-8000-00000000000a"
    private val other = "0199a3c0-0000-7000-8000-000000000002"

    private fun link(id: String) = SignedVerificationInput(id, "hash", 1_800_000_000, "signature")

    private fun context(
        id: String,
        epoch: Long,
    ) = VerificationContext(id, "demo@example.com", epoch)

    @Test fun guestLinkIsConsumedOnceByTheMatchingFirstSignIn() {
        val inbox = VerificationLinkInbox()
        inbox.sessionChanged(SessionPresence.SignedOut, 1)
        val input = link(account.uppercase())
        assertTrue(inbox.receive(input))
        assertNull(inbox.take(context(account, 1)))
        inbox.sessionChanged(SessionPresence.SignedIn(account), 2)
        assertNull(inbox.take(context(account, 1)))
        assertEquals(input, inbox.take(context(account, 2)))
        assertNull(inbox.take(context(account, 2)))
    }

    @Test fun foreignFirstSignInDropsTheLinkAndCannotApplyItLater() {
        val inbox = VerificationLinkInbox()
        inbox.sessionChanged(SessionPresence.SignedOut, 1)
        inbox.receive(link(account))
        inbox.sessionChanged(SessionPresence.SignedIn(other), 2)
        assertNull(inbox.pending)
        assertNull(inbox.take(context(other, 2)))
        inbox.sessionChanged(SessionPresence.SignedIn(account), 3)
        assertNull(inbox.take(context(account, 3)))
    }

    @Test fun verifiedSignInAndRepeatedOldLinksHaveNoVerificationDestination() {
        val inbox = VerificationLinkInbox()
        inbox.sessionChanged(SessionPresence.SignedOut, 1)
        inbox.receive(link(account))
        inbox.sessionChanged(SessionPresence.SignedIn(account), 2, true)
        assertNull(inbox.pending)
        assertFalse(inbox.receive(link(account)))
        assertNull(inbox.take(context(account, 2)))
        inbox.sessionChanged(SessionPresence.SignedOut, 3)
        inbox.sessionChanged(SessionPresence.SignedIn(other), 4)
        assertNull(inbox.take(context(other, 4)))
    }

    @Test fun signOutAndSessionReplacementClearPendingInputSignedOut() = replacement(SessionPresence.SignedOut)

    @Test fun signOutAndSessionReplacementClearPendingInputSameAccount() =
        replacement(
            SessionPresence.SignedIn(account),
        )

    @Test fun signOutAndSessionReplacementClearPendingInputOtherAccount() = replacement(SessionPresence.SignedIn(other))

    private fun replacement(next: SessionPresence) {
        val inbox = VerificationLinkInbox()
        inbox.sessionChanged(SessionPresence.SignedIn(account), 5)
        inbox.receive(link(account))
        inbox.sessionChanged(next, 6)
        assertNull(inbox.pending)
        val signedOut = VerificationLinkInbox()
        signedOut.sessionChanged(SessionPresence.SignedOut, 1)
        signedOut.receive(link(account))
        signedOut.sessionChanged(SessionPresence.SignedOut, 2)
        assertNull(signedOut.pending)
    }

    @Test fun coalescedUpdatesCannotCarryGuestInputAcrossAMissedSignOut() {
        val inbox = VerificationLinkInbox()
        inbox.sessionChanged(SessionPresence.SignedOut, 1)
        inbox.receive(link(account))
        inbox.sessionChanged(SessionPresence.SignedIn(account), 4)
        assertNull(inbox.pending)
        assertNull(inbox.take(context(account, 4)))
        inbox.sessionChanged(SessionPresence.SignedOut, 2)
        assertTrue(inbox.receive(link(account)))
        assertEquals(link(account), inbox.take(context(account, 4)))
    }

    @Test fun liveForeignAndMalformedInputsDoNotReplaceAMatchingPendingLink() {
        val inbox = VerificationLinkInbox()
        inbox.sessionChanged(SessionPresence.SignedIn(account), 1)
        assertTrue(inbox.receive(link(account)))
        assertFalse(inbox.receive(link(other)))
        assertFalse(inbox.receive(link("not-a-uuid")))
        assertFalse(inbox.receive(null))
        assertEquals(link(account), inbox.take(context(account, 1)))
    }

    @Test fun foreignGuestLinkLeavesTheNewAccountsFirstVerifyScreenWaiting() =
        runTest(timeout = 60.seconds) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val source = authTestSource(dispatcher, verified = false)
            val result =
                (
                    source.signIn(
                        PasswordSignInInput("demo@example.com", "Password123!", AUTH_DEVICE_ID, "test"),
                    ) as SignInResult.Session
                ).result
            val context = context(result.account.id, 2)
            val inbox = VerificationLinkInbox()
            inbox.sessionChanged(SessionPresence.SignedOut, 1)
            inbox.receive(link(other))
            inbox.sessionChanged(SessionPresence.SignedIn(context.accountId), 2)
            val model =
                VerifyEmailModel(context, source, {
                    1_800_000_000.0
                }, {
                    context
                }, {
                    error("foreign persisted")
                }, { error("foreign routed") }, {}, scope = backgroundScope, main = dispatcher, io = dispatcher)
            inbox.take(context)?.let(model::receiveLink)
            model.activate()
            assertEquals(VerifyState.Waiting, model.state)
            assertTrue(model.uiState.value.hasStatus)
            assertFalse(source.fetchIdentity().hasVerifiedEmail)
        }
}
