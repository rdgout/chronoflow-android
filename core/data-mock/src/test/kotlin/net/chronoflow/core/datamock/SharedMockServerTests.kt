package net.chronoflow.core.datamock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.model.api.TwoFactorInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SharedMockServerTests {
    /** A concurrent removal must remain removed after a wrong proof (new). */
    @Test fun wrongTwoFactorProofDoesNotRestoreRemovedChallenge() =
        runTest(timeout = 60.seconds) {
            val (challenge, state) = wrongProofAfterSharedChallengeChange { null }
            assertNull(state.twoFactor[challenge])
        }

    /** An increment uses the store's current attempts rather than the request snapshot (new). */
    @Test fun wrongTwoFactorProofIncrementsCurrentChallenge() =
        runTest(timeout = 60.seconds) {
            val (challenge, state) = wrongProofAfterSharedChallengeChange { it.copy(attempts = 3) }
            assertEquals(4, state.twoFactor[challenge]?.attempts)
        }

    /** Removal uses the count read before another instance changes the challenge (new). */
    @Test fun wrongTwoFactorProofKeepsChallengeWhenEarlierReadWasBelowLimit() =
        runTest(timeout = 60.seconds) {
            val (challenge, state) = wrongProofAfterSharedChallengeChange { it.copy(attempts = 4) }
            assertEquals(5, state.twoFactor[challenge]?.attempts)
        }

    private suspend fun wrongProofAfterSharedChallengeChange(
        change: (MockTwoFactor) -> MockTwoFactor?,
    ): Pair<String, MockServerState> {
        val store = MemoryMockStateStore()
        val original = scenarioState()
        var interleave: (() -> Unit)? = null
        // Account lookup follows the challenge read, so this places a shared-store write between read and update.
        val accounts =
            object : Map<String, MockServerAccount> by original.accounts {
                override fun get(key: String): MockServerAccount? {
                    val account = original.accounts[key]
                    if (key == twoFactorID) {
                        val action = interleave
                        interleave = null
                        action?.invoke()
                    }
                    return account
                }
            }
        val source = mockSource(store = store, seed = original.copy(accounts = accounts))
        val challenge =
            (
                source.signIn(
                    passwordInput(email = MockScenarioValues.twoFactorEmail),
                ) as SignInResult.TwoFactor
            ).challengeId
        interleave = {
            store.update(checkNotNull(store.load())) { current ->
                val changed = change(checkNotNull(current.twoFactor[challenge]))
                val challenges = current.twoFactor - challenge
                current.copy(twoFactor = if (changed == null) challenges else challenges + (challenge to changed)) to
                    Unit
            }
        }
        assertEquals(
            DataFailure.Validation(
                mapOf("code" to FieldMessage("The authentication code was not accepted. Try again.")),
                422,
            ),
            failure { source.completeTwoFactor(TwoFactorInput(challenge, TwoFactorInput.Proof.Code("000000"))) },
        )
        assertNull(interleave)
        return challenge to checkNotNull(store.load())
    }

    @Test fun cachedSourcesShareMailAndSessionInvalidation() =
        runTest(timeout = 60.seconds) {
            val store = MemoryMockStateStore()
            val first = mockSource(store = store)
            val session = login(first)
            val second =
                MockDataSource(store = store, delay = {
                    0.0
                }, sleep = {}, bearer = { session.token }, zone = mockZone, io = Dispatchers.Unconfined)
            assertEquals(demoID, first.fetchIdentity().id)
            assertEquals(demoID, second.fetchIdentity().id)
            assertTrue(second.inbox().isEmpty())
            first.requestPasswordReset("demo@example.com")
            assertEquals(1, second.inbox().size)
            second.requestPasswordReset("demo@example.com")
            assertEquals(2, first.inbox().size)
            second.signOut()
            assertEquals(DataFailure.Unauthorized(401), failure { first.fetchIdentity() })
        }

    @Test fun concurrentMemoryMutationsRetainEveryMail() =
        runTest(timeout = 60.seconds) {
            val store = MemoryMockStateStore()
            val clock = MockClock()
            val first = mockSource(clock, store)
            first.inbox()
            val second = mockSource(clock, store)
            second.inbox()
            withContext(Dispatchers.Default) {
                coroutineScope {
                    launch { first.requestPasswordReset("demo@example.com") }
                    launch { second.requestPasswordReset("twofactor@example.com") }
                }
            }
            val mail = first.inbox()
            assertEquals(2, mail.size)
            assertEquals(mail, second.inbox())
        }
}
