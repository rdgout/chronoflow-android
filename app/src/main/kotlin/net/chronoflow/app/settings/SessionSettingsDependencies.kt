package net.chronoflow.app.settings

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import net.chronoflow.app.AppGraph
import net.chronoflow.app.SessionGraph
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.ProfileChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.feature.settings.SettingsDependencies
import java.time.ZoneId

internal class SessionSettingsDependencies(
    private val app: AppGraph,
    private val session: SessionGraph,
) : SettingsDependencies {
    override val snapshot = session.snapshot

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
