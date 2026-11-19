package net.chronoflow.core.data.store

import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.store.fetchAccount
import net.chronoflow.core.database.store.saveAccount
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.SessionAccount
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.model.store.StoreFailure

suspend fun AccountDatabase.upsertSessionAccount(account: SessionAccount) {
    val id = accountID ?: throw StoreFailure.InvalidAccountID
    val incoming =
        try {
            AccountID.of(account.id)
        } catch (
            _: AccountID.InvalidUUID,
        ) {
            throw StoreFailure.InvalidAccountID
        }
    if (id != incoming) throw StoreFailure.InvalidAccountID
    write { sql ->
        var record = sql.fetchAccount(id.rawValue) ?: AccountRecord(id.rawValue, account.name, account.email)
        if (StoreApplication.pending(sql).none { it.kind == "profile" }) {
            record = record.copy(name = account.name, email = account.email)
        }
        val seen = account.walkthroughSeen || StoreApplication.pending(sql).any { it.kind == "walkthrough" }
        sql.saveAccount(record.copy(hasVerifiedEmail = account.hasVerifiedEmail, walkthroughSeen = seen))
        val policies = ApiJson.response.encodeToString(AccountPolicies.serializer(), account.policies)
        StoreApplication.setting("policies", json(policies), sql)
    }
}
