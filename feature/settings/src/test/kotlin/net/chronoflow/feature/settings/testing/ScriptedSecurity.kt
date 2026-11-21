package net.chronoflow.feature.settings.testing

import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult

class ScriptedSecurity {
    val changes = mutableListOf<AccountSecurityChange>()
    val responses = ArrayDeque<suspend () -> AccountSecurityResult>()

    suspend fun perform(change: AccountSecurityChange): AccountSecurityResult {
        changes += change
        return responses.removeFirst().invoke()
    }

    fun returns(result: AccountSecurityResult) {
        responses.addLast { result }
    }

    fun fails(failure: Throwable) {
        responses.addLast { throw failure }
    }
}
