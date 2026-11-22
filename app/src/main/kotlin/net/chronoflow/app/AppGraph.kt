package net.chronoflow.app

import android.app.Application
import android.content.ComponentName
import android.os.Process
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.chronoflow.app.auth.AuthServices
import net.chronoflow.app.auth.LazyPublicDataSource
import net.chronoflow.app.auth.VerificationSessionAdapter
import net.chronoflow.app.auth.mockResetMail
import net.chronoflow.app.links.AuthLinkDispatcher
import net.chronoflow.app.links.AuthLinkRouting
import net.chronoflow.app.reminders.AndroidReminderStrings
import net.chronoflow.app.reminders.AppLanguageSource
import net.chronoflow.app.reminders.AppReminderRuntime
import net.chronoflow.app.reminders.ReminderSessionHooks
import net.chronoflow.app.wizard.SessionTripWizardDependencies
import net.chronoflow.core.data.clock.SystemPlanClock
import net.chronoflow.core.data.passkey.CredentialManagerClient
import net.chronoflow.core.data.passkey.PasskeyAnchor
import net.chronoflow.core.data.reminders.AndroidReminderAlarms
import net.chronoflow.core.data.reminders.AndroidReminderNotices
import net.chronoflow.core.data.reminders.ReminderPermissions
import net.chronoflow.core.data.reminders.ReminderService
import net.chronoflow.core.data.reminders.ReminderWork
import net.chronoflow.core.data.repository.AccountModel
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.session.AccountReadinessRepository
import net.chronoflow.core.data.session.AccountWiper
import net.chronoflow.core.data.session.GuestSecrets
import net.chronoflow.core.data.session.SessionAccountDatabase
import net.chronoflow.core.data.session.SessionHooks
import net.chronoflow.core.data.sync.ConnectivitySyncPathMonitor
import net.chronoflow.core.data.sync.system
import net.chronoflow.core.database.AndroidAccountDriverFactory
import net.chronoflow.core.datastore.AndroidKeystoreSessionCipher
import net.chronoflow.core.datastore.DeviceIdentityStore
import net.chronoflow.core.datastore.DevicePreferences
import net.chronoflow.core.datastore.KeystoreGuestSecretStore
import net.chronoflow.core.datastore.KeystoreSessionStore
import net.chronoflow.core.datastore.WipeMarker
import net.chronoflow.core.datastore.androidDeviceName
import net.chronoflow.core.model.guest.GuestSecretStore
import net.chronoflow.core.model.passkey.CredentialClient
import net.chronoflow.core.model.passkey.PasskeyCapability
import net.chronoflow.core.model.passkey.PasskeyFailure
import net.chronoflow.core.model.session.SessionStore
import net.chronoflow.core.model.session.readStoredSession
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.navigation.LaunchIntentHandler
import net.chronoflow.core.navigation.ReminderTapRouter
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.toRoute
import net.chronoflow.core.network.DataSourceFactory
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.PlanClock
import net.chronoflow.core.plan.PlanDecoder
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
import java.time.Instant

