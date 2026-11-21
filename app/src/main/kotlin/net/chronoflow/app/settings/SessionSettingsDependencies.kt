package net.chronoflow.app.settings

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import net.chronoflow.app.AppGraph
import net.chronoflow.app.SessionGraph
import net.chronoflow.core.data.security.AccountSecurityStore
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.ProfileChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.feature.settings.SettingsDependencies
import java.time.ZoneId

internal class SessionSettingsDependencies(
    private val app: AppGraph,
    private val session: SessionGraph,
) : SettingsDependencies {
    override val passkeyCapability get() = app.passkeyCapability
    override val passkeyRegistration get() = app.passkeyRegistration

    override suspend fun deviceName() = app.settingsDeviceName()

    override val securityScope get() = session.scope
    override val snapshot = session.snapshot
    override val passwordPromptSeed: String? get() = net.chronoflow.app.LaunchPlatform.passwordPromptSeed
    override val clipboard get() = app.settingsClipboard

    override suspend fun requestSecurity(change: AccountSecurityChange): AccountSecurityResult =
        withContext(app.dispatchers.io) {
            if (!isCurrent()) throw StaleSessionException()
            val result =
                try {
                    session.sync.accountSecurity(change, snapshot.session, snapshot.epoch)
                } catch (_: CancellationException) {
                    currentCoroutineContext().ensureActive()
                    throw StaleSessionException()
                }
            if (!isCurrent()) throw StaleSessionException()
            val permit = app.controller.mutationPermit(snapshot) ?: throw StaleSessionException()
            AccountSecurityStore(session.database, session.settings).apply(change, result, permit)
            session.sync.requestSync()
            result
        }

    override fun account() = session.accounts.values()

    override fun settings() = session.settings.values()

    override fun outbox() = session.outbox.values()

    override suspend fun pendingOutbox() = session.outbox.pending()

    override fun syncEvents() = session.sync.events()

    override fun sessionUpdates() = app.controller.updates.filterNotNull()

    override suspend fun isCurrent() = app.controller.activeSession() == snapshot

    override suspend fun signOut(discardPending: Boolean) = app.controller.signOut(snapshot, discardPending)

    override suspend fun enqueueProfile(
        name: String,
        email: String,
    ) = withContext(app.dispatchers.io) {
        val permit = app.controller.mutationPermit(snapshot) ?: throw StaleSessionException()
        session.outbox.enqueue(OutboxChange.Profile(ProfileChange(name, email)), permit)
        session.sync.requestSync()
    }

    override suspend fun requestSync() = session.sync.requestSync()

    override val clock = app.planClock
    override val zone: ZoneId get() = ZoneId.systemDefault()

    override suspend fun formatter() = CopyLineFormatter(app.copyCatalogs.catalog())

    override suspend fun enqueueSettings(
        section: SettingsSection,
        patch: JsonObject,
    ) = withContext(app.dispatchers.io) {
        val permit = app.controller.mutationPermit(snapshot) ?: throw StaleSessionException()
        session.outbox.enqueue(OutboxChange.Settings(section, patch), permit)
        Unit
    }
}
