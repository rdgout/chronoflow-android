package net.chronoflow.feature.auth.testing

import kotlinx.coroutines.CompletableDeferred
import net.chronoflow.core.datamock.MockMethod
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.ResetPasswordInput
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GatedDataSource(
    private val source: DataSource,
    private val method: MockMethod,
) : DataSource by source {
    val entered = CompletableDeferred<Unit>()
    var entries = 0
        private set
    private var continuation: Continuation<Unit>? = null

    private suspend fun waitForRelease() {
        entries++
        suspendCoroutine {
            continuation = it
            entered.complete(Unit)
        }
    }

    fun release() {
        continuation?.resume(Unit)
        continuation = null
    }

    override suspend fun requestPasswordReset(email: String) {
        if (method == MockMethod.RequestPasswordReset) waitForRelease()
        source.requestPasswordReset(email)
    }

    override suspend fun resetPassword(input: ResetPasswordInput) {
        if (method == MockMethod.ResetPassword) waitForRelease()
        source.resetPassword(input)
    }
}
