package net.chronoflow.core.sync.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.session.SessionMutationPermit
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.SessionPresence.SignedIn
import net.chronoflow.core.model.session.SessionPresence.SignedOut
import net.chronoflow.core.model.session.SessionSnapshot
import net.chronoflow.core.model.session.SessionStore
import net.chronoflow.core.model.session.SessionUpdate
import net.chronoflow.core.model.session.SignOutResult
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.model.session.readStoredSession
import kotlin.time.Duration.Companion.seconds

class SessionController(
    private val credentials: SessionStore,
    private val accounts: SessionAccountStore,
    private val teardown: SessionTeardown,
    io: CoroutineDispatcher,
) {
    private val confined = io.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + confined)
    private var tail: Job? = null
    private var transitioning = false
    private var permit: SessionMutationPermit? = null
    private var stopSync: suspend () -> Unit = {}
    private var requestSync: suspend (Long) -> Unit = {}

    @Volatile var presence: SessionPresence? = null
        private set

    @Volatile var epoch: Long = 0
        private set

    @Volatile internal var storedSession: StoredSession? = null
        private set

    @Volatile internal var generation: Long = 0
        private set

    @Volatile var pendingUnauthorizedEpoch: Long? = null
        private set

    @Volatile internal var signOutFlight: Deferred<SignOutResult>? = null
        private set

    private data class UnconditionalSignOut(
        val discardPending: Boolean,
        val unauthorized: Boolean,
    )

    private var unconditionalSignOut: UnconditionalSignOut? = null
    private var discardRequested = false
    private var unauthorizedRequested = false
    private val mutableUpdates = MutableStateFlow<SessionUpdate?>(null)
    val updates = mutableUpdates.asStateFlow()

    fun attach(
        stopSync: suspend () -> Unit = {},
        requestSync: suspend (Long) -> Unit = {},
    ) {
        check(generation == 0L)
        this.stopSync = stopSync
        this.requestSync = requestSync
    }

    suspend fun activeSession(): SessionSnapshot? = withContext(confined) { snapshot() }

    private fun snapshot(): SessionSnapshot? {
        val stored = storedSession ?: return null
        return if (!transitioning && presence == SignedIn(stored.accountId)) SessionSnapshot(stored, epoch) else null
    }

    suspend fun mutationPermit(snapshot: SessionSnapshot): SessionMutationPermit? =
        withContext(confined) { if (snapshot() == snapshot) permit else null }

    suspend fun restore(): SessionPresence =
        withContext(confined) {
            generation += 1
            val gen = generation
            awaitCancellable(enqueue { performRestore(gen) })
        }

    suspend fun accept(result: SessionResult): Unit =
        withContext(confined) {
            generation += 1
            val gen = generation
            awaitCancellable(enqueue { performAccept(result, gen) })
        }

    suspend fun confirmEmailVerification(
        expected: SessionSnapshot,
        email: String,
    ): Boolean =
        withContext(confined) {
            val gen = generation
            awaitCancellable(
                enqueue {
                    currentCoroutineContext().ensureActive()
                    if (generation != gen || snapshot() != expected) return@enqueue false
                    val updated = accounts.markEmailVerified(AccountID.of(expected.session.accountId), email)
                    currentCoroutineContext().ensureActive()
                    if (!updated || generation != gen || snapshot() != expected) return@enqueue false
                    requestSync(expected.epoch)
                    generation == gen && snapshot() == expected
                },
            )
        }

    suspend fun signOut(
        discardPending: Boolean,
        unauthorized: Boolean = false,
    ): SignOutResult = withContext(confined) { enqueueSignOut(discardPending, unauthorized, null) }

    suspend fun signOut(
        expected: SessionSnapshot,
        discardPending: Boolean,
    ): SignOutResult =
        withContext(confined) {
            if (snapshot() !=
                expected
            ) {
                SignOutResult.SignedOut
            } else {
                enqueueSignOut(discardPending, false, expected.epoch)
            }
        }

    suspend fun unauthorized(epoch: Long): SignOutResult =
        withContext(confined) {
            if (this@SessionController.epoch != epoch) return@withContext SignOutResult.SignedOut
            pendingUnauthorizedEpoch = epoch
            val result = enqueueSignOut(true, true, epoch)
            if (pendingUnauthorizedEpoch == epoch) pendingUnauthorizedEpoch = null
            result
        }

    suspend fun retryUnauthorized() =
        withContext(confined) {
            val pending = pendingUnauthorizedEpoch ?: return@withContext
            if (epoch != pending) {
                pendingUnauthorizedEpoch = null
                return@withContext
            }
            unauthorized(pending)
            Unit
        }

    private suspend fun enqueueSignOut(
        discardPending: Boolean,
        unauthorized: Boolean,
        expectedEpoch: Long?,
    ): SignOutResult {
        if (expectedEpoch != null && epoch != expectedEpoch) return SignOutResult.SignedOut
        discardRequested = discardRequested || discardPending
        unauthorizedRequested = unauthorizedRequested || unauthorized
        if (expectedEpoch == null) {
            generation += 1
            unconditionalSignOut =
                UnconditionalSignOut(
                    (unconditionalSignOut?.discardPending ?: false) || discardPending,
                    (unconditionalSignOut?.unauthorized ?: false) || unauthorized,
                )
        }
        signOutFlight?.let { return it.await() }
        val flight = enqueue { withContext(NonCancellable) { performSignOut(expectedEpoch) } }
        signOutFlight = flight
        return flight.await()
    }

    private suspend fun performSignOut(expectedEpoch: Long?): SignOutResult {
        try {
            if (expectedEpoch != null && epoch != expectedEpoch) {
                val explicit = unconditionalSignOut ?: return SignOutResult.SignedOut
                discardRequested = explicit.discardPending
                unauthorizedRequested = explicit.unauthorized
            }
            var captured = storedSession
            if (captured == null) {
                stopSync()
                accounts.close()
                teardown.prepare()
                captured = credentials.readStoredSession()
            }
            val session = captured
            if (session == null) {
                credentials.delete()
                publish(SignedOut)
                teardown.detach()
                return SignOutResult.SignedOut
            }
            if (!unauthorizedRequested && !discardRequested) {
                var count = teardown.pendingCount()
                if (count > 0) {
                    try {
                        teardown.flush()
                    } catch (
                        cancelled: CancellationException,
                    ) {
                        throw cancelled
                    } catch (_: Exception) {
                    }
                    count = teardown.pendingCount()
                }
                if (count > 0 && !discardRequested &&
                    !unauthorizedRequested
                ) {
                    return SignOutResult.NeedsConfirmation(count)
                }
            }
            val id = AccountID.of(session.accountId)
            teardown.beginWipe(id)
            storedSession = null
            publish(SignedOut)
            stopSync()
            if (!unauthorizedRequested) bestEffortLogout(session)
            teardown.detach()
            accounts.close()
            var failure: Exception? = null
            try {
                teardown.erase(id)
            } catch (
                cancelled: CancellationException,
            ) {
                throw cancelled
            } catch (error: Exception) {
                failure =
                    error
            }
            try {
                credentials.delete()
            } catch (
                cancelled: CancellationException,
            ) {
                throw cancelled
            } catch (error: Exception) {
                if (failure ==
                    null
                ) {
                    failure = error
                }
            }
            failure?.let { throw it }
            teardown.finishWipe()
            return SignOutResult.SignedOut
        } finally {
            signOutFlight = null
            unconditionalSignOut = null
            discardRequested = false
            unauthorizedRequested = false
        }
    }

    private suspend fun bestEffortLogout(session: StoredSession) =
        coroutineScope {
            val request =
                launch {
                    try {
                        teardown.logout(session)
                    } catch (
                        cancelled: CancellationException,
                    ) {
                        throw cancelled
                    } catch (_: Exception) {
                    }
                }
            val deadline =
                launch {
                    try {
                        teardown.sleep(3.seconds)
                    } catch (
                        cancelled: CancellationException,
                    ) {
                        throw cancelled
                    } catch (_: Exception) {
                    }
                }
            try {
                select {
                    request.onJoin { }
                    deadline.onJoin { }
                }
            } finally {
                request.cancelAndJoin()
                deadline.cancelAndJoin()
            }
        }

    private fun <T> enqueue(work: suspend () -> T): Deferred<T> {
        val previous = tail
        val job =
            scope.async {
                previous?.join()
                currentCoroutineContext().ensureActive()
                work()
            }
        // A cancelled operation can finish before its predecessor; the queue must still wait for both.
        tail =
            scope.launch {
                withContext(NonCancellable) {
                    previous?.join()
                    job.join()
                }
            }
        return job
    }

    private suspend fun <T> awaitCancellable(job: Deferred<T>): T =
        try {
            job.await()
        } catch (failure: CancellationException) {
            job.cancel()
            throw failure
        }

    private suspend fun checkGeneration(gen: Long) {
        currentCoroutineContext().ensureActive()
        if (generation != gen) throw SessionFailure.Superseded
    }

    private suspend fun performRestore(gen: Long): SessionPresence {
        checkGeneration(gen)
        transitioning = true
        try {
            stopSync()
            teardown.prepare()
            checkGeneration(gen)
            val stored = mapped(SessionFailure.CredentialsUnavailable) { credentials.readStoredSession() }
            checkGeneration(gen)
            val id =
                stored?.let {
                    try {
                        AccountID.of(it.accountId)
                    } catch (_: AccountID.InvalidUUID) {
                        null
                    }
                }
            if (stored == null || id == null) {
                if (stored != null) {
                    mapped(SessionFailure.CredentialsUnavailable) { credentials.delete() }
                    checkGeneration(gen)
                }
                mapped(SessionFailure.DatabaseUnavailable) { accounts.close() }
                checkGeneration(gen)
                storedSession = null
                publish(SignedOut)
                return SignedOut
            }
            mapped(SessionFailure.DatabaseUnavailable) { accounts.open(id) }
            checkGeneration(gen)
            storedSession = StoredSession(stored.token, id.rawValue)
            publish(SignedIn(id.rawValue))
            transitioning = false
            requestSync(epoch)
            checkGeneration(gen)
            return SignedIn(id.rawValue)
        } finally {
            transitioning = false
        }
    }

    private suspend fun performAccept(
        result: SessionResult,
        gen: Long,
    ) {
        checkGeneration(gen)
        val id =
            try {
                AccountID.of(result.account.id)
            } catch (
                _: AccountID.InvalidUUID,
            ) {
                throw SessionFailure.InvalidResult
            }
        if (result.token.isBlank()) throw SessionFailure.InvalidResult
        transitioning = true
        try {
            stopSync()
            teardown.prepare()
            checkGeneration(gen)
            val current = presence
            if (current is SignedIn && current.accountId != id.rawValue) {
                storedSession = null
                publish(SignedOut)
            }
            try {
                accounts.open(id)
                checkGeneration(gen)
                accounts.upsert(result.account)
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: SessionFailure) {
                throw failure
            } catch (_: Exception) {
                throw SessionFailure.DatabaseUnavailable
            }
            checkGeneration(gen)
            val session = StoredSession(result.token, id.rawValue)
            // The write commits the session even if a newer request arrives while storage is suspended.
            withContext(NonCancellable) {
                mapped(SessionFailure.CredentialWrite) { credentials.write(session.encode()) }
                storedSession = session
                publish(SignedIn(id.rawValue))
                transitioning = false
                requestSync(epoch)
            }
        } finally {
            transitioning = false
        }
    }

    private suspend fun <T> mapped(
        failure: SessionFailure,
        work: suspend () -> T,
    ): T =
        try {
            work()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            throw failure
        }

    private fun publish(presence: SessionPresence) {
        permit?.invalidate()
        permit = if (presence is SignedIn) SessionMutationPermit() else null
        epoch += 1
        this.presence = presence
        mutableUpdates.value = SessionUpdate(presence, epoch)
    }

    fun close() {
        scope.cancel()
    }
}
