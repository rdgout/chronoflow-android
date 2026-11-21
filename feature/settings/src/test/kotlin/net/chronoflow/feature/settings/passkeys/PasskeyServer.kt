package net.chronoflow.feature.settings.passkeys

import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.OutboxKind
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiPasskey
import net.chronoflow.core.model.api.ApiPasskeyCreationOptions
import net.chronoflow.core.model.api.ApiPasskeyRegistrationChallenge
import net.chronoflow.core.model.api.ApiPasskeyRegistrationInput

internal class PasskeyServer {
    var passkeys =
        listOf(
            ApiPasskey(1, "iPhone 16", null, "2026-09-14T08:00:00.000000Z", null),
            ApiPasskey(2, "MacBook Pro", null, "2026-10-02T08:00:00.000000Z", null),
        )
    val calls = mutableListOf<AccountSecurityChange>()
    var failure: DataFailure? = null
    var failsOnAdd = false
    var failsOnOptions = false
    var optionsChallenge = "Y2g"
    val optionsPassword get() =
        calls
            .filterIsInstance<AccountSecurityChange.PasskeyCreationOptions>()
            .firstOrNull()
            ?.current
    val added: ApiPasskeyRegistrationInput? get() =
        calls
            .filterIsInstance<AccountSecurityChange.AddPasskey>()
            .firstOrNull()
            ?.input

    suspend fun perform(change: AccountSecurityChange): AccountSecurityResult {
        calls += change
        failure?.let { failure ->
            val fails =
                when {
                    failsOnOptions -> change is AccountSecurityChange.PasskeyCreationOptions
                    failsOnAdd -> change is AccountSecurityChange.AddPasskey
                    else -> change != AccountSecurityChange.ListPasskeys
                }
            if (fails || (change == AccountSecurityChange.ListPasskeys && failure == DataFailure.Offline)) throw failure
        }
        return when (change) {
            AccountSecurityChange.ListPasskeys -> {
                AccountSecurityResult.Passkeys(passkeys)
            }

            is AccountSecurityChange.PasskeyCreationOptions -> {
                AccountSecurityResult.PasskeyCreation(challenge())
            }

            is AccountSecurityChange.AddPasskey -> {
                val item = ApiPasskey(3, change.input.name, null, "2026-11-12T09:30:00.000000Z", null)
                passkeys = listOf(item) + passkeys
                AccountSecurityResult.PasskeyAdded(item)
            }

            is AccountSecurityChange.RemovePasskey -> {
                passkeys = passkeys.filterNot { it.id == change.id }
                AccountSecurityResult.Changed
            }

            else -> {
                throw DataFailure.Unsupported(OutboxKind.Security)
            }
        }
    }

    fun challenge() =
        ApiPasskeyRegistrationChallenge(
            "c1",
            ApiJson.response.decodeFromString<ApiPasskeyCreationOptions>(
                """{"challenge":"$optionsChallenge","timeout":60000,"rp":{"id":"app.chronoflow.net","name":"ChronoFlow"},
        "user":{"id":"dXNlcg","name":"demo@example.com","displayName":"Demo"},
        "pubKeyCredParams":[{"type":"public-key","alg":-7}],
        "authenticatorSelection":{"requireResidentKey":true,"userVerification":"required","residentKey":"required"},
        "attestation":"none","excludeCredentials":[]}""",
            ),
        )
}
