@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.security

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.feature.settings.support.FakeSettingsDependencies
import net.chronoflow.feature.settings.support.SettingsFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.ContinuationInterceptor
import kotlin.time.Duration.Companion.seconds

class SecurityConfirmedDateStateTests {
    @Test fun noFormattingBeforeLanguageIsKnownAndDutchIsTheFirstFormattedDate() =
        runTest(timeout = 60.seconds) {
            withModel { vm, deps ->
                var formats = 0
                deps.makeFormatter = {
                    formats++
                    CopyLineFormatter(SettingsFixture.catalog)
                }
                deps.settings.value = security("2026-10-30T08:00:00Z")
                runCurrent()
                assertEquals(
                    true,
                    vm.ui.value.summary
                        ?.twoFactorEnabled,
                )
                assertEquals(0, formats)
                assertNull(vm.ui.value.since)

                vm.setLanguage(Language.Nl)
                runCurrent()
                assertEquals(1, formats)
                assertEquals("30 okt 2026", vm.ui.value.since)
                assertNull(vm.ui.value.feedback)
            }
        }

    @Test fun formatterFailureOnComputeOmitsTheDateWithoutFeedback() =
        runTest(timeout = 60.seconds) {
            val compute = StandardTestDispatcher(testScheduler, "compute")
            withModel(compute) { vm, deps ->
                var formats = 0
                deps.makeFormatter = {
                    formats++
                    assertEquals(compute, currentCoroutineContext()[ContinuationInterceptor])
                    throw IOException("formatter unavailable")
                }
                deps.settings.value = security("2026-10-30T08:00:00Z")
                vm.setLanguage(Language.En)
                runCurrent()
                assertEquals(1, formats)
                assertEquals(
                    true,
                    vm.ui.value.summary
                        ?.twoFactorEnabled,
                )
                assertNull(vm.ui.value.since)
                assertNull(vm.state.value.message)
                assertNull(vm.ui.value.feedback)
            }
        }

    @Test fun summaryAndLanguageDriveTheDateWithoutScreenEffects() =
        runTest(timeout = 60.seconds) {
            withModel { vm, deps ->
                var formats = 0
                deps.makeFormatter = {
                    formats++
                    CopyLineFormatter(SettingsFixture.catalog)
                }
                deps.settings.value = security("2026-10-30T08:00:00Z")
                vm.setLanguage(Language.En)
                runCurrent()
                assertEquals("30 Oct 2026", vm.ui.value.since)
                assertEquals(1, formats)

                deps.account.value = deps.account.value?.copy(name = "Updated name")
                runCurrent()
                assertEquals(1, formats)

                vm.setLanguage(Language.Nl)
                runCurrent()
                assertEquals("30 okt 2026", vm.ui.value.since)
                assertEquals(2, formats)

                deps.settings.value = security("2026-11-01T08:00:00Z")
                runCurrent()
                assertEquals("1 nov 2026", vm.ui.value.since)
                assertEquals(3, formats)

                deps.settings.value = security(null)
                runCurrent()
                assertNull(vm.ui.value.since)
                assertEquals(3, formats)
                assertNull(vm.ui.value.feedback)
            }
        }

    private fun security(raw: String?): List<SettingsRecord> =
        listOf(
            SettingsRecord(
                "security",
                """{"twoFactorEnabled":true,"twoFactorConfirmedAt":${raw?.let { "\"$it\"" } ?: "null"}}""",
            ),
        )

    private suspend fun TestScope.withModel(
        compute: CoroutineDispatcher = StandardTestDispatcher(testScheduler, "compute"),
        block: suspend (SecurityViewModel, FakeSettingsDependencies) -> Unit,
    ) {
        val deps = FakeSettingsDependencies(backgroundScope)
        val main = StandardTestDispatcher(testScheduler, "main")
        val vm = SecurityViewModel(deps, main, compute, main)
        val store = ViewModelStore().apply { put("security", vm) }
        try {
            runCurrent()
            block(vm, deps)
        } finally {
            withContext(NonCancellable) {
                store.clear()
                vm.viewModelScope.coroutineContext.job
                    .cancelAndJoin()
            }
        }
    }
}
