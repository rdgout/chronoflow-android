package net.chronoflow.core.datamock

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.api.ApiPasskeyRegistrationInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class MockPasskeyManagementTests {
    /** Swift character limits count surrogate pairs as one grapheme (new). */
    @Test fun addAcceptsNamesWithinTheGraphemeLimit() =
        runTest(timeout = 60.seconds) {
            val source = mockSource(seed = passkeyState())
            login(source)
            val challenge = options(source)
            val name = "👍".repeat(128)
            val added =
                source.accountSecurity(
                    AccountSecurityChange.AddPasskey(
                        ApiPasskeyRegistrationInput(challenge.challengeId, name, attestation(challenge)),
                    ),
                ) as AccountSecurityResult.PasskeyAdded
            assertEquals(name, added.passkey.name)
            assertEquals(3, list(source).size)
        }

    /** An unknown 64-grapheme challenge reaches the expiry check (new). */
    @Test fun addChallengeLengthCountsGraphemes() =
        runTest(timeout = 60.seconds) {
            val source = mockSource(seed = passkeyState())
            login(source)
            val challenge = options(source)
            assertEquals(
                DataFailure.Validation(
                    mapOf("challengeId" to FieldMessage("The passkey registration request expired. Please try again.")),
                    422,
                ),
                failure {
                    source.accountSecurity(
                        AccountSecurityChange.AddPasskey(
                            ApiPasskeyRegistrationInput("👍".repeat(64), "Phone", attestation(challenge)),
                        ),
                    )
                },
            )
        }

    @Test fun listReturnsPersistedPasskeysAndAnAccountWithoutAnyIsEmpty() =
        runTest(timeout = 60.seconds) {
            val source = mockSource(seed = passkeyState())
            login(source)
            assertEquals(listOf("iPhone 16", "MacBook Pro"), list(source).map { it.name })
            val bare = mockSource()
            login(bare)
            assertTrue(list(bare).isEmpty())
        }

    @Test fun addRegistersOnceReturnsTheItemAndKeepsTheCountInTheSecuritySummary() =
        runTest(timeout = 60.seconds) {
            val source = mockSource(seed = passkeyState())
            login(source)
            val challenge = options(source)
            assertEquals("app.chronoflow.net", challenge.options.rp.id)
            assertTrue(
                challenge.options.user.name
                    .isNotEmpty(),
            )
            assertEquals(64, challenge.challengeId.length)
            assertTrue(challenge.options.excludeCredentials.isEmpty())
            val input = ApiPasskeyRegistrationInput(challenge.challengeId, " This phone ", attestation(challenge))
            val added =
                (
                    source.accountSecurity(
                        AccountSecurityChange.AddPasskey(input),
                    ) as AccountSecurityResult.PasskeyAdded
                ).passkey
            assertEquals("This phone", added.name)
            assertEquals(3L, added.id)
            assertNull(added.lastUsedAt)
            assertEquals(listOf("This phone", "iPhone 16", "MacBook Pro"), list(source).map { it.name })
            assertEquals(JsonPrimitive(3), source.fetchAccount().security["passkeyCount"])
            failure { source.accountSecurity(AccountSecurityChange.AddPasskey(input)) }
            assertEquals(3, list(source).size)
            assertEquals(listOf("AQID"), options(source).options.excludeCredentials.map { it.id })
        }

    @Test fun aWrongPasswordFailsAtOptionsAndIssuesNoChallenge() =
        runTest(timeout = 60.seconds) {
            val store = MemoryMockStateStore()
            val source = mockSource(store = store, seed = passkeyState())
            login(source)
            assertEquals(
                DataFailure.Validation(
                    mapOf("current_password" to FieldMessage("The provided password was incorrect.")),
                    422,
                ),
                failure { options(source, "wrong") },
            )
            assertTrue(store.load()!!.passkeyRegistrations.isNullOrEmpty())
        }

    @Test fun aFieldFailureKeepsTheChallengeButAFailedCredentialConsumesIt() =
        runTest(timeout = 60.seconds) {
            val source = mockSource(seed = passkeyState())
            login(source)
            val challenge = options(source)
            val input = ApiPasskeyRegistrationInput(challenge.challengeId, "", attestation(challenge))
            failure { source.accountSecurity(AccountSecurityChange.AddPasskey(input)) }
            val malformed =
                input.copy(
                    name = "Phone",
                    credential =
                        input.credential.copy(
                            response = input.credential.response.copy(attestationObject = "not base64url!"),
                        ),
                )
            failure { source.accountSecurity(AccountSecurityChange.AddPasskey(malformed)) }
            val error =
                failure {
                    source.accountSecurity(AccountSecurityChange.AddPasskey(input.copy(name = "Phone")))
                } as DataFailure.Validation
            assertEquals(422, error.status)
            assertNotNull(error.fields["challengeId"])
        }

    @Test fun removeNeedsThePasswordAndRevokesOnlyTheOtherSessions() =
        runTest(timeout = 60.seconds) {
            val store = MemoryMockStateStore()
            val source = mockSource(store = store, seed = passkeyState())
            login(source, device = "0199a3c0-0000-7000-8000-0000000000a1")
            val other = login(source, device = "0199a3c0-0000-7000-8000-0000000000b2")
            assertEquals(demoID, other.account.id)
            assertEquals(2, store.load()!!.sessions.size)
            assertEquals(
                DataFailure.Validation(
                    mapOf("current_password" to FieldMessage("The provided password was incorrect.")),
                    422,
                ),
                failure { source.accountSecurity(AccountSecurityChange.RemovePasskey(1, "wrong")) },
            )
            assertEquals(2, list(source).size)
            assertEquals(
                AccountSecurityResult.Changed,
                source.accountSecurity(AccountSecurityChange.RemovePasskey(1, fixturePassword)),
            )
            assertEquals(listOf(2L), list(source).map { it.id })
            assertEquals(JsonPrimitive(1), source.fetchAccount().security["passkeyCount"])
            assertEquals(1, store.load()!!.sessions.size)
            assertEquals(
                DataFailure.Server(404),
                failure {
                    source.accountSecurity(AccountSecurityChange.RemovePasskey(99, fixturePassword))
                },
            )
            assertEquals(
                DataFailure.Server(404),
                failure {
                    source.accountSecurity(AccountSecurityChange.RemovePasskey(99, "wrong"))
                },
            )
        }

    @Test fun passkeyCallsHonourOfflineInjectedFailuresAndTheCredentialLimiter() =
        runTest(timeout = 60.seconds) {
            val source = mockSource(seed = passkeyState())
            login(source)
            source.setOffline(true)
            assertEquals(DataFailure.Offline, failure { source.accountSecurity(AccountSecurityChange.ListPasskeys) })
            source.setOffline(false)
            source.failNext(MockMethod.AccountSecurity, DataFailure.Server(503))
            assertEquals(
                DataFailure.Server(503),
                failure { source.accountSecurity(AccountSecurityChange.ListPasskeys) },
            )
            repeat(6) { options(source) }
            assertTrue(failure { options(source) } is DataFailure.Throttled)
            assertEquals(2, list(source).size)
        }
}
