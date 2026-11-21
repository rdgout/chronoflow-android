package net.chronoflow.app.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.api.ApiRecoveryCodes
import net.chronoflow.core.model.api.ApiTwoFactorSetup
import net.chronoflow.core.model.session.SessionSnapshot
import net.chronoflow.core.model.session.SignOutResult
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.testing.FakePlanClock
import net.chronoflow.feature.settings.SettingsDependencies
import net.chronoflow.feature.settings.security.SensitiveClipboard
import java.time.Instant
import java.time.ZoneOffset

internal class SecuritySavedStateDependencies(
    override val securityScope: kotlinx.coroutines.CoroutineScope,
) : SettingsDependencies {
    override val passkeyCapability = net.chronoflow.core.model.passkey.PasskeyCapability.Shipped
    override val passkeyRegistration: net.chronoflow.core.model.passkey.PasskeyRegistrationProviding? = null

    override suspend fun deviceName() = "Android"

    val setup = ApiTwoFactorSetup("JBSWY3DPEHPK3PXP", "otpauth://totp/ChronoFlow?secret=JBSWY3DPEHPK3PXP")
    val codes =
        listOf(
            "8f3k-29dm",
            "q7zp-4wxr",
            "m2vt-8ncb",
            "h6ye-1lsa",
            "c9rk-5jtd",
            "w4bn-7ugq",
            "t1xf-3hzo",
            "p5ma-6evk",
        )
    override val snapshot = SessionSnapshot(StoredSession("fixture", "0199a3c0-0000-7000-8000-000000000001"), 1)
    override val deleteSheetSeed = false

    override suspend fun deleteAccount(password: String): Unit = error("Unused delete")

    override val passwordPromptSeed: String? = null
    override val clipboard: SensitiveClipboard get() = error("Unused clipboard")

    override suspend fun requestSecurity(change: AccountSecurityChange): AccountSecurityResult =
        if (change is AccountSecurityChange.EnableTwoFactor) {
            AccountSecurityResult.Setup(setup)
        } else {
            AccountSecurityResult.Codes(ApiRecoveryCodes(codes))
        }

    override fun account() =
        MutableStateFlow<AccountRecord?>(AccountRecord(snapshot.session.accountId, "Rick", "rick@example.com"))

    override fun settings() = MutableStateFlow(listOf(SettingsRecord("security", "{\"twoFactorEnabled\":true}")))

    override fun outbox() = MutableStateFlow(emptyList<OutboxRecord>())

    override suspend fun pendingOutbox() = emptyList<OutboxRecord>()

    override fun syncEvents() = emptyFlow<net.chronoflow.core.sync.SyncEvent>()

    override fun sessionUpdates() = emptyFlow<net.chronoflow.core.model.session.SessionUpdate>()

    override suspend fun isCurrent() = true

    override suspend fun signOut(discardPending: Boolean) = SignOutResult.SignedOut

    override suspend fun enqueueProfile(
        name: String,
        email: String,
    ) = Unit

    override suspend fun requestSync() = Unit

    override val clock = FakePlanClock(Instant.parse("2026-11-21T12:00:00Z"))

    override suspend fun formatter(): Nothing = error("No confirmed date in this fixture")

    override val zone = ZoneOffset.UTC

    override suspend fun enqueueSettings(
        section: net.chronoflow.core.model.SettingsSection,
        patch: kotlinx.serialization.json.JsonObject,
    ) = Unit
}
