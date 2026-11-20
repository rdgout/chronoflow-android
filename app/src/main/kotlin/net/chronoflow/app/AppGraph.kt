package net.chronoflow.app

import android.app.Application
import android.os.Process
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.chronoflow.app.auth.AuthServices
import net.chronoflow.app.auth.LazyPublicDataSource
import net.chronoflow.app.auth.VerificationSessionAdapter
import net.chronoflow.app.auth.mockResetMail
import net.chronoflow.app.links.AuthLinkDispatcher
import net.chronoflow.app.links.AuthLinkRouting
import net.chronoflow.app.wizard.SessionTripWizardDependencies
import net.chronoflow.core.data.clock.SystemPlanClock
import net.chronoflow.core.data.passkey.CredentialManagerClient
import net.chronoflow.core.data.passkey.PasskeyAnchor
import net.chronoflow.core.data.repository.AccountModel
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.session.AccountReadinessRepository
import net.chronoflow.core.data.session.AccountWiper
import net.chronoflow.core.data.session.NoopSessionHooks
import net.chronoflow.core.data.session.SessionAccountDatabase
import net.chronoflow.core.data.session.SessionHooks
import net.chronoflow.core.data.sync.ConnectivitySyncPathMonitor
import net.chronoflow.core.data.sync.system
import net.chronoflow.core.database.AndroidAccountDriverFactory
import net.chronoflow.core.datastore.AndroidKeystoreSessionCipher
import net.chronoflow.core.datastore.DeviceIdentityStore
import net.chronoflow.core.datastore.DevicePreferences
import net.chronoflow.core.datastore.KeystoreSessionStore
import net.chronoflow.core.datastore.androidDeviceName
import net.chronoflow.core.model.passkey.CredentialClient
import net.chronoflow.core.model.passkey.PasskeyCapability
import net.chronoflow.core.model.passkey.PasskeyFailure
import net.chronoflow.core.model.session.SessionStore
import net.chronoflow.core.model.session.readStoredSession
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.navigation.LaunchIntentHandler
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.toRoute
import net.chronoflow.core.network.DataSourceFactory
import net.chronoflow.core.plan.PlanClock
import net.chronoflow.core.sync.SessionSync
import net.chronoflow.core.sync.SyncScheduler
import net.chronoflow.core.sync.session.AccountReadinessSource
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionGateRouter
import net.chronoflow.core.sync.session.SessionTeardown
import net.chronoflow.core.sync.session.StoredDataReadiness
import net.chronoflow.feature.auth.links.GuestResetLinkRouter
import net.chronoflow.feature.auth.links.VerificationLinkInbox
import net.chronoflow.feature.auth.model.SessionAcceptor
import net.chronoflow.feature.auth.model.passkey.DeterministicAssertionProvider
import net.chronoflow.feature.auth.model.passkey.SystemPasskeyAssertionProvider
import net.chronoflow.feature.auth.verify.VerificationCoordinator
import net.chronoflow.feature.launch.LaunchController
import net.chronoflow.feature.launch.LaunchTrace
import java.io.File

