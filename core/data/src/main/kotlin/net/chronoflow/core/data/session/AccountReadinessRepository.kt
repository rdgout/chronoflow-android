package net.chronoflow.core.data.session

import kotlinx.coroutines.flow.Flow
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.store.fetchAccount
import net.chronoflow.core.database.store.fetchPendingOutbox
import net.chronoflow.core.database.store.fetchSetting
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.session.StoredAccountReadiness
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.sync.session.AccountReadinessSource

class AccountReadinessRepository(
    private val database: AccountDatabase,
) : AccountReadinessSource {
    override suspend fun values(accountId: String): Flow<StoredAccountReadiness?> {
        val id = AccountID.of(accountId)
        if (database.accountID != id) throw StoreFailure.InvalidAccountID
        return database.observe("account", "settings", "outbox") { sql ->
            val account = sql.fetchAccount(id.rawValue) ?: return@observe null
            val seen = account.walkthroughSeen || sql.fetchPendingOutbox().any { it.kind == "walkthrough" }
            val policies =
                sql.fetchSetting("policies")?.let {
                    ApiJson.response.decodeFromString(AccountPolicies.serializer(), it.value)
                }
            val gate =
                when {
                    !account.hasVerifiedEmail -> Gate.Verify
                    policies == null -> null
                    policies.acceptedTermsVersion != policies.termsVersion -> Gate.Terms
                    !seen -> Gate.Walkthrough
                    else -> Gate.App
                }
            StoredAccountReadiness(
                id.rawValue,
                gate,
                account.syncedAt != null,
                if (seen) Gate.App else Gate.Walkthrough,
            )
        }
    }
}
