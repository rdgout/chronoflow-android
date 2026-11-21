package net.chronoflow.feature.settings.support

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.ProfileChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.SessionSnapshot
import net.chronoflow.core.model.session.SessionUpdate
import net.chronoflow.core.model.session.SignOutResult
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.sync.SyncEvent
import net.chronoflow.core.testing.FakePlanClock
import net.chronoflow.feature.settings.SettingsDependencies
import java.time.ZoneOffset

class FakeSettingsDependencies(
    clipboardScope: kotlinx.coroutines.CoroutineScope? = null,
) : SettingsDependencies {
    var security: suspend (net.chronoflow.core.model.api.AccountSecurityChange) ->
    net.chronoflow.core.model.api.AccountSecurityResult = {
        net.chronoflow.core.model.api.AccountSecurityResult.Changed
    }

    override suspend fun performSecurity(change: net.chronoflow.core.model.api.AccountSecurityChange) = security(change)

    override var passwordPromptSeed: String? = null
    val clipboardPort = RecordingClipboardPort()
    override val clipboard by lazy {
        net.chronoflow.feature.settings.security.SensitiveClipboard(
            clipboardPort,
            checkNotNull(clipboardScope),
            "ChronoFlow",
        )
    }

    override val snapshot = SessionSnapshot(StoredSession("fixture", "019a562b-8000-7000-8000-000000000071"), 1)
    val account =
        MutableStateFlow<AccountRecord?>(AccountRecord(snapshot.session.accountId, "Rick Gout", "rick@example.com"))
    val settings = MutableStateFlow(emptyList<SettingsRecord>())
    val rows = MutableStateFlow(emptyList<OutboxRecord>())
    val events = MutableSharedFlow<SyncEvent>(replay = 1)
    val updates = MutableStateFlow(SessionUpdate(SessionPresence.SignedIn(snapshot.session.accountId), 1))
    var current = true
    var requests = 0
    var saves = 0
    var save: suspend (String, String) -> Unit = { name, email ->
        saves++
        val change = OutboxChange.Profile(ProfileChange(name, email))
        rows.value += OutboxPayload.record(change, "profile-$saves", "now")
        account.value = account.value?.copy(name = name)
    }
    var settingsSaves = 0
    var saveSettings: suspend (SettingsSection, JsonObject) -> Unit = { section, patch ->
        settingsSaves++
        rows.value +=
            OutboxPayload.record(
                OutboxChange.Settings(section, patch),
                "settings-$settingsSaves",
                clock.now.toString(),
            )
    }

    override fun account() = account

    override fun settings() = settings

    override fun outbox() = rows

    override suspend fun pendingOutbox() = rows.value

    override fun syncEvents() = events

    override fun sessionUpdates() = updates

    override suspend fun isCurrent() = current

    override suspend fun signOut(discardPending: Boolean) =
        if (discardPending || rows.value.isEmpty()) {
            SignOutResult.SignedOut
        } else {
            SignOutResult.NeedsConfirmation(rows.value.size)
        }

    override suspend fun enqueueProfile(
        name: String,
        email: String,
    ) = save(name, email)

    override suspend fun requestSync() {
        requests++
    }

    override val clock = FakePlanClock(settingsNow)
    override val zone = ZoneOffset.UTC

    override suspend fun formatter() =
        net.chronoflow.core.plan
            .CopyLineFormatter(SettingsFixture.catalog)

    override suspend fun enqueueSettings(
        section: SettingsSection,
        patch: JsonObject,
    ) = saveSettings(section, patch)
}

class RecordingClipboardPort : net.chronoflow.feature.settings.security.ClipboardPort {
    var text: String? = null
    var sensitive = false
    var stamp = 0L

    override fun set(
        label: String,
        text: String,
        sensitive: Boolean,
    ): Long {
        this.text = text
        this.sensitive = sensitive
        return ++stamp
    }

    override fun currentTimestamp(): Long = stamp

    override fun clear() {
        text = null
    }

    override fun toString(): String = "RecordingClipboardPort(***)"
}
