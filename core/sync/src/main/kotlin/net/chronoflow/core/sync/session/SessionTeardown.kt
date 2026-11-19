package net.chronoflow.core.sync.session

import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.session.StoredSession
import kotlin.time.Duration

data class SessionTeardown(
    val sleep: suspend (Duration) -> Unit,
    val prepare: suspend () -> Unit = {},
    val pendingCount: suspend () -> Int = { 0 },
    val flush: suspend () -> Unit = {},
    val beginWipe: suspend (AccountID) -> Unit = {},
    val erase: suspend (AccountID) -> Unit = {},
    val finishWipe: suspend () -> Unit = {},
    val logout: suspend (StoredSession) -> Unit = {},
    val detach: suspend () -> Unit = {},
)
