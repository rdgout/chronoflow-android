package net.chronoflow.feature.auth.model

import net.chronoflow.core.model.api.SessionResult

fun interface SessionAcceptor {
    suspend fun accept(result: SessionResult)
}