class AppGraph(
    private val app: Application,
    val fontPreloader: FontPreloader = ResourceFontPreloader,
    private val launchProvider: (() -> LaunchController)? = null,
    val overrides: GraphOverrides = GraphOverrides.None,
    private val suppliedDispatchers: AppDispatchers = AppDispatchers(),
) {
    private val wizardOverrides by lazy {
        net.chronoflow.app.wizard
            .WizardStartOverrides()
    }
    var wizardStartOverride: net.chronoflow.app.wizard.WizardStartOverride?
        get() = wizardOverrides.wizardStartOverride
        internal set(value) {
            wizardOverrides.wizardStartOverride = value
        }
    internal val wizardRecoveryCheck by lazy {
        net.chronoflow.app.wizard.WizardRecoveryCheck(
            dispatchers,
            {
                val deps = sessionGraphs.current.value?.wizard as? SessionTripWizardDependencies
                deps?.store?.recover() != null
            },
            { LaunchPlatform.wizardBoardCapture },
        )
    }
    val dispatchers by lazy { overrides.dispatchers ?: suppliedDispatchers }
    val appScope by lazy { CoroutineScope(SupervisorJob() + dispatchers.main) }
    internal val launchScope get() = appScope
    internal val processToken = "${Process.myPid()}:${Process.getStartElapsedRealtime()}"
    val launch: LaunchController get() = launchProvider?.invoke() ?: LaunchPlatform.controller(this)
    val copyCatalogs: net.chronoflow.core.plan.CopyCatalogProvider by lazy {
        net.chronoflow.core.plan
            .CopyCatalogProvider(dispatchers.io)
    }
    val planClock: PlanClock by lazy { SystemPlanClock(app) }
    private val rootMutex by lazy { Mutex() }
    private var resolvedRoot: File? = null
    val storageRoot: suspend () -> File by lazy {
        {
            withContext(dispatchers.io) {
                rootMutex.withLock {
                    resolvedRoot
                        ?: (overrides.storageDir ?: app.noBackupFilesDir).also { resolvedRoot = it }
                }
            }
        }
    }
    val selection by lazy {
        DataSourceFactory(
            BuildConfig.DATA_SOURCE,
            BuildConfig.API_BASE_URL,
            release = !BuildConfig.DEBUG,
            testing = false,
        )
    }
    val namespace by lazy { selection.namespace }
    private val credentialMutex by lazy { Mutex() }
    private var credentials: KeystoreSessionStore? = null

    private suspend fun credentials(): KeystoreSessionStore =
        withContext(dispatchers.io) {
            credentialMutex.withLock {
                credentials ?: KeystoreSessionStore(
                    File(storageRoot(), "${namespace.directoryName}/session.bin"),
                    overrides.sessionAlias ?: namespace.sessionKeyAlias,
                    overrides.sessionCipher ?: AndroidKeystoreSessionCipher(),
                    dispatchers.io,
                ).also { credentials = it }
            }
        }

    val sessionStore: SessionStore by lazy {
        object : SessionStore {
            override suspend fun read(): ByteArray? = credentials().read()

            override suspend fun write(data: ByteArray) = credentials().write(data)

            override suspend fun delete() = credentials().delete()
        }
    }
    val devicePreferences by lazy { DevicePreferences.shared(app) }
    val deviceIdentity by lazy { DeviceIdentityStore(devicePreferences) }
    val wiper by lazy { AccountWiper(storageRoot, namespace, sessionStore, io = dispatchers.io) }
    private val accountsLazy: Lazy<SessionAccountDatabase> =
        lazy {
            SessionAccountDatabase(
                storageRoot,
                namespace,
                AndroidAccountDriverFactory(app),
                dispatchers.io,
                detach = { sessionGraphs.drop() },
            )
        }
    val sessionAccounts: SessionAccountDatabase by accountsLazy
    val mockSources: MockSources? by lazy { LaunchPlatform.mockSources(dispatchers.io) }
    val dataSources by lazy {
        SessionDataSources(selection, storageRoot, overrides, mockSources, {
            sessionStore.readStoredSession()?.token
        }, dispatchers.io)
    }
    val accountModel by lazy { AccountModel(appScope) }
    val hooks: SessionHooks by lazy { NoopSessionHooks }
    val rootNavigator by lazy { RootNavigator() }
    val welcomeDestinations: WelcomeDestinations by lazy { LaunchPlatform.welcomeDestinations(rootNavigator) }
    val passkeyAnchor: PasskeyAnchor by lazy { PasskeyAnchor() }
    val credentialClient: CredentialClient by lazy { CredentialManagerClient(app, passkeyAnchor) }
    val passkeyCapability: PasskeyCapability get() = LaunchPlatform.passkeyCapability
    val auth: AuthServices by lazy {
        AuthServices(
            rootNavigator,
            appScope,
            dispatchers,
            LazyPublicDataSource { dataSources.public() },
            SessionAcceptor { controller.accept(it) },
            { deviceIdentity.identity() },
            { androidDeviceName(app, dispatchers.io) },
            {
                readiness.routeAcceptedSession(
                    navigate = { rootNavigator.navigate(it.toRoute()) },
                    verification = ::onVerify,
                )
            },
            mockMail = mockResetMail(selection.source, mockSources, { dataSources.public() }) { auth.openReset(it) },
            verificationFactory = {
                VerificationCoordinator(
                    VerificationSessionAdapter(this),
                    VerificationLinkInbox(),
                    rootNavigator,
                    routeAccepted = { navigate, verification ->
                        readiness.routeAcceptedSession({ navigate(it.toRoute()) }, verification)
                    },
                    clearSignInSecrets = { auth.signIn.clearSecrets() },
                    verificationMail = LaunchPlatform.verificationMail(this),
                    nowSeconds = { System.currentTimeMillis() / 1000.0 },
                    scope = appScope,
                    main = dispatchers.main,
                    io = dispatchers.io,
                )
            },
            passkeyCapability = passkeyCapability,
            assertionProvider =
                if (selection.source == DataSourceFactory.Source.Mock) {
                    DeterministicAssertionProvider { throw PasskeyFailure.Unavailable }
                } else {
                    SystemPasskeyAssertionProvider(credentialClient)
                },
        )
    }

    val authLinks: AuthLinkDispatcher by lazy {
        AuthLinkDispatcher(pendingLaunchIntents) { authLinkRouting(it) }
    }
    val resetRouter: GuestResetLinkRouter by lazy {
        GuestResetLinkRouter({ controller.presence }, controller.updates, appScope) {
            rootNavigator.navigate(Route.SignIn)
            auth.openReset(it)
        }
    }
    val authLinkRouting: AuthLinkRouting by lazy { AuthLinkRouting(appScope, resetRouter, auth.verification) }

    val launchIntentHandler: LaunchIntentHandler by lazy { LaunchIntentHandler {} }
    private val syncLazy: Lazy<SessionSync> =
        lazy {
            SessionSync(
                sessionAccounts,
                SyncScheduler.system(),
                makePath = { ConnectivitySyncPathMonitor(app) },
                source = dataSources::forSession,
                isCurrent = { session, epoch ->
                    val current = controller.activeSession()
                    current != null && current.session == session && current.epoch == epoch
                },
                unauthorized = { epoch ->
                    try {
                        controller.unauthorized(epoch)
                    } catch (
                        cancelled: CancellationException,
                    ) {
                        throw cancelled
                    } catch (_: Exception) {
                    }
                },
                verification = ::onVerify,
                dispatcher = dispatchers.io,
            )
        }
    val sessionSync: SessionSync by syncLazy

    internal suspend fun stopSyncIfStarted() {
        if (syncLazy.isInitialized()) sessionSync.stop()
    }

    private val controllerLazy: Lazy<SessionController> =
        lazy {
            SessionController(
                sessionStore,
                sessionAccounts,
                SessionTeardown(
                    sleep = { delay(it) },
                    prepare = {
                        sessionAccounts.close()
                        wiper.recover()
                    },
                    pendingCount = {
                        OutboxRepository(
                            sessionAccounts.activeAccountDatabase() ?: throw StoreFailure.Closed,
                        ).pending().size
                    },
                    flush = { sessionSync.refreshNow() },
                    beginWipe = wiper::begin,
                    erase = wiper::erase,
                    finishWipe = wiper::finish,
                    logout = { dataSources.forSession(it).signOut() },
                    detach = {
                        accountModel.detach()
                        withContext(dispatchers.main) { onSignedOut() }
                    },
                ),
                dispatchers.io,
            ).also {
                it.attach(stopSync = {
                    hooks.onSessionEnding()
                    sessionSync.stop()
                }, requestSync = { epoch ->
                    sessionSync.requestStart(controller, epoch)
                })
            }
        }
    val controller: SessionController by controllerLazy
    private val readinessLazy: Lazy<StoredDataReadiness> =
        lazy {
            StoredDataReadiness(
                controller,
                AccountReadinessSource { id ->
                    AccountReadinessRepository(
                        sessionAccounts.activeAccountDatabase() ?: throw StoreFailure.Closed,
                    ).values(id)
                },
                dispatchers.io,
            )
        }
    val readiness: StoredDataReadiness by readinessLazy
    private val graphsLazy: Lazy<SessionGraphs> =
        lazy {
            SessionGraphs(
                controller,
                sessionAccounts,
                accountModel,
                sessionSync,
                dataSources::forSession,
                appScope,
                dispatchers.io,
            ).also {
                it.start()
            }
        }
    val sessionGraphs: SessionGraphs by graphsLazy
    val onboardingSessions: net.chronoflow.feature.onboarding.OnboardingSessions by lazy {
        net.chronoflow.app.onboarding
            .AppOnboardingSessions(controller, sessionGraphs.current)
    }
    private val routerLazy: Lazy<SessionGateRouter> =
        lazy {
            SessionGateRouter(
                controller,
                readiness,
                appScope,
                ::onSignedOut,
                { rootNavigator.navigate(it.toRoute()) },
                ::onVerify,
            )
        }
    val gateRouter: SessionGateRouter by routerLazy
    val activation by lazy {
        SessionActivation(
            appScope,
            { dataSources.evictIdleConnections() },
            { sessionSync.setEnabled(it) },
            { hooks.onForeground() },
            { controller.retryUnauthorized() },
            { readiness.retry() },
            { controller.activeSession() },
            { sessionSync.resume(it.session, it.epoch) },
        )
    }

    private val firstCommit by lazy { CompletableDeferred<Unit>() }
    internal val launchReadiness by lazy { FirstCommitReadiness(firstCommit, { readiness }, dispatchers.io) }
    val launchBridge by lazy { LaunchSessionBridge(rootNavigator) { authLinks.attach() } }
    val pendingLaunchIntents by lazy { PendingLaunchIntents() }
    private val finishMutex by lazy { Mutex() }
    private var launchFinished = false

    internal fun onFirstCommit() {
        if (firstCommit.isCompleted) return
        LaunchTrace.beginSession()
        firstCommit.complete(Unit)
    }

    suspend fun finishLaunch() =
        finishMutex.withLock {
            authLinks.attach()
            if (!launchFinished) {
                launchFinished = true
                auth.verification
                gateRouter.start()
                LaunchPlatform.observeRoutes(this)
            }
            pendingLaunchIntents.consume()?.let { launchIntentHandler.handle(it) }
        }

    suspend fun handlePendingLaunchIntents() =
        finishMutex.withLock {
            if (launchFinished) pendingLaunchIntents.consume()?.let { launchIntentHandler.handle(it) }
        }

    private fun onSignedOut() {
        val route = rootNavigator.state.value.route
        // Both session observers can arrive after the pending link has opened its reset route.
        if (route is Route.Reset && auth.resetModel?.isActive == true) return
        if (route != Route.SignIn) rootNavigator.navigate(Route.SignIn)
    }

    suspend fun onVerify(epoch: Long) {
        auth.verification.onVerify(epoch)
    }

    suspend fun close() =
        withContext(NonCancellable) {
            if (routerLazy.isInitialized()) gateRouter.stop()
            stopSyncIfStarted()
            if (graphsLazy.isInitialized()) sessionGraphs.close()
            if (accountsLazy.isInitialized()) sessionAccounts.close()
            if (readinessLazy.isInitialized()) readiness.close()
            if (controllerLazy.isInitialized()) controller.close()
            appScope.cancel()
        }
}
