@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.security

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.feature.settings.support.FakeSettingsDependencies
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class SecurityConfirmedDateTests(
    private val raw: String?,
    private val language: Language,
    private val expected: String?,
) {
    @Test fun confirmedDateUsesTheAccountZoneAndOmitsInvalidValues() =
        runTest(timeout = 60.seconds) {
            val deps = FakeSettingsDependencies(backgroundScope)
            val security =
                buildJsonObject {
                    put("twoFactorEnabled", true)
                    put("twoFactorConfirmedAt", raw?.let(::JsonPrimitive) ?: JsonNull)
                }
            deps.settings.value = listOf(SettingsRecord("security", security.toString()))
            val dispatcher = StandardTestDispatcher(testScheduler)
            val vm = SecurityViewModel(deps, dispatcher, dispatcher, dispatcher)
            val store = ViewModelStore().apply { put("security", vm) }
            try {
                runCurrent()
                assertEquals(expected, vm.confirmedDate(language))
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    store.clear()
                    vm.viewModelScope.coroutineContext.job
                        .cancelAndJoin()
                }
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}-{1}")
        fun cases(): List<Array<Any?>> =
            listOf(
                arrayOf("2026-10-30T08:00:00.000000Z", Language.En, "30 Oct 2026"),
                arrayOf("2026-10-30T08:00:00Z", Language.Nl, "30 okt 2026"),
                arrayOf(null, Language.En, null),
                arrayOf("garbage", Language.En, null),
            )
    }
}
