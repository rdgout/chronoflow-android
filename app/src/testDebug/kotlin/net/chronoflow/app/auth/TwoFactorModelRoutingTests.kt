package net.chronoflow.app.auth

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.SessionAccount
import net.chronoflow.core.model.session.readStoredSession
import net.chronoflow.core.sync.session.MemorySessionStore
import net.chronoflow.core.sync.session.SessionAccountStore
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionTeardown
import net.chronoflow.feature.auth.model.SecretField
import net.chronoflow.feature.auth.model.SessionAcceptor
import net.chronoflow.feature.auth.model.SignInModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TwoFactorModelRoutingTests {
    @Test fun onlySessionAcceptancePersistsATokenAndNeverTheChallengeOrProof() =
        runTest(timeout = 60.seconds) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val credentials = MemorySessionStore()
            val accounts = CapturingTwoFactorAccounts()
            val controller = SessionController(credentials, accounts, SessionTeardown(sleep = {}), dispatcher)
            try {
                val signIn =
                    SignInModel(
                        twoFactorTestSource(dispatcher),
                        SessionAcceptor(controller::accept),
                        { "0199a3c0-0000-7000-8000-000000000001" },
                        { "test" },
                        accepted = { _, _ -> },
                        onTwoFactor = {},
                    )
                signIn.editEmail("twofactor@example.com")
                signIn.submitPassword(TwoFactorTestSecret()) {}
                val challenge = checkNotNull(signIn.navigation.challengeID)
                val model = checkNotNull(signIn.twoFactor.value)
                model.editCode("000000")
                model.submit()
                assertNull(credentials.data)
                assertEquals("", accounts.records)
                model.toggleMode()
                model.editCode(RECOVERY_PROOF)
                assertNull(credentials.data)
                model.submit()
                val stored = checkNotNull(credentials.data).decodeToString()
                assertEquals(
                    setOf("token", "accountId"),
                    ApiJson.response
                        .parseToJsonElement(stored)
                        .jsonObject.keys,
                )
                assertNotNull(credentials.readStoredSession())
                assertFalse(credentials.readStoredSession()!!.token.isEmpty())
                val records = stored + accounts.records
                assertFalse(records.contains(challenge))
                assertFalse(records.contains(RECOVERY_PROOF))
                assertEquals("", model.code)
                assertNull(signIn.navigation.challengeID)
            } finally {
                withContext(NonCancellable) { controller.close() }
            }
        }
}

private class CapturingTwoFactorAccounts : SessionAccountStore {
    var records = ""

    override suspend fun open(id: AccountID) {}

    override suspend fun upsert(account: SessionAccount) {
        records = ApiJson.request.encodeToString(SessionAccount.serializer(), account)
    }

    override suspend fun close() {}
}

internal class TwoFactorTestSecret : SecretField {
    private var value = "password"

    override fun read(): String = value

    override fun clear() {
        value = ""
    }

    override fun toString(): String = "SecretField(***)"
}
