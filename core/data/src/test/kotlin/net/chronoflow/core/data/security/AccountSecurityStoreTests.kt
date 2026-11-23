package net.chronoflow.core.data.security

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.sync.support.SyncStore
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.api.AccountSnapshot
import net.chronoflow.core.model.api.ApiRecoveryCodes
import net.chronoflow.core.model.session.SessionMutationPermit
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.core.testing.junit.measureClassWarmup
import net.chronoflow.core.testing.sync.snapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class AccountSecurityStoreTests {
    @Test fun secretsStayOutOfSQLiteAndPendingBodyClockSurvivesSecuritySnapshot() =
        runTest(timeout = 60.seconds) {
            withStore { f ->
                f.enqueue(
                    OutboxChange.Settings(
                        SettingsSection.BodyClock,
                        JsonObject(
                            mapOf("age_band" to JsonPrimitive("65_plus")),
                        ),
                    ),
                )
                val settings = SettingsRepository(f.database)
                AccountSecurityStore(f.database, settings).apply(
                    AccountSecurityChange.ConfirmTwoFactor("123456"),
                    AccountSecurityResult.Codes(ApiRecoveryCodes(listOf("secret-code"))),
                    SessionMutationPermit(),
                )
                val rows = settings.settings()
                assertFalse(rows.any { "secret-code" in it.value || "123456" in it.value })
                assertTrue(rows.single { it.section == "security" }.value.contains("\"twoFactorEnabled\":true"))
                assertEquals(listOf("settings"), OutboxRepository(f.database).pending().map { it.kind })
            }
        }

    @Test fun disableSnapshotWritesTwoFactorOffAndKeepsPendingBodyClock() =
        runTest(timeout = 60.seconds) {
            withStore { f ->
                f.enqueue(
                    OutboxChange.Settings(
                        SettingsSection.BodyClock,
                        JsonObject(
                            mapOf("age_band" to JsonPrimitive("65_plus")),
                        ),
                    ),
                )
                val settings = SettingsRepository(f.database)
                AccountSecurityStore(f.database, settings).apply(
                    AccountSecurityChange.DisableTwoFactor("password"),
                    AccountSecurityResult.Snapshot(
                        snapshot().let {
                            AccountSnapshot(
                                it.id,
                                it.name,
                                it.email,
                                it.hasVerifiedEmail,
                                it.settings,
                                it.readiness,
                                it.billing,
                                JsonObject(
                                    mapOf("twoFactorEnabled" to JsonPrimitive(false)),
                                ),
                            )
                        },
                    ),
                    SessionMutationPermit(),
                )
                val rows = settings.settings()
                assertTrue(rows.single { it.section == "security" }.value.contains("\"twoFactorEnabled\":false"))
                assertTrue(rows.single { it.section == "bodyClock" }.value.contains("65_plus"))
                assertEquals(listOf("settings"), OutboxRepository(f.database).pending().map { it.kind })
            }
        }

    @Test fun revokedPermitWritesNothing() =
        runTest(timeout = 60.seconds) {
            withStore { f ->
                val settings = SettingsRepository(f.database)
                val before = settings.settings()
                val permit = SessionMutationPermit().apply { invalidate() }
                try {
                    AccountSecurityStore(f.database, settings).apply(
                        AccountSecurityChange.ConfirmTwoFactor("123456"),
                        AccountSecurityResult.Codes(ApiRecoveryCodes(listOf("secret-code"))),
                        permit,
                    )
                    throw AssertionError("A revoked permit was admitted")
                } catch (_: StaleSessionException) {
                    assertEquals(before, settings.settings())
                }
            }
        }

    private suspend fun TestScope.withStore(block: suspend (SyncStore) -> Unit) {
        val f = SyncStore(StandardTestDispatcher(testScheduler))
        try {
            block(f)
        } finally {
            withContext(NonCancellable) { f.close() }
        }
    }

    companion object {
        @JvmStatic @BeforeClass
        fun warmJdbc() {
            measureClassWarmup(AccountSecurityStoreTests::class.java, 0, "jdbc") {
                runTest(timeout = 60.seconds) { SyncStore(StandardTestDispatcher(testScheduler)).close() }
            }
        }
    }
}