class AppGraph(
    private val app: Application,
    override val fontPreloader: FontPreloader = ResourceFontPreloader,
    private val launchProvider: (() -> LaunchController)? = null,
    val overrides: GraphOverrides = GraphOverrides.None,
    private val suppliedDispatchers: AppDispatchers = AppDispatchers(),
) : RootThemeGraph {
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
    override val dispatchers by lazy { overrides.dispatchers ?: suppliedDispatchers }
    val appScope by lazy { CoroutineScope(SupervisorJob() + dispatchers.main) }
    internal val launchScope get() = appScope
    internal val processToken = "${Process.myPid()}:${Process.getStartElapsedRealtime()}"
    override val launch: LaunchController get() = launchProvider?.invoke() ?: LaunchPlatform.controller(this)
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
    val deviceAppearance by lazy {
        net.chronoflow.core.datastore.DeviceAppearance(
            devicePreferences,
            net.chronoflow.core.datastore
                .UiModeNightModeSink(app),
            dispatchers.io,
        )
    }
    val languageFeedback by lazy {
        net.chronoflow.app.settings
            .LanguageFeedback()
    }
    val languageApplier by lazy {
        net.chronoflow.app.settings
            .AppLanguageApplier(app, devicePreferences, dispatchers.io, languageFeedback)
    }
    val deviceIdentity by lazy { DeviceIdentityStore(devicePreferences) }
    val guestSecrets: GuestSecretStore by lazy {
        KeystoreGuestSecretStore.forNamespace(
            app,
            namespace,
            overrides.sessionCipher ?: AndroidKeystoreSessionCipher(),
            dispatchers.io,
        )
    }
    val guestWizardStore by lazy {
        net.chronoflow.core.wizard.model.GuestWizardStore(
            file = {
                net.chronoflow.core.model.guest.GuestRecoveryLocation
                    .file(storageRoot(), namespace)
            },
            io = dispatchers.io,
            now = { planClock.now },
        )
    }
    val guestAcceptance by lazy {
        net.chronoflow.core.data.guest.GuestSessionAcceptance(
            guestWizardStore,
            guestSecrets,
            { planClock.now },
            dispatchers.io,
        )
    }
    val wiper by lazy {
        AccountWiper(
            storageRoot,
            namespace,
            sessionStore,
            guestSecrets =
                object : GuestSecrets {
                    override suspend fun deleteAll() = guestSecrets.deleteAll()
                },
            io = dispatchers.io,
        )
    }
    private val accountsLazy: Lazy<SessionAccountDatabase> =
        lazy {
            SessionAccountDatabase(
                storageRoot,
                namespace,
                AndroidAccountDriverFactory(app),
                dispatchers.io,
                detach = {
                    reminderTaps.clear()
                    reminderService.detach()
                    sessionGraphs.drop()
                },
            )
        }
    val sessionAccounts: SessionAccountDatabase by accountsLazy
    val mockSources: MockSources? by lazy { LaunchPlatform.mockSources(dispatchers.io) }
    val dataSources by lazy {
        SessionDataSources(selection, storageRoot, overrides, mockSources, {
            sessionStore.readStoredSession()?.token
        }, dispatchers.io)
    }
    val reminderPermissions by lazy { ReminderPermissions(app, devicePreferences, dispatchers.io) }
    val reminderStrings by lazy { AndroidReminderStrings(app) }
    val appLanguage by lazy { AppLanguageSource(languageApplier, dispatchers.io) }
    val reminderNotices by lazy { AndroidReminderNotices(app, dispatchers.io, reminderStrings::channelName) }
    private val reminderServiceLazy =
        lazy {
            ReminderService(
                AndroidReminderAlarms(app, dispatchers.io),
                reminderNotices,
                permission = { reminderPermissions.refresh() },
                language = { appLanguage.current() },
                strings = reminderStrings,
                formatter = { CopyLineFormatter(copyCatalogs.catalog()) },
                decoder = PlanDecoder(dispatchers.default),
                clock = { reminderRuntime.clock() },
                dispatcher = dispatchers.default,
            )
        }
    val reminderService: ReminderService by reminderServiceLazy
    val reminderTaps by lazy { ReminderTapRouter() }
    val reminderRuntime by lazy {
        AppReminderRuntime(
            io = dispatchers.io,
            scope = appScope,
            notificationIcon = R.drawable.ic_stat_moon,
            component = { ComponentName(app, MainActivity::class.java) },
            clock = { Instant.now() },
            sessionStore = sessionStore,
            wipeMarker = { WipeMarker(File(storageRoot(), namespace.directoryName), dispatchers.io) },
            prepareBackground = { activation.prepareBackground() },
            startSessionGraphs = { sessionGraphs.start() },
            session = { readiness.session() },
            attachedAccount = reminderService.attachedAccount,
            reschedule = { reminderService.reschedule() },
            delivered = reminderService::delivered,
            ensureChannel = { reminderNotices.ensureChannel(appLanguage.current()) },
        )
    }
    val accountModel by lazy { AccountModel(appScope) }
    val hooks: SessionHooks by lazy {
        net.chronoflow.app.guest
            .GuestSessionHooks(
                guestWizardStore,
                ReminderSessionHooks(
                    { reminderService.reschedule() },
                    { reminderService.detach() },
                ),
            )
    }
    val rootNavigator by lazy { RootNavigator() }
    internal val guestRecoveryScheduler by lazy {
        net.chronoflow.app.guest.GuestRecoveryScheduler(
            dispatchers,
            guestWizardStore,
            rootNavigator,
            { controller.presence },
            { controller.updates.value?.epoch },
            { LaunchPlatform.guestRecoveryAllowed },
        )
    }
    val welcomeDestinations: WelcomeDestinations by lazy { LaunchPlatform.welcomeDestinations(rootNavigator) }
    val passkeyAnchor: PasskeyAnchor by lazy { PasskeyAnchor() }
    val credentialClient: CredentialClient by lazy { CredentialManagerClient(app, passkeyAnchor) }
    val passkeyCapability: PasskeyCapability get() = LaunchPlatform.passkeyCapability
    val passkeyRegistration: net.chronoflow.core.model.passkey.PasskeyRegistrationProviding by lazy {
        if (selection.source == DataSourceFactory.Source.Mock) {
            net.chronoflow.core.model.passkey
                .DeterministicRegistrationProvider()
        } else {
            net.chronoflow.core.model.passkey
                .SystemPasskeyRegistrationProvider(credentialClient)
        }
    }

    suspend fun settingsDeviceName(): String = androidDeviceName(app, dispatchers.io)

    val auth: AuthServices by lazy {
        AuthServices(
            rootNavigator,
            appScope,
            dispatchers,
            LazyPublicDataSource { dataSources.public() },
            SessionAcceptor { controller.accept(it) },
            { deviceIdentity.identity() },
            { androidDeviceName(app, dispatchers.io) },
            { result, method ->
                guestAcceptance.routeAcceptedSession(
                    result,
                    method,
                    controller,
                    sessionAccounts,
                    readiness,
                    appScope,
                    navigate = { rootNavigator.navigate(it.toRoute()) },
                    verification = ::onVerify,
                )
            },
            claim = { guestWizardStore.claim(guestSecrets) },
            guestReviewExpired = { claim ->
                net.chronoflow.app.guest
                    .quietly { guestWizardStore.reviewExpired(claim) }
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
                copyCatalogs = copyCatalogs,
                attachReminders = { reminderService.attach(it) },
                detachReminders = { reminderService.detach() },
                clearReminderTaps = { reminderTaps.clear() },
                ensureRefill = { ReminderWork.ensureRefill(app) },
                rescheduleReminders = { reminderService.reschedule() },
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
    private val settingsClipboardLazy =
        lazy {
            net.chronoflow.feature.settings.security.SensitiveClipboard(
                net.chronoflow.feature.settings.security.AndroidClipboardPort(
                    checkNotNull(app.getSystemService(android.content.ClipboardManager::class.java)),
                ),
                appScope,
                app.getString(net.chronoflow.core.ui.R.string.clipboard_label),
            )
        }
    val settingsClipboard by settingsClipboardLazy

    val activation by lazy {
        SessionActivation(
            appScope,
            { dataSources.evictIdleConnections() },
            { sessionSync.setEnabled(it) },
            {
                if (settingsClipboardLazy.isInitialized()) settingsClipboard.foreground()
                hooks.onForeground()
            },
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
        appScope.launch(dispatchers.io) { reminderRuntime.ensureChannel() }
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

    private var deleteSignOutEpoch: Long? = null

    internal suspend fun signOutAfterDeletingAccount(expected: net.chronoflow.core.model.session.SessionSnapshot) =
        withContext(dispatchers.main) {
            if (controller.activeSession() != expected) {
                throw net.chronoflow.core.model.session
                    .StaleSessionException()
            }
            val signedOutEpoch = expected.epoch + 1
            deleteSignOutEpoch = signedOutEpoch
            try {
                controller.signOut(expected, discardPending = true)
            } finally {
                if (deleteSignOutEpoch == signedOutEpoch) deleteSignOutEpoch = null
            }
        }

    private fun onSignedOut() {
        val route = rootNavigator.state.value.route
        // Both session observers can arrive after the pending link has opened its reset route.
        val resetting = route is Route.Reset && auth.resetModel?.isActive == true
        val destination = if (deleteSignOutEpoch == controller.epoch) Route.Welcome else Route.SignIn
        net.chronoflow.app.settings
            .signedOutDestination(route, resetting, destination)
            ?.let(rootNavigator::navigate)
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
            if (reminderServiceLazy.isInitialized()) reminderService.close()
            if (readinessLazy.isInitialized()) readiness.close()
            if (controllerLazy.isInitialized()) controller.close()
            appScope.cancel()
        }
}
