package net.chronoflow.feature.auth.verify

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.SignedVerificationInput
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.SessionSnapshot
import net.chronoflow.core.model.session.SessionUpdate
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import net.chronoflow.feature.auth.links.VerificationLinkInbox
import net.chronoflow.feature.auth.links.VerifyLinkReceiver
import net.chronoflow.feature.auth.model.VerificationContext
import net.chronoflow.feature.auth.model.VerifyEmailModel

data class StoredVerificationAccount(
    val id: String,
    val email: String,
    val hasVerifiedEmail: Boolean,
)

interface VerificationSessionPort {
    suspend fun epoch(): Long

    suspend fun presence(): SessionPresence?

    suspend fun activeSession(): SessionSnapshot?

    suspend fun storedAccount(): StoredVerificationAccount?

    suspend fun source(snapshot: SessionSnapshot): DataSource

    suspend fun confirm(
        expected: SessionSnapshot,
        email: String,
    ): Boolean

    suspend fun signOutDiscarding(expected: SessionSnapshot)

    fun updates(): Flow<SessionUpdate>
}

fun interface VerificationMail {
    suspend fun latest(context: VerificationContext): SignedVerificationInput?
}

class VerificationCoordinator(
    private val port: VerificationSessionPort,
    private val inbox: VerificationLinkInbox,
    private val navigator: RootNavigator,
    private val routeAccepted: suspend (navigate: (Route) -> Unit, verification: suspend (Long) -> Unit) -> Unit,
    private val clearSignInSecrets: () -> Unit,
    private val verificationMail: VerificationMail?,
    private val nowSeconds: () -> Double,
    private val scope: CoroutineScope,
    private val main: CoroutineDispatcher,
    private val io: CoroutineDispatcher,
) : VerifyLinkReceiver {
    private val mutableModel = MutableStateFlow<VerifyEmailModel?>(null)
    val model = mutableModel.asStateFlow()

    init {
        scope.launch(main, start = CoroutineStart.UNDISPATCHED) {
            port.updates().collect { synchronizeVerificationSession() }
        }
    }

    suspend fun verificationContext(requireUnverified: Boolean = false): VerificationContext? =
        withContext(main) {
            val snapshot = port.activeSession() ?: return@withContext null
            val account = port.storedAccount() ?: return@withContext null
            if (account.id != snapshot.session.accountId || (requireUnverified && account.hasVerifiedEmail) ||
                port.activeSession() != snapshot
            ) {
                return@withContext null
            }
            VerificationContext(account.id, account.email, snapshot.epoch)
        }

    suspend fun synchronizeVerificationSession(): Boolean =
        withContext(main) {
            val epoch = port.epoch()
            val presence = port.presence() ?: return@withContext false
            var verified = false
            if (presence is SessionPresence.SignedIn) {
                val snapshot = port.activeSession() ?: return@withContext false
                if (snapshot.epoch != epoch) return@withContext false
                val account = port.storedAccount() ?: return@withContext false
                if (account.id != presence.accountId) return@withContext false
                verified = account.hasVerifiedEmail
            }
            if (port.epoch() != epoch || port.presence() != presence) return@withContext false
            inbox.sessionChanged(presence, epoch, verified)
            model.value?.let {
                if (it.context.epoch != epoch) {
                    it.cancel()
                    mutableModel.value = null
                }
            }
            true
        }

    suspend fun prepareVerification(): Unit =
        withContext(main) {
            if (!synchronizeVerificationSession()) return@withContext
            val context = verificationContext(requireUnverified = true) ?: return@withContext
            val existing = model.value
            if (existing?.context != context || !existing.isActive) {
                val snapshot = port.activeSession() ?: return@withContext
                if (snapshot.epoch != context.epoch || snapshot.session.accountId != context.accountId) {
                    return@withContext
                }
                val source = port.source(snapshot)
                if (port.activeSession() != snapshot || verificationContext(true) != context) {
                    return@withContext
                }
                if (model.value?.context != context || model.value?.isActive != true) {
                    model.value?.cancel()
                    lateinit var created: VerifyEmailModel
                    created =
                        VerifyEmailModel(
                            context,
                            source,
                            nowSeconds,
                            current = { verificationContext() },
                            persist = { ctx ->
                                val expected = port.activeSession()
                                if (expected == null || expected.epoch != ctx.epoch ||
                                    expected.session.accountId != ctx.accountId
                                ) {
                                    false
                                } else {
                                    port.confirm(expected, ctx.email)
                                }
                            },
                            route = {
                                routeAccepted(
                                    { route ->
                                        if (navigator.state.value.route == Route.Verify) navigator.navigate(route)
                                    },
                                    ::onVerify,
                                )
                            },
                            discard = discard@{
                                val expected = port.activeSession() ?: return@discard
                                if (expected.epoch != context.epoch ||
                                    expected.session.accountId != context.accountId ||
                                    verificationContext() != context
                                ) {
                                    return@discard
                                }
                                // Publication cancels the model's scope before the account wipe has finished.
                                withContext(NonCancellable) {
                                    port.signOutDiscarding(expected)
                                    if (port.presence() == SessionPresence.SignedOut) {
                                        if (model.value === created) {
                                            created.cancel()
                                            mutableModel.value = null
                                        }
                                        if (navigator.state.value.route == Route.Verify) {
                                            navigator.navigate(Route.SignIn)
                                        }
                                    }
                                }
                            },
                            mail = verificationMail?.let { mail -> { mail.latest(context) } },
                            scope = scope,
                            main = main,
                            io = io,
                        )
                    mutableModel.value = created
                }
            }
            inbox.take(context)?.let { model.value?.receiveLink(it) }
        }

    override suspend fun receiveVerificationLink(input: SignedVerificationInput?): Unit =
        withContext(main) {
            if (!synchronizeVerificationSession() || !inbox.receive(input)) return@withContext
            if (port.presence() == SessionPresence.SignedOut) {
                clearSignInSecrets()
                navigator.navigate(Route.SignIn)
                return@withContext
            }
            prepareVerification()
            val model = model.value ?: return@withContext
            if (!model.isActive || verificationContext(true) != model.context) return@withContext
            navigator.navigate(Route.Verify)
            model.activate()
        }

    suspend fun onVerify(epoch: Long): Unit =
        withContext(main) {
            if (port.epoch() != epoch) return@withContext
            val account = port.storedAccount() ?: return@withContext
            if (account.hasVerifiedEmail || port.epoch() != epoch) return@withContext
            prepareVerification()
            if (port.epoch() != epoch || verificationContext(true)?.epoch != epoch || model.value?.isActive != true) {
                return@withContext
            }
            if (navigator.state.value.route != Route.Verify) navigator.navigate(Route.Verify)
        }
}
